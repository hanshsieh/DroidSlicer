package org.droidslicer.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.ifds.BypassFlowFunctions;
import org.droidslicer.ifds.CallRecords;
import org.droidslicer.ifds.DependencySolver;
import org.droidslicer.value.solver.ValueSourceFunctions;
import org.droidslicer.value.solver.ValueSourceSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.SparseIntSet;

public class EntityUtils
{
	private final static Logger mLogger = LoggerFactory.getLogger(EntityUtils.class);
	protected static class Context
	{
		private final AndroidAnalysisContext mAnalysisCtx;
		private CallRecords mCallFlows;
		private Set<Statement> mFlowStms = new HashSet<Statement>();
		private Set<Statement> mSrcFlowStms = new HashSet<Statement>();
		public Context(AndroidAnalysisContext analysisCtx)
		{
			mAnalysisCtx = analysisCtx;
		}
		public AndroidAnalysisContext getAnalysisContext()
		{
			return mAnalysisCtx;
		}
		public void addInOutFlowStatement(Statement stm)
		{
			mFlowStms.add(stm);
		}
		public void addSourceInOutFlowStatement(Statement stm)
		{
			mSrcFlowStms.add(stm);
		}
		public Collection<Statement> getInOutFlowStatements()
		{
			return mFlowStms;
		}
		public Collection<Statement> getSourceInOutFlowStatements()
		{
			return mSrcFlowStms;
		}
		public void setCallFlows(CallRecords callFlows)
		{
			mCallFlows = callFlows;
		}
		public CallRecords getCallFlows()
		{
			return mCallFlows;
		}
	}
	/**
	 * Find the input or output statements of an InputStream or OutputStream instance.
	 * @param ctx
	 * @param seeds
	 * @param isInput
	 * @param monitor
	 * @return the set of statements for the in/out flows
	 * @throws CancelException
	 */
	protected static Set<Statement> findInOutflows(
			Context ctx, Map<Statement, IntSet> seeds, boolean isInput, ProgressMonitor monitor)
		throws CancelException
	{
		if(monitor.isCanceled())
			throw CancelException.make("Operation canceled");
		monitor.beginTask("Finding in/out flow of InputStream or OutputStream related instances", 100);
		try
		{
			AndroidAnalysisContext analysisCtx = ctx.getAnalysisContext();
			Set<Statement> reachedStms;
			Set<Statement> stmsToTracked = new HashSet<Statement>();
			{
				Predicate<Statement> terminatorsPred = Predicate.falsePred();
				BypassFlowFunctions functs = new BypassFlowFunctions(terminatorsPred, 0);
				functs.setCutReturnToSynthetic(true);
				functs.setRecordBypassedCalls(false);
				DependencySolver dependSolver = new DependencySolver(analysisCtx, functs);
				dependSolver.solve(seeds, ctx.getCallFlows(), new SubProgressMonitor(monitor, 95));
				reachedStms = dependSolver.getReachedStatements().keySet();
				ctx.setCallFlows(dependSolver.getCallRecords());
			}
	
			if(mLogger.isDebugEnabled())
			{
				StringBuilder builder = new StringBuilder();
				builder.append("Reached statements of in finding in/out flow of stream");
				for(Statement reachedStm : reachedStms)
				{
					builder.append('\t');
					builder.append(reachedStm);
					builder.append('\n');
				}
				mLogger.debug("{}", builder.toString());
			}
			for(Statement reachedStm : reachedStms)
			{
				if(!reachedStm.getKind().equals(Statement.Kind.PARAM_CALLER))
					continue;
				
				// The statement is PARAM_CALLER
				ParamCaller paramStm = (ParamCaller)reachedStm;
				CGNode callerNode = paramStm.getNode();
				SSAAbstractInvokeInstruction invokeInst =  paramStm.getInstruction();
				int paramValNum = paramStm.getValueNumber();
				MethodReference declaredTarget = invokeInst.getDeclaredTarget();
				
				// If the PARAM_CALLER statement is the receiver of an non-static method
				if(!invokeInst.isStatic() && invokeInst.getReceiver() == paramValNum)
				{
					// If the invoked method isn't constructor
					if(!declaredTarget.getName().equals(MethodReference.initAtom))
					{
						if(isInput)
						{
							int nParam = declaredTarget.getNumberOfParameters();
							boolean hasArrayParam = false;
							for(int paramIdx = 0; paramIdx < nParam; ++paramIdx)
							{
								TypeReference oParamType = declaredTarget.getParameterType(paramIdx);
								
								// If it has array parameter, then we assume that the input data is returned by the
								// array parameter.
								if(oParamType.isArrayType())
								{
									hasArrayParam = true;
									int useIdx = invokeInst.isStatic() ? paramIdx : paramIdx + 1;
									int oParamValNum = invokeInst.getUse(useIdx);
									
									// Record that the source statement of the parameter should be tainted 
									ParamCaller oParamStm = new ParamCaller(callerNode, paramStm.getInstructionIndex(), oParamValNum);
									ctx.addSourceInOutFlowStatement(oParamStm);
								}
							}
							
							// If it doesn't have array parameter, then we assume that the input data is 
							// returned by return value.
							if(!hasArrayParam)
							{
								TypeReference retType = declaredTarget.getReturnType();
								if(!retType.equals(TypeReference.Void))
								{
									NormalReturnCaller retStm = new NormalReturnCaller(callerNode, paramStm.getInstructionIndex());
									ctx.addInOutFlowStatement(retStm);
								}
							}
						}
						else
						{
							
							// Check if the method has array parameter
							int nParam = declaredTarget.getNumberOfParameters();
							boolean hasArrayParam = false;
							for(int paramIdx = 0; paramIdx < nParam; ++paramIdx)
							{
								TypeReference oParamType = declaredTarget.getParameterType(paramIdx);
								
								// If it has array parameter, then we only consider the array parameter as the possible out-flow
								// statement.
								if(oParamType.isArrayType())
								{
									hasArrayParam = true;
									int useIdx = invokeInst.isStatic() ? paramIdx : paramIdx + 1;
									int oParamValNum = invokeInst.getUse(useIdx);
									ParamCaller oParamStm = new ParamCaller(callerNode, paramStm.getInstructionIndex(), oParamValNum);
									ctx.addInOutFlowStatement(oParamStm);
								}
							}
							
							// If the method doesn't have any array parameter, then we consider all the parameters as the
							// possible out-flow statement.
							if(!hasArrayParam)
							{
								// Mark the parameter (including 'this') as the possible out-flow of the
								// OutputStream
								// E.g. BitMap.compress(...)
								int nUse = invokeInst.getNumberOfParameters();
								for(int useIdx = 0; useIdx < nUse; ++useIdx)
								{
									int oParamValNum = invokeInst.getUse(useIdx);
									if(oParamValNum == paramValNum)
										continue;
									ParamCaller oParamStm = new ParamCaller(callerNode, paramStm.getInstructionIndex(), oParamValNum);
									ctx.addInOutFlowStatement(oParamStm);
								}
							}
						}
					}
				}
				else
				{
					if(isInput)
					{
						// If the invoke instruction has return value, assume that the return value
						// is dependent on the input stream.
						if(invokeInst.hasDef())
						{
							NormalReturnCaller retCaller = new NormalReturnCaller(callerNode, paramStm.getInstructionIndex());
							ctx.addInOutFlowStatement(retCaller);
						}
					}
					
					// The statement isn't a receiver
					// If the invocation isn't static
					if(!invokeInst.isStatic())
					{
						int receiver = invokeInst.getReceiver();
						
						// If the statement is a parameter of a construction invocation
						if(invokeInst.isSpecial() && 
							declaredTarget.getName().equals(MethodReference.initAtom))
						{
							// Assume that the class of the constructor is a wrapper of InputStream/OutputStream 
							// Add the receiver to the list of statements that the sources should be tracked
							ParamCaller receiverStm = new ParamCaller(callerNode, paramStm.getInstructionIndex(), receiver);
							stmsToTracked.add(receiverStm);
						}
						
						// If it is output stream
						if(!isInput)
						{
							// Assume that the method is to write the content of the receiver to the output stream
							// E.g. java.util.Properties#store(OutputStream, String), 
							// org.apache.http.HttpEntity#writeTo(OutputStream).
							ParamCaller oParamStm = new ParamCaller(callerNode, paramStm.getInstructionIndex(), receiver);
							ctx.addInOutFlowStatement(oParamStm);
						}
					}
				}
			}
			return stmsToTracked;
		}
		finally
		{
			monitor.done();
		}
	}
	protected static Set<Statement> findSources(Context ctx, Statement stm, ProgressMonitor monitor)
		throws CancelException
	{
		if(monitor.isCanceled())
			throw CancelException.make("Operation canceled");
		monitor.beginTask("Finding source of FileInputStream or FileOutputStream related instances", 100);
		try
		{
			ValueSourceSolver solver = new ValueSourceSolver(ctx.getAnalysisContext(), new ValueSourceFunctions()
			{
				@Override
				public IUnaryFlowFunction getCallFlowFunction(Statement caller,
						Statement callee, Statement ret)
				{
					switch(caller.getKind())
					{
					case NORMAL_RET_CALLER:
						{
							NormalReturnCaller retCaller = (NormalReturnCaller)caller;
							CGNode calleeNode = callee.getNode();
							IMethod calleeMethod = calleeNode.getMethod();
							IClass calleeClass = calleeMethod.getDeclaringClass();
							ClassLoaderReference calleeClassLoaderRef = calleeClass.getClassLoader().getReference();
							if(calleeClassLoaderRef.equals(ClassLoaderReference.Primordial) || (calleeClass instanceof SyntheticClass))
								return new ValueSourceFunctions.KillReportFlowFunction(retCaller, callee);
							else
								return IdentityFlowFunction.identity();
						}
					case HEAP_RET_CALLER:
						{
							CGNode calleeNode = callee.getNode();
							IMethod calleeMethod = calleeNode.getMethod();
							IClass calleeClass = calleeMethod.getDeclaringClass();
							ClassLoaderReference calleeClassLoaderRef = calleeClass.getClassLoader().getReference();
							if(calleeClassLoaderRef.equals(ClassLoaderReference.Application))
								return IdentityFlowFunction.identity();
							else
								return KillEverything.singleton();				
						}
					default:
						return KillEverything.singleton();
					}
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
			}, ctx.getCallFlows());
			
			SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 100);
			solver.solve(stm, subMonitor);
			
			IntSet facts = solver.getStatementFacts(stm);
			Set<Statement> result = new HashSet<Statement>();
			for(Iterator<Pair<Statement, Statement>> itr = solver.getCallSources(facts);
					itr.hasNext();)
			{
				Pair<Statement, Statement> pair = itr.next();
				Statement src = pair.getLeft();
				switch(src.getKind())
				{
				case NORMAL_RET_CALLER:
					result.add(src);
					break;
				default:
					break;
				}
			}
			for(Iterator<NormalStatement> itr = solver.getAllocSources(facts);
					itr.hasNext();)
			{
				NormalStatement normalStm = itr.next();
				SSAInstruction inst = normalStm.getInstruction();
				if(inst instanceof SSANewInstruction)
					result.add(normalStm);
			}
			return result;
		}
		finally
		{
			monitor.done();
		}
	}
	public static Collection<Statement> computeInOutStreamFlowStatements(
			AndroidAnalysisContext analysisCtx, StatementWithInstructionIndex startStm, boolean isInput, ProgressMonitor monitor)
		throws CancelException
	{
		return computeInOutStreamFlowStatements(analysisCtx, startStm, isInput, null, monitor);
	}
	public static Collection<Statement> computeInOutStreamFlowStatements(
			AndroidAnalysisContext analysisCtx, StatementWithInstructionIndex startStm, boolean isInput, CallRecords callRecords, ProgressMonitor monitor)
		throws CancelException
	{
		if(monitor.isCanceled())
			throw CancelException.make("Operation canceled");
		monitor.beginTask("Finding the in-flow statements of FileOutputStream", 100);
		try
		{
			// Prepare the initial seeds
			Map<Statement, IntSet> seeds = new HashMap<Statement, IntSet>();
			Set<Statement> trackedSeeds = new HashSet<Statement>();
			seeds.put(startStm, SparseIntSet.singleton(1));
			trackedSeeds.add(startStm);
			Context ctx = new Context(analysisCtx);
			ctx.setCallFlows(callRecords == null ? new CallRecords(analysisCtx.getCallGraph()) : callRecords);
			do
			{
				Set<Statement> stmsToTracked;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					stmsToTracked = findInOutflows(ctx, seeds, isInput, subMonitor);
				}
				seeds.clear();
				for(Statement stmToTrack : stmsToTracked)
				{
					Set<Statement> sources;
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						sources = findSources(ctx, stmToTrack, subMonitor);
					}
					for(Statement source : sources)
					{
						if(!trackedSeeds.contains(source))
						{
							seeds.put(source, SparseIntSet.singleton(1));
							trackedSeeds.add(source);
						}
					}
				}
				
			}while(!seeds.isEmpty());
			
			for(Statement stm : ctx.getSourceInOutFlowStatements())
			{
				Set<Statement> sources;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					sources = findSources(ctx, stm, subMonitor);
				}
				for(Statement source : sources)
				{
					ctx.addInOutFlowStatement(source);
				}
			}
			return ctx.getInOutFlowStatements();
		}
		finally
		{
			monitor.done();
		}
	}
}
