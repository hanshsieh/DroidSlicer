package org.droidslicer.ifds;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.analysis.RecordCallTabulationSolver;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.StatementUtils;
import org.droidslicer.util.StatementUtils.ValueDef;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.Utils;
import org.droidslicer.value.solver.ValueSourceFunctions;
import org.droidslicer.value.solver.ValueSourceSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.SparseIntSet;

public class DependencySolver
{
	public static enum DependType
	{
		ARRAY,
		CLASS_FIELD,
		NORMAL
	}
	public static interface FactsTransformer
	{
		public IntSet transform(DependType dependType, Statement srcStm, Statement dstStm, IntSet facts);
	}
	private static class IdentityFactsTransformer implements FactsTransformer
	{
		@Override
		public IntSet transform(DependType dependType, Statement srcStm, Statement dstStm, IntSet facts)
		{
			return facts;
		}		
	}
	public static class ExtendedDependsInstructionVisitor extends SSAInstruction.Visitor
	{
		protected DependType mDependType;
		protected Collection<Statement> mDependStms;
		protected NormalStatement mNormalStm;
		protected ProgressMonitor mMonitor;
		protected boolean mCanceled = false;
		protected DependencySolver mSolver;
		public ExtendedDependsInstructionVisitor(DependencySolver solver)
		{
			mSolver = solver;
		}
		public Pair<DependType, Collection<Statement>> visit(NormalStatement normalStm, ProgressMonitor monitor)
			throws CancelException
		{
			mDependType = null;
			mDependStms = null;
			mCanceled = false;
			mNormalStm = normalStm;
			mMonitor = monitor;
			SSAInstruction inst = normalStm.getInstruction();
			inst.visit(this);
			Pair<DependType, Collection<Statement>> result = null;
			if(mDependType != null && mDependStms != null)
				result = Pair.of(mDependType, mDependStms);
			mDependType = null;
			mDependStms = null;
			mNormalStm = null;
			if(mCanceled)
				throw CancelException.make("Operation canceled");
			return result;
		}
		@Override
		public void visitPut(SSAPutInstruction putInst)
		{
			mDependType = DependType.CLASS_FIELD;
			mDependStms = getExtendedPutInstDepends(mNormalStm, putInst);
		}
		@Override
		public void visitArrayStore(SSAArrayStoreInstruction arrayStoreInst) 
		{
			mDependType = DependType.ARRAY;
			try
			{
				mDependStms = getExtendedAStoreInstDepends(mNormalStm, arrayStoreInst, mMonitor);
			}
			catch(CancelException ex)
			{
				mCanceled = true;
			}
		}
		protected Collection<Statement> getExtendedAStoreInstDepends(
				NormalStatement normalStm, SSAArrayStoreInstruction astoreInst, ProgressMonitor monitor)
				throws CancelException
		{
			try
			{
				monitor.beginTask("Finding sources of a array reference", 100);
				// Find the sources of the array reference, and add dataflow facts to the sources
				int ref = astoreInst.getArrayRef();
				ValueDef valDef = StatementUtils.getValNumDefStatement(normalStm.getNode(), ref);
				if(valDef == null)
					return Collections.emptySet();
				if(!valDef.isConstant())
				{
					return getValueSources(valDef.getDefiningStatement(), new SubProgressMonitor(monitor, 100));
				}
				else
					return Collections.emptySet();
			}
			finally
			{
				monitor.done();
			}
		}
		protected Collection<Statement> getExtendedPutInstDepends(NormalStatement normalStm, SSAPutInstruction putInst)
		{
			AndroidAnalysisContext analysisCtx = mSolver.getAnalysisContext();
			FieldReference fieldRef = putInst.getDeclaredField();
			IClassHierarchy cha = analysisCtx.getClassHierarchy();
			IField field = cha.resolveField(fieldRef);
			if(field == null)
				return Collections.emptySet();
			Collection<Statement> readStms;
			if(putInst.isStatic())
			{
				StaticFieldKey fieldPointer = new StaticFieldKey(field);
				readStms = analysisCtx.getReadsToStaticField(fieldPointer);
			}
			else
			{
				LocalPointerKey refPointer = new LocalPointerKey(normalStm.getNode(), putInst.getRef());
				readStms = analysisCtx.getReadsToInstanceField(refPointer, field);
			}
			Set<Statement> result = new HashSet<Statement>();
			for(Statement readStm : readStms)
			{
				CGNode readNode = readStm.getNode();
				IMethod readMethod = readNode.getMethod();
				ClassLoaderReference readClassLoaderRef = readMethod.getDeclaringClass().getClassLoader().getReference();
				if(!readClassLoaderRef.equals(analysisCtx.getAnalysisScope().getApplicationLoader()) && !readMethod.isSynthetic())
					continue;
				result.add(readStm);
			}
			return result;
		}
		protected Collection<Statement> getValueSources(Statement stm, ProgressMonitor monitor)
				throws CancelException
		{
			AndroidAnalysisContext analysisCtx = mSolver.getAnalysisContext();
			CallRecords callRecords = mSolver.getCallRecords();
			Collection<Statement> sources = new LinkedHashSet<Statement>();
			ValueSourceSolver valSrcSolver = new ValueSourceSolver(analysisCtx, new ValueSourceFunctions()
			{
				@Override
				public IUnaryFlowFunction getCallFlowFunction(Statement caller,
						Statement callee, Statement ret)
				{
					if(caller instanceof NormalReturnCaller)
					{
						NormalReturnCaller retCaller = (NormalReturnCaller)caller;
						CGNode calleeNode = callee.getNode();
						IMethod calleeMethod = calleeNode.getMethod();
						IClass calleeClass = calleeMethod.getDeclaringClass();
						ClassLoaderReference calleeClassLoaderRef = calleeClass.getClassLoader().getReference();
						if(!calleeClassLoaderRef.equals(ClassLoaderReference.Application))
							return new ValueSourceFunctions.KillReportFlowFunction(retCaller, callee);
						else
							return IdentityFlowFunction.identity();
					}
					else
						return KillEverything.singleton();
				}
				@Override
				public IFlowFunction getReturnFlowFunction(Statement call, Statement exit,
						Statement ret)
				{
					return getUnbalancedReturnFlowFunction(exit, ret);
				}
				@Override
				public IFlowFunction getUnbalancedReturnFlowFunction(Statement exit,
						Statement ret)
				{
					IMethod retMethod = ret.getNode().getMethod();
					if(retMethod.isSynthetic())
						return KillEverything.singleton();
					ClassLoaderReference retClassLoaderRef = retMethod.getDeclaringClass().getClassLoader().getReference();
					if(!retClassLoaderRef.equals(ClassLoaderReference.Application))
						return KillEverything.singleton();
					return IdentityFlowFunction.identity();
				}
			}, callRecords);
			
			// Run the value solver
			{
				valSrcSolver.setIsRecordCalls(true);
				valSrcSolver.solve(stm, monitor);
				callRecords = valSrcSolver.getCallRecords();
			}
			
			IntSet paramFacts = valSrcSolver.getStatementFacts(stm);
			for(Iterator<Pair<Statement, Statement>> itr = valSrcSolver.getCallSources(paramFacts); 
				itr.hasNext(); )
			{
				Pair<Statement, Statement> call = itr.next();
				Statement src = call.getKey();
				switch(src.getKind())
				{
				case NORMAL_RET_CALLER:
					sources.add(src);
					break;
				default:
					break;
				}
			}
			for(Iterator<NormalStatement> itr = valSrcSolver.getAllocSources(paramFacts);
					itr.hasNext();)
			{
				NormalStatement normalStm = itr.next();
				SSAInstruction newInst = normalStm.getInstruction();
				if(newInst instanceof SSANewInstruction)
				{
					sources.add(normalStm);
				}
			}
			return sources;
		}
	}
	private static final Logger mLogger = LoggerFactory.getLogger(DependencySolver.class);
	private static final double MAX_MEMORY_USAGE = 0.90;
	private AndroidAnalysisContext mAnalysisCtx;
	private final IDependencyFlowFunctions mFlowFuncts;
	private CallRecords mCallRecords;
	private Map<Statement, MutableIntSet> mFacts = new HashMap<Statement, MutableIntSet>();
	private Map<Statement, MutableIntSet> mHistorySeeds = new HashMap<Statement, MutableIntSet>();
	private FactsTransformer mExtendedFactsTransformer = new IdentityFactsTransformer();
	private ExtendedDependsInstructionVisitor mExtenedDependsInstVisitor = new ExtendedDependsInstructionVisitor(this);
	private int mMaxRounds = -1;
	
