package org.droidslicer.graph.entity.resolver;

import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.TypeId;

import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public class ActivityIntentInflowResolver extends InvocationEntityResolver
{
	public ActivityIntentInflowResolver(MethodReference method, boolean isStatic)
	{
		super(method, isStatic);
		TypeReference retTypeRef = method.getReturnType();
		if(!retTypeRef.getName().equals(TypeId.ANDROID_INTENT.getTypeReference().getName()))
			throw new IllegalArgumentException("The return type must be " + TypeId.ANDROID_INTENT.getTypeReference().getName());
	}

	@Override
	public void resolve(UnitsResolverContext ctx,
			SSAInvokeInstruction invokeInst, int instIdx,
			ProgressMonitor monitor) throws CancelException
	{
		try
		{
			monitor.beginTask("Resolving intent in-flow of activity", 100);
			if(!invokeInst.getDeclaredTarget().getDescriptor().equals(getMethodReference().getDescriptor()) ||
				invokeInst.isStatic() != isStatic())
				return;
			NormalReturnCaller retCaller = new NormalReturnCaller(ctx.getCurrentNode(), instIdx);
			ctx.addActivityInFlow(retCaller);
		}
		finally
		{
			monitor.done();
		}
	}
	
}
