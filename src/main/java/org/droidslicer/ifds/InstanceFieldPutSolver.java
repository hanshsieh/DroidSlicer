package org.droidslicer.ifds;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.IntSet;

public class InstanceFieldPutSolver
{
	private final static Logger mLogger = LoggerFactory.getLogger(InstanceFieldPutSolver.class);
	private final static double MAX_MEMORY_USAGE = 0.8;
	private final AndroidAnalysisContext mAnalysisCtx;
	private CallRecords mCallRecords = null;
	public InstanceFieldPutSolver(AndroidAnalysisContext analysisCtx)
	{
		mAnalysisCtx = analysisCtx;
	}
	public Collection<Statement> findSources(Collection<LocalPointerKey> pointers, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Finding source of local pointer", 100);
			
			// Find the defining statements for the local pointer
			Set<Statement> seeds = new HashSet<Statement>();
			for(LocalPointerKey pointer : pointers)
			{
				ValueDef valueDef = StatementUtils.getValNumDefStatement(pointer.getNode(), pointer.getValueNumber());
				if(valueDef.isConstant())
					continue;
				Statement stm = valueDef.getDefiningStatement();
				seeds.add(stm);
			}
			
			// Find the source of the statements
			ValueSourceSolver valSrcSolver = new ValueSourceSolver(mAnalysisCtx, new ValueSourceFunctions()
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
						if(calleeClassLoaderRef.equals(ClassLoaderReference.Primordial))
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
					if(retMethod.isSynthetic() || 
						!retMethod.getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application))
					{
						return KillEverything.singleton();
					}
					else
						return IdentityFlowFunction.identity();
				}
			}, mCallRecords);
			valSrcSolver.setIsRecordCalls(true);
			
			// Find the sources
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 98);
				valSrcSolver.solve(seeds, subMonitor);
				mCallRecords.addAllCalls(valSrcSolver.getCallRecords());
			}

			
			// Collect the sources
			Set<Statement> sources = new HashSet<Statement>();
			AnalysisScope scope = mAnalysisCtx.getAnalysisScope();
			for(Statement seed : seeds)
			{
				IntSet paramFacts = valSrcSolver.getStatementFacts(seed);
				for(Iterator<Pair<Statement, Statement>> itr = valSrcSolver.getCallSources(paramFacts); 
					itr.hasNext(); )
				{
					Pair<Statement, Statement> call = itr.next();
					Statement src = call.getLeft();
					switch(src.getKind())
					{
					case NORMAL_RET_CALLER:
						{
							ClassLoaderReference classLoaderRef = src.getNode().getMethod().getDeclaringClass().getClassLoader().getReference();
							if(classLoaderRef.equals(scope.getApplicationLoader()))
								sources.add(src);
							break;
						}
					default:
						break;
					}
				}
				for(Iterator<NormalStatement> itr = valSrcSolver.getAllocSources(paramFacts);
						itr.hasNext();)
				{
					NormalStatement normalStm = itr.next();
					ClassLoaderReference classLoaderRef = normalStm.getNode().getMethod().getDeclaringClass().getClassLoader().getReference();
					if(!classLoaderRef.equals(scope.getApplicationLoader()))
						continue;
					SSAInstruction newInst = normalStm.getInstruction();
					if(newInst instanceof SSANewInstruction)
					{
						sources.add(normalStm);
					}
				}
			}
			
			return sources;
		}
		finally
		{
			monitor.done();
		}
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
	public Collection<Statement> solve(Collection<LocalPointerKey> pointers, CallRecords callRecords, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Finding recursive object backward dependency", 100);
			mCallRecords = callRecords;
			Set<Statement> result = new HashSet<Statement>();
			while(!pointers.isEmpty())
			{
				// Find the sources of these pointers
				Collection<Statement> sources = findSources(pointers, new SubProgressMonitor(monitor, 10));
				if(sources.isEmpty())
					break;
				
				checkMemoryUsage();
				// Find the statements of instructions putting values to the instances pointed by the pointers
				InstanceFieldPutProblem problem = new InstanceFieldPutProblem(mAnalysisCtx.getSDG(), sources);
				RecordCallTabulationSolver<Object> solver = 
						RecordCallTabulationSolver.create(problem, mCallRecords, new SubProgressMonitor(monitor, 10));
				solver.setRecordCalls(true);
				solver.solve();
				mCallRecords.addAllCalls(solver.getCallRecords());
				
				// Clear the set of pointers we need to solve
				pointers = new HashSet<LocalPointerKey>();
				Set<NormalStatement> putStms = problem.getPutStatements();
				
				// For each such put statement
				for(NormalStatement putStm : putStms)
				{
					if(!result.add(putStm))
						continue;
					
					// If it hasn't been added to the result
					SSAInstruction inst = putStm.getInstruction();
					if(!(inst instanceof SSAPutInstruction))
						continue;
					SSAPutInstruction putInst = (SSAPutInstruction)inst;
					int val = putInst.getVal();
					CGNode node = putStm.getNode();
					
					// Add it to the set of pointers to be solved in the next round
					pointers.add(new LocalPointerKey(node, val));
				}
			}
			return result;
		}
		finally
		{
			mCallRecords = null;
			monitor.done();
		}
	}
}
