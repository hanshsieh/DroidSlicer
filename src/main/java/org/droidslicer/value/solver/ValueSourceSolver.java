package org.droidslicer.value.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.analysis.RecordCallTabulationSolver;
import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.PhiStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.SparseIntSet;

public class ValueSourceSolver
{
	private class InstructionExtendedDependsVisitor extends SSAInstruction.Visitor
	{
		private Collection<Statement> mResult;
		private NormalStatement mNormalStm;
		private IntSet mDependFacts;
		public Collection<Statement> visit(NormalStatement stm, IntSet dependFacts)
		{
			SSAInstruction inst = stm.getInstruction();
			mResult = Collections.emptySet();
			mNormalStm = stm;
			mDependFacts = dependFacts;
			inst.visit(this);
			Collection<Statement> result = mResult;
			mNormalStm = null;
			mResult = null;
			mDependFacts = null;
			return result;
		}
		public void visitGet(SSAGetInstruction getInst) 
		{
			FieldReference fieldRef = getInst.getDeclaredField();
			IClassHierarchy cha = mAnalysisCtx.getClassHierarchy();
			IField field = cha.resolveField(fieldRef);
			if(field == null)
			{
				mResult = null;
				return;
			}
			Collection<Statement> writeStms;
			if(getInst.isStatic())
			{
				StaticFieldKey fieldPointer = new StaticFieldKey(field);
				writeStms = mAnalysisCtx.getWritesToStaticField(fieldPointer);
			}
			else
			{
				LocalPointerKey refPointer = new LocalPointerKey(mNormalStm.getNode(), getInst.getRef());
				writeStms = mAnalysisCtx.getWritesToInstanceField(refPointer, field);
			}
			mResult = new LinkedHashSet<Statement>();		
			for(Statement writeStm : writeStms)
			{
				CGNode writeNode = writeStm.getNode();
				IMethod writeMethod = writeNode.getMethod();
				ClassLoaderReference writeClassLoaderRef = writeMethod.getDeclaringClass().getClassLoader().getReference();
				if(!writeMethod.isClinit() && 
					!writeMethod.isSynthetic() && 
					!writeClassLoaderRef.equals(mAnalysisCtx.getAnalysisScope().getApplicationLoader()))
				{
					continue;
				}
				mResult.add(writeStm);
			}
		}
		public void visitNew(SSANewInstruction newInst) 
		{
			mAllocSources.add(mNormalStm);
		}
		public void visitLoadMetadata(SSALoadMetadataInstruction loadMetaDataInst)
		{
			Object token = loadMetaDataInst.getToken();
			if(token instanceof TypeReference)
			{
				updateConstantFacts((TypeReference)token, mDependFacts);
			}
		}
		public void visitPut(SSAPutInstruction putInst)
		{
			CGNode node = mNormalStm.getNode();
			int val = putInst.getVal();
			SymbolTable symbolTable = node.getIR().getSymbolTable();
			if(symbolTable.isConstant(val))
			{
				Object constVal = symbolTable.getConstantValue(val);
				updateConstantFacts(constVal, mDependFacts);
			}
		}
		public void visitReturn(SSAReturnInstruction retInst)
		{
			CGNode node = mNormalStm.getNode();
			int val = retInst.getResult();
			SymbolTable symbolTable = node.getIR().getSymbolTable();
			if(symbolTable.isConstant(val))
			{
				Object constVal = symbolTable.getConstantValue(val);
				updateConstantFacts(constVal, mDependFacts);
			}
		}
	}
	private final static Logger mLogger = LoggerFactory.getLogger(ValueSourceSolver.class);
	private final static double MAX_MEMORY_USAGE = 0.85;
	private final static int INITIAL_FACT = 1;
	private final AndroidAnalysisContext mAnalysisCtx;
	private final ValueSourceFunctions mFunct;
	private Set<Pair<Statement, Statement>> mCallSources = null;
	private Set<NormalStatement> mAllocSources = null; 
	private Map<Object, MutableIntSet> mConstants = null;
	private CallRecords mCallRecords;
	private boolean mIsRecordCalls = false;
	private InstructionExtendedDependsVisitor mInstExtendedDependsVisitor = new InstructionExtendedDependsVisitor();

