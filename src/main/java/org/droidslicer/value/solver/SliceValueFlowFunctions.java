package org.droidslicer.value.solver;

import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.FakeRootClass;
import com.ibm.wala.ipa.slicer.HeapStatement.HeapReturnCaller;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.Pair;

public class SliceValueFlowFunctions extends ValueSourceFunctions
{
	private final static int MAX_CACHE_SIZE = 100000;
	private final IClass mAndroidCtxClass;
	private final SliceValueSolver.CallResultSolver mCallResultSolver;
	private final LoadingCache<Pair<Statement, Statement>, IUnaryFlowFunction> mCallFlowFunct = 
		CacheBuilder.newBuilder()
		.maximumSize(MAX_CACHE_SIZE)
		.softValues()
		.build(new CacheLoader<Pair<Statement, Statement>, IUnaryFlowFunction>()
		{
			@Override
			public IUnaryFlowFunction load(
					Pair<Statement, Statement> key)
					throws Exception
			{
				Statement caller = key.fst;
				Statement callee = key.snd;
				int instIdx;
				if(caller instanceof NormalReturnCaller)
					instIdx = ((NormalReturnCaller) caller).getInstructionIndex();
				else if(caller instanceof HeapReturnCaller)
					instIdx = ((HeapReturnCaller) caller).getCallIndex();
				else
					return KillEverything.singleton();
				CGNode calleeNode = callee.getNode();
				boolean callResultConsume = isCallConsumed(caller, calleeNode, instIdx);
				if(!callResultConsume)
					return IdentityFlowFunction.identity();
				CGNode callerNode = caller.getNode();
				IR ir = callerNode.getIR();
				SSAInstruction inst = ir.getInstructions()[instIdx];
				if(!(inst instanceof SSAAbstractInvokeInstruction))
					return KillEverything.singleton();
				IMethod calleeMethod = calleeNode.getMethod();
				SliceValueSolver.CallResultSolverEntry entry = mCallResultSolver.getEntry(calleeMethod.getReference());
				if(entry == null)
				{
					//return IdentityFlowFunction.identity();
					// Some static library methods are used to initialize static class fields
					if(calleeMethod.isStatic())
						return IdentityFlowFunction.identity();
					else if(Utils.isReturnThis(calleeNode))
						addRet2ParamFlow(new ParamCaller(callerNode, instIdx, inst.getUse(0)));
				}
				else
				{
					for(int useIdx = 0; useIdx < inst.getNumberOfUses(); ++useIdx)
					{
						if(entry.shouldTrackParameter(useIdx))
						{
							int use = inst.getUse(useIdx);
							addRet2ParamFlow(new ParamCaller(callerNode, instIdx, use));
						}
					}
				}
				return new ValueSourceFunctions.KillReportFlowFunction(caller, callee);
			}
			private boolean isCallConsumed(Statement callerStm, CGNode calleeNode, int instIdx)
			{
				IMethod calleeMethod = calleeNode.getMethod();
				IClass calleeClass = calleeMethod.getDeclaringClass();
				if(calleeClass instanceof FakeRootClass)
					return false;
				ClassLoaderReference calleeClassLoaderRef = calleeClass.getClassLoader().getReference();
				if(calleeClassLoaderRef.equals(ClassLoaderReference.Primordial))
				{
					if(TypeId.ANDROID_CONTEXT_WRAPPER.getTypeReference().equals(calleeClass.getReference()) && !calleeMethod.isStatic())
						return false;
					
					// It is a static method of initiating a SQLiteDatabase instance (e.g. SQLiteDatabase#create(SQLiteDatabase.CursorFactory)
					if(TypeId.ANDROID_SQLITE_DB.getTypeReference().getName().equals(calleeMethod.getReturnType().getName()) && 
						calleeMethod.isStatic())
						return false;

					return true;
				}
				else
					return false;
			}
		});
	public SliceValueFlowFunctions(AndroidAnalysisContext analysisCtx, SliceValueSolver.CallResultSolver callResultSolver)
	{
		if(analysisCtx == null || callResultSolver == null)
			throw new IllegalArgumentException();
		mAndroidCtxClass = analysisCtx.getClassHierarchy().lookupClass(TypeId.ANDROID_CONTEXT.getTypeReference());
		if(mAndroidCtxClass == null)
			throw new IllegalArgumentException("Fail to find android.content.Context in class hierarchy");
		mCallResultSolver = callResultSolver;
	}
	@Override
	public IUnaryFlowFunction getCallFlowFunction(Statement caller,
			Statement callee, Statement ret)
	{
		return mCallFlowFunct.getUnchecked(Pair.make(caller, callee));
	}
	@Override
	public IFlowFunction getReturnFlowFunction(Statement call, Statement exit,
			Statement ret)
	{
		return IdentityFlowFunction.identity();
	}
	@Override
	public IFlowFunction getUnbalancedReturnFlowFunction(Statement exit,
			Statement ret)
	{
		return IdentityFlowFunction.identity();
		/*
		IMethod retMethod = ret.getNode().getMethod();
		if(!retMethod.isSynthetic() && s
			!retMethod.getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application))
		{
			if(retMethod.isStatic())
				return IdentityFlowFunction.identity();
			return KillEverything.singleton();
		}
		else
			return IdentityFlowFunction.identity();*/
	}
}