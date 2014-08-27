package org.droidslicer.graph.entity.resolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.graph.entity.CSQLiteDbInputUnit;
import org.droidslicer.graph.entity.CSQLiteDbOutputUnit;
import org.droidslicer.ifds.BypassFlowFunctions;
import org.droidslicer.ifds.CallRecords;
import org.droidslicer.ifds.DependencySolver;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.solver.ConcreteValueSolver;
import org.droidslicer.value.solver.ValueSourceFunctions;
import org.droidslicer.value.solver.ValueSourceSolver;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.SparseIntSet;

public class SQLiteStatementResolver extends InvocationEntityResolver
{
	private final boolean mIsRead;
	public SQLiteStatementResolver(MethodReference method, boolean isStatic, boolean isRead)
	{
		super(method, isStatic);
		if(isStatic)
			throw new IllegalArgumentException("API to SQLiteStatement must be non-static");
		TypeReference clazzTypeRef = method.getDeclaringClass();
		if(!clazzTypeRef.getName().equals(TypeId.ANDROID_SQLITE_STATEMENT.getTypeReference().getName()))
			throw new IllegalArgumentException("The declaring class of the method must be android.database.sqlite.SQLiteStatement");
		mIsRead = isRead;
		if(isRead)
		{
			TypeReference retType = method.getReturnType();
			if(retType.equals(TypeReference.Void))
				throw new IllegalArgumentException("A read SQLiteStatement must have return value");
		}
	}
	