	public DependencySolver(AndroidAnalysisContext analysisCtx, IDependencyFlowFunctions flowFuncts)
	{
		if(analysisCtx == null || flowFuncts == null)
			throw new IllegalArgumentException();
		mAnalysisCtx = analysisCtx;
		mFlowFuncts = flowFuncts;
	}
	public AndroidAnalysisContext getAnalysisContext()
	{
		return mAnalysisCtx;
	}
	public int getMaxRounds()
	{
		return mMaxRounds;
	}
	public void setMaxRounds(int round)
	{
		mMaxRounds = round;
	}
	public void setExtendedFactsTransformer(FactsTransformer transformer)
	{
		if(transformer == null)
			throw new IllegalArgumentException();
		mExtendedFactsTransformer = transformer;
	}
	public FactsTransformer getExtendedFactsTransformer()
	{
		return mExtendedFactsTransformer;
	}
	public void setExtendedDependsInstructionVisitor(ExtendedDependsInstructionVisitor visitor)
	{
		if(visitor == null)
			throw new IllegalArgumentException();
		mExtenedDependsInstVisitor = visitor;
	}
	public ExtendedDependsInstructionVisitor getExtendedDependsInstructionVisitor()
	{
		return mExtenedDependsInstVisitor;
	}
	public IDependencyFlowFunctions getFlowFunctions()
	{
		return mFlowFuncts;
	}
	public CallRecords getCallRecords()
	{
		return mCallRecords;
	}
	public Map<Statement, ? extends IntSet> getReachedStatements()
	{
		return mFacts;
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
	public void solve(Map<Statement, ? extends IntSet> seeds, CallRecords callRecords, ProgressMonitor monitor)
		throws CancelException
	{
		if(seeds == null || callRecords == null || monitor == null)
			throw new IllegalArgumentException();
		try
		{
			monitor.beginTask("Find dependencies of statements", 100);
			mLogger.debug("Solving dependency");
			mCallRecords = callRecords;
			mFacts.clear();
			mHistorySeeds.clear();
			for(Map.Entry<Statement, ? extends IntSet> entry : seeds.entrySet())
			{
				Statement stm = entry.getKey();
				MutableSparseIntSet facts = MutableSparseIntSet.make(entry.getValue());
				if(facts.isEmpty())
					continue;
				mFacts.put(stm, facts);
				mHistorySeeds.put(stm, facts);
			}
			Set<Statement> roundSeeds = seeds.keySet();
			int round = 0;
			while(roundSeeds != null && !roundSeeds.isEmpty())
			{
				if(mMaxRounds >= 0 && round >= mMaxRounds)
					break;
				roundSeeds = solveInternal(roundSeeds, new SubProgressMonitor(monitor, 20));
				++round;
			}
		}
		finally
		{
			monitor.done();
			mLogger.debug("Finish solving dependencies");
		}
	}
	protected Set<Statement> solveInternal(Set<Statement> seeds, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Internal round of dependencies solving", 100);
			Set<Statement> nextSeeds = new LinkedHashSet<Statement>();
			for(Statement seed : seeds)
			{
				IntSet facts = mHistorySeeds.get(seed);
				if(facts == null || facts.isEmpty())
					continue;
				checkMemoryUsage();
				mFlowFuncts.clearSeeds();
				mFlowFuncts.addSeed(seed, SparseIntSet.singleton(1));
				BypassSliceProblem problem = new BypassSliceProblem(new SDGSupergraph(mAnalysisCtx.getSDG()), mFlowFuncts);
				RecordCallTabulationSolver<Object> solver = RecordCallTabulationSolver.create(problem, mCallRecords, new SubProgressMonitor(monitor, 60));
				solver.setRecordCalls(true);
				TabulationResult<Statement, PDG, Object> tResult;
				tResult = solver.solve();
				mCallRecords = solver.getCallRecords();
				for(Statement stm : tResult.getSupergraphNodesReached())
				{
					MutableIntSet oldFacts = mFacts.get(stm);
					if(oldFacts == null)
					{
						oldFacts = MutableSparseIntSet.makeEmpty();
						mFacts.put(stm, oldFacts);
					}
					if(!oldFacts.addAll(facts))
						continue;
					Pair<DependType, Collection<Statement>> pair = getExtendedDepends(stm, new SubProgressMonitor(monitor, 10));
					if(pair == null)
						continue;
					DependType dependType = pair.getLeft();
					for(Statement depend : pair.getRight())
					{
						IntSet dependFacts = mExtendedFactsTransformer.transform(dependType, stm, depend, facts);
						if(dependFacts.isEmpty())
							continue;
						MutableIntSet oldSeedFacts = mHistorySeeds.get(depend);
						if(oldSeedFacts == null)
						{
							oldSeedFacts = MutableSparseIntSet.makeEmpty();
							mHistorySeeds.put(depend, oldSeedFacts);
						}
						if(oldSeedFacts.addAll(dependFacts))
							nextSeeds.add(depend);
					}
				}
			}
			return nextSeeds;
		}
		finally
		{
			monitor.done();
		}
	}
	
	protected Pair<DependType, Collection<Statement>> getExtendedDepends(Statement stm, ProgressMonitor monitor)
		throws CancelException
	{
		switch(stm.getKind())
		{
		case NORMAL:
			return getExtendedDependsInstructionVisitor().visit((NormalStatement)stm, monitor);
		default:
			break;
		}
		return null;
	}
}
