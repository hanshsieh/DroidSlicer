package org.droidslicer.graph.entity.dependence;

import java.util.Map;
import java.util.Set;

import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.ifds.BypassFlowFunctions;
import org.droidslicer.util.TypeId;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.Predicate;

public class EntityDependencyFlowFunctions extends BypassFlowFunctions
{
	private final AndroidAnalysisContext mAnalysisCtx;
	public EntityDependencyFlowFunctions(AndroidAnalysisContext analysisCtx,
			Predicate<Statement> terminators, int zeroFact)
	{
		super(terminators, zeroFact);
		mAnalysisCtx = analysisCtx;
		setRecordBypassedCalls(true);
		
		// Some library method may invoke callback, and return the return value of the callback
		setCutReturnToSynthetic(false);
	}
	@Override
	protected boolean shouldBypassCall(Statement caller, Statement callee)
	{
		if(callee == null)
			return true;
		IClass clazz;
		IMethod target = callee.getNode().getMethod();
		if(target.isNative())
			return true;
		if(target.isSynthetic())
		{
			if(target.getDeclaringClass().getReference().equals(TypeId.ANDROID_ASYNC_TASK.getTypeReference()))
				return false;
			return true;
		}
		clazz = target.getDeclaringClass();
		ClassLoaderReference loaderRef = clazz.getClassLoader().getReference();
		if(loaderRef.equals(mAnalysisCtx.getAnalysisScope().getPrimordialLoader()))
			return true;
		else
			return false;
	}
	@Override
	public IUnaryFlowFunction getCallToReturnFlowFunction(Statement caller,
			Statement ret) 
	{
		if(caller.getKind().equals(Statement.Kind.PARAM_CALLER) && 
			ret.getKind().equals(Statement.Kind.NORMAL_RET_CALLER))
		{
			Map<Statement, Set<Statement>> bypassed = getBypassedCallers();
			assert bypassed != null;
			if(bypassed.containsKey(caller))
				return getFlowFunction(caller);
			else
				return KillEverything.singleton();
		}
		else
			return KillEverything.singleton();
	}
	@Override
	public IUnaryFlowFunction getCallNoneToReturnFlowFunction(Statement caller,
			Statement ret) 
	{
		if(caller.getKind().equals(Statement.Kind.PARAM_CALLER) && 
			ret.getKind().equals(Statement.Kind.NORMAL_RET_CALLER))
		{
			return getFlowFunction(caller);
		}
		else
			return KillEverything.singleton();
	}
}