	protected Pair<Set<NormalReturnCaller>, CallRecords> findSources(
			UnitsResolverContext ctx, ParamCaller paramStm, boolean recordCalls, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Finding source of SQLiteStatement", 100);
			AndroidAnalysisContext analysisCtx = ctx.getAnalysisContext();
			ValueSourceSolver valSrcSolver = new ValueSourceSolver(analysisCtx, new ValueSourceFunctions()
			{
				@Override
				public IUnaryFlowFunction getCallFlowFunction(Statement caller,
						Statement callee, Statement ret)
				{
					if(caller instanceof NormalReturnCaller)
					{
						CGNode calleeNode = callee.getNode();
						IMethod calleeMethod = calleeNode.getMethod();
						IClass calleeClass = calleeMethod.getDeclaringClass();
						ClassLoaderReference calleeClassLoaderRef = calleeClass.getClassLoader().getReference();
						if(calleeClassLoaderRef.equals(ClassLoaderReference.Primordial))
							return new ValueSourceFunctions.KillReportFlowFunction(caller, callee);
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
			}, new CallRecords(analysisCtx.getCallGraph()));
			valSrcSolver.setIsRecordCalls(recordCalls);
			
			Set<NormalReturnCaller> retSources = new HashSet<NormalReturnCaller>(); 
			valSrcSolver.solve(paramStm, new SubProgressMonitor(monitor, 95));
			IntSet paramFacts = valSrcSolver.getStatementFacts(paramStm);
			Iterator<Pair<Statement, Statement>> callSources = valSrcSolver.getCallSources(paramFacts);
			while(callSources.hasNext())
			{
				Pair<Statement, Statement> pair = callSources.next();
				Statement src = pair.getLeft();
				switch(src.getKind())
				{
				case NORMAL_RET_CALLER:
					{
						NormalReturnCaller retCaller = (NormalReturnCaller)src;
						SSAAbstractInvokeInstruction invokeInst = retCaller.getInstruction();
						MethodReference declaredTarget = invokeInst.getDeclaredTarget();
						if(invokeInst.isStatic() || 
							invokeInst.getNumberOfParameters() <= 0 || 
							!declaredTarget.getName().equals(MethodId.ANDROID_SQLITE_DB_COMPILE_STM.getMethodReference().getName()))
						{
							continue;
						}
						retSources.add(retCaller);
						break;
					}
				default:
					break;
				}				
			}
			return Pair.of(retSources, recordCalls ? valSrcSolver.getCallRecords() : null);
		}
		finally
		{
			monitor.done();
		}
	}

	protected ConcreteValue solveDbValue(ConcreteValueSolver valSolver, Collection<NormalReturnCaller> retCallers, CallRecords callRecords, ProgressMonitor monitor)
		throws CancelException
	{
		OrValue dbVals = new OrValue();
		try
		{
			monitor.beginTask("Solving Database value", retCallers.size() * 100);
			for(NormalReturnCaller retCaller : retCallers)
			{
				SSAAbstractInvokeInstruction compileInvoke = retCaller.getInstruction();
				Descriptor descriptor = compileInvoke.getDeclaredTarget().getDescriptor();
				if(!MethodId.ANDROID_SQLITE_DB_COMPILE_STM.getMethodReference().getDescriptor().equals(descriptor) || compileInvoke.isStatic())
					continue;
				int compileInstIdx = retCaller.getInstructionIndex();
				CGNode compileNode = retCaller.getNode();
				int dbValNum = compileInvoke.getReceiver();
				ParamCaller dbParam = new ParamCaller(compileNode, compileInstIdx, dbValNum);
				ConcreteValue dbVal = valSolver.solve(
						dbParam, 
						compileNode, 
						compileInstIdx, 
						TypeId.ANDROID_SQLITE_DB.getTypeReference(),
						callRecords,
						getResolveDepth(), 
						new SubProgressMonitor(monitor, 100));
				dbVals.addValue(dbVal);
			}
			return dbVals.simplify();
		}
		finally
		{
			monitor.done();
		}
	}
	protected void attachInflowStms(
			UnitsResolverContext ctx, CSQLiteDbOutputUnit outputUnit, Set<NormalReturnCaller> retCallers, CallRecords callRecords, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Finding in-flow statements of SQLiteStatement", 100);
			
			// Add the parameter of compileStatement(String) as in-flow statement
			Map<Statement, IntSet> seeds = new HashMap<Statement, IntSet>();
			for(NormalReturnCaller retCaller : retCallers)
			{
				SSAAbstractInvokeInstruction invokeInst = retCaller.getInstruction();
				Descriptor descriptor = invokeInst.getDeclaredTarget().getDescriptor();
				if(!MethodId.ANDROID_SQLITE_DB_COMPILE_STM.getMethodReference().getDescriptor().equals(descriptor) || invokeInst.isStatic())
					continue;
				ParamCaller strStm = new ParamCaller(retCaller.getNode(), retCaller.getInstructionIndex(), invokeInst.getUse(1));
				outputUnit.addInflowStatement(strStm);
				seeds.put(retCaller, SparseIntSet.singleton(1));
			}

			Collection<Statement> reachedStms;
			{
				Predicate<Statement> terminatorsPred = Predicate.falsePred();
				BypassFlowFunctions functs = new BypassFlowFunctions(terminatorsPred, 0);
				functs.setCutReturnToSynthetic(true);
				functs.setRecordBypassedCalls(false);
				DependencySolver dependSolver = new DependencySolver(ctx.getAnalysisContext(), functs);
				dependSolver.solve(seeds, callRecords, new SubProgressMonitor(monitor, 95));
				reachedStms = dependSolver.getReachedStatements().keySet();
			}
	
			for(Statement reachedStm : reachedStms)
			{
				if(!(reachedStm instanceof ParamCaller))
					continue;
				ParamCaller paramStm = (ParamCaller)reachedStm;
				SSAAbstractInvokeInstruction invokeInst = paramStm.getInstruction();
				if(invokeInst.isStatic() || invokeInst.getReceiver() != paramStm.getValueNumber())
					continue;
				MethodReference declaredTarget = invokeInst.getDeclaredTarget();
				String methodName = declaredTarget.getName().toString();
				int nParam = invokeInst.getNumberOfParameters();
				int instIdx = paramStm.getInstructionIndex();
				CGNode node = paramStm.getNode();
				int paramIdxStart;
				if(methodName.startsWith("bindAll"))
				{
					paramIdxStart = 1;					
				}
				else if(methodName.startsWith("bind") && declaredTarget.getParameterType(0).equals(TypeReference.Int))
				{
					paramIdxStart = 2;
				}
				else
					continue;
				for(int paramIdx = paramIdxStart; paramIdx < nParam; ++paramIdx)
				{
					ParamCaller bindStm = new ParamCaller(node, instIdx, invokeInst.getUse(paramIdx));
					outputUnit.addInflowStatement(bindStm);
				}
			}
		}
		finally
		{
			monitor.done();
		}
	}
	@Override
	public void resolve(UnitsResolverContext ctx,
			SSAInvokeInstruction invokeInst, int instIdx,
			ProgressMonitor monitor) throws CancelException 
	{
		try
		{
			monitor.beginTask("Resolving SQLiteDatabase API access", 100);
			int nParam = getNumberOfParameters();
			MethodReference declaredTarget = invokeInst.getDeclaredTarget();
			MethodReference methodRef = getMethodReference();
			if(!declaredTarget.getDescriptor().equals(methodRef.getDescriptor()) || 
				invokeInst.isStatic() != isStatic() ||
				invokeInst.getNumberOfParameters() != nParam)
			{
				return;
			}
			
			CGNode node = ctx.getCurrentNode();
			ParamCaller thisParam = new ParamCaller(node, instIdx, invokeInst.getReceiver());
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			if(mIsRead)
			{
				CSQLiteDbInputUnit inputUnit = new CSQLiteDbInputUnit(node, instIdx);
				NormalReturnCaller retStm = new NormalReturnCaller(node, instIdx);
				inputUnit.addOutflowStatement(retStm);
				
				Pair<Set<NormalReturnCaller>, CallRecords> pair = findSources(ctx, thisParam, true, new SubProgressMonitor(monitor, 20));
				Set<NormalReturnCaller> retCallers = pair.getLeft();
				CallRecords callRecords = pair.getRight();
				ConcreteValue dbVal = solveDbValue(valSolver, retCallers, callRecords, new SubProgressMonitor(monitor, 80));
				inputUnit.setValue(dbVal);
				ctx.addUnit(inputUnit);
			}
			else
			{
				CSQLiteDbOutputUnit outputUnit = new CSQLiteDbOutputUnit(node, instIdx);
				Pair<Set<NormalReturnCaller>, CallRecords> pair = findSources(ctx, thisParam, true, new SubProgressMonitor(monitor, 15));
				Set<NormalReturnCaller> retCallers = pair.getLeft();
				CallRecords callRecords = pair.getRight();
				ConcreteValue dbVal = solveDbValue(valSolver, retCallers, callRecords, new SubProgressMonitor(monitor, 70));
				outputUnit.setValue(dbVal);
				attachInflowStms(ctx, outputUnit, retCallers, callRecords, new SubProgressMonitor(monitor, 15));
				ctx.addUnit(outputUnit);
			}
		}
		finally
		{
			monitor.done();
		}
	}
}