	public ValueSourceSolver(AndroidAnalysisContext analysisCtx, ValueSourceFunctions funct, CallRecords callRecords)
	{
		mAnalysisCtx = analysisCtx;
		mFunct = funct;
		mCallRecords = callRecords;
	}
	public void setIsRecordCalls(boolean val)
	{
		mIsRecordCalls = val;
	}
	public boolean isRecordCalls()
	{
		return mIsRecordCalls;
	}
	public CallRecords getCallRecords()
	{
		return mCallRecords;
	}
	public void solve(Collection<Statement> seeds, ProgressMonitor monitor)
		throws CancelException
	{
		monitor.beginTask("Solving value source problem", 100);		
		if(mLogger.isDebugEnabled())
		{
			StringBuilder builder = new StringBuilder();
			builder.append("Find the value sources of the following statements: \n");
			for(Statement seed : seeds)
			{
				builder.append('\t');
				builder.append(seed);
				builder.append('\n');
			}
			mLogger.debug("{}", builder.toString());
		}
		try
		{
			mCallSources = new HashSet<Pair<Statement, Statement>>();
			mAllocSources = new HashSet<NormalStatement>();
			mConstants = new HashMap<Object, MutableIntSet>();
			mFunct.clearReachedStatements();
			mFunct.setNextGenFact(INITIAL_FACT + 1);
			solveInternal(seeds, SparseIntSet.singleton(INITIAL_FACT), mCallRecords, monitor);
		}
		finally
		{
			monitor.done();
		}
	}
	public void solve(Statement seed, ProgressMonitor monitor)
		throws CancelException
	{
		solve(Collections.singleton(seed), monitor);
	}
	protected void checkMemoryUsage()
	{
		Runtime runtime = Runtime.getRuntime();
		double usage = 1.0 - (double)runtime.freeMemory() / runtime.maxMemory();
		if(usage > MAX_MEMORY_USAGE)
		{
			mLogger.debug("Memory-use rate {}% exceeding threashold, releasing it", usage * 100.0);
			Utils.releaseMemory(mAnalysisCtx);
		}
	}
	public void solveInternal(Collection<Statement> seeds, IntSet facts, CallRecords callRecords, ProgressMonitor monitor)
		throws CancelException
	{
		if(monitor.isCanceled())
			throw CancelException.make("Operation canceled");
		mLogger.debug("Solving internal round start");
		
		// Remove the seeds that won't update the facts
		{
			Set<Statement> newSeeds = new HashSet<Statement>();
			for(Statement seed : seeds)
			{
				IntSet oriFacts = mFunct.getStatementFacts(seed);
				if(!facts.isSubset(oriFacts))
					newSeeds.add(seed);
			}
			if(newSeeds.isEmpty())
				return;
			seeds = newSeeds;
			if(seeds.isEmpty())
				return;
		}
		
		// Notice that we should prevent keeping a local reference to the SDG, which may prevent 
		// the SDG to be released by GC
		Map<Statement, IntSet> reachedStms = new LinkedHashMap<Statement, IntSet>();
		checkMemoryUsage();
		{
			SDG sdg = mAnalysisCtx.getSDG();
			ValueSourceProblem problem = new ValueSourceProblem(sdg, mFunct, seeds, facts);
			RecordCallTabulationSolver<Statement> solver = RecordCallTabulationSolver.create(problem, callRecords, monitor);
			solver.setRecordCalls(true);
	
			TabulationResult<Statement, PDG, Statement> tResult = solver.solve();
			if(mIsRecordCalls)
			{
				mCallRecords.addAllCalls(solver.getCallRecords());
			}
			
			Iterator<Pair<Statement, Statement>> killEdges = mFunct.getKilledEdges();
			
			while(killEdges.hasNext())
			{
				Pair<Statement, Statement> pair = killEdges.next();
				Statement caller = pair.getKey();
				if(caller.getKind().equals(Statement.Kind.NORMAL_RET_CALLER) || 
					caller.getKind().equals(Statement.Kind.HEAP_RET_CALLER))
				{
					mCallSources.add(pair);
				}
			}
			for(Statement reachedStm : tResult.getSupergraphNodesReached())
			{
				IntSet dependFacts = tResult.getResult(reachedStm);
				if(dependFacts.isEmpty())
					continue;
				reachedStms.put(reachedStm, dependFacts);
				mFunct.addReachedStatement(reachedStm, dependFacts);
				if(reachedStm.getKind().equals(Statement.Kind.NORMAL_RET_CALLER) || 
					reachedStm.getKind().equals(Statement.Kind.HEAP_RET_CALLER))
				{
					// If it doesn't have a callee
					if(!sdg.getPredNodes(reachedStm).hasNext())
						mCallSources.add(Pair.of(reachedStm, (Statement)null));
				}
			}
			callRecords = solver.getCallRecords();
		}
		for(Map.Entry<Statement, IntSet> entry : reachedStms.entrySet())
		{
			Statement reachedStm = entry.getKey();
			IntSet dependFacts = entry.getValue();
			Collection<Statement> depends = getExtendedDepends(reachedStm, dependFacts);
			if(depends != null && !depends.isEmpty())
			{
				solveInternal(depends, dependFacts, new CallRecords(callRecords), monitor);
			}
		}
	}
	protected void updateConstantFacts(Object constVal, IntSet newFacts)
	{
		MutableIntSet oldFacts = mConstants.get(constVal);
		if(oldFacts == null)
		{
			oldFacts = MutableSparseIntSet.makeEmpty();
			mConstants.put(constVal, oldFacts);
		}
		oldFacts.addAll(newFacts);
	}
	protected Collection<Statement> getExtendedDependsNormalStm(NormalStatement normalStm, IntSet dependFacts)
	{
		return mInstExtendedDependsVisitor.visit(normalStm, dependFacts);
	}
	protected Collection<Statement> getExtendedDependsParamCallerStm(ParamCaller paramCallerStm, IntSet dependFacts)
	{
		CGNode node = paramCallerStm.getNode();
		int valNum = paramCallerStm.getValueNumber();
		SymbolTable symbolTable = node.getIR().getSymbolTable();
		if(symbolTable.isConstant(valNum))
		{
			Object constVal = symbolTable.getConstantValue(valNum);
			updateConstantFacts(constVal, dependFacts);
		}
		return null;
	}
	protected Collection<Statement> getExtendedDependsPhiStm(PhiStatement phiStm, IntSet dependFacts)
	{
		CGNode node = phiStm.getNode();
		SSAPhiInstruction phiInst = phiStm.getPhi();
		int nUse = phiInst.getNumberOfUses();
		for(int i = 0; i < nUse; ++i)
		{
			int use = phiInst.getUse(i);
			// The used value number of Phi instruction may happen.
			// See the API doc of PhiInstruction
			if(use >= 0)
			{
				SymbolTable symbolTable = node.getIR().getSymbolTable();
				if(symbolTable.isConstant(use))
				{
					Object constVal = symbolTable.getConstantValue(use);
					updateConstantFacts(constVal, dependFacts);
				}
			}
		}
		return null;
	}
	protected Collection<Statement> getExtendedDepends(Statement stm, IntSet dependFacts)
	{
		switch(stm.getKind())
		{
		case NORMAL:
			return getExtendedDependsNormalStm((NormalStatement)stm, dependFacts);
		case PARAM_CALLER:
			return getExtendedDependsParamCallerStm((ParamCaller)stm, dependFacts);
		case PHI:
			return getExtendedDependsPhiStm((PhiStatement)stm, dependFacts);
		default:
			return null;
		}
	}
	public Iterator<Statement> getReachedStatements(final IntSet facts)
	{
		return mFunct.getReachedStatements(facts);
	}
	public IntSet getStatementFacts(Statement stm)
	{
		return mFunct.getStatementFacts(stm);
	}
	public Iterator<NormalStatement> getAllocSources(final IntSet facts)
	{
		return Iterators.filter(mAllocSources.iterator(), new Predicate<NormalStatement>()
		{
			@Override
			public boolean apply(NormalStatement normalStm)
			{
				IntSet oldFact = mFunct.getStatementFacts(normalStm);
				if(oldFact == null)
					return facts.isEmpty();
				else
					return facts.isSubset(oldFact);
			}	
		});
	}
	public Iterator<Object> getConstants(final IntSet facts)
	{
		return Iterators.filter(mConstants.keySet().iterator(), new Predicate<Object>()
		{
			@Override
			public boolean apply(Object val)
			{
				IntSet oldFacts = mConstants.get(val);
				if(oldFacts == null)
					return false;
				else
					return facts.isSubset(oldFacts);
			}
		});
	}
	public Iterator<Pair<Statement, Statement>> getCallSources(final IntSet facts)
	{
		return Iterators.filter(mCallSources.iterator(), new Predicate<Pair<Statement, Statement>>()
		{
			@Override
			public boolean apply(Pair<Statement, Statement> call)
			{
				IntSet oldFact = mFunct.getStatementFacts(call.getKey());
				if(oldFact == null)
					return facts.isEmpty();
				else
					return facts.isSubset(oldFact);
			}	
		});
	}
}
