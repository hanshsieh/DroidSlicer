package org.droidslicer.graph.entity.resolver;

import org.droidslicer.util.ProgressMonitor;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;

public class ActivityIntentOutflowResolver extends InvocationEntityResolver
{
	public ActivityIntentOutflowResolver(MethodReference method, boolean isStatic)
	{
		super(method, isStatic);
	}

	@Override
	public void resolve(UnitsResolverContext ctx,
			SSAInvokeInstruction invokeInst, int instIdx,
			ProgressMonitor monitor) throws CancelException
	{
		try
		{
			monitor.beginTask("Resolving out-flow of activity", 100);
			if(!invokeInst.getDeclaredTarget().getDescriptor().equals(getMethodReference().getDescriptor()) || 
				invokeInst.isStatic() != isStatic())
				return;
			CGNode node = ctx.getCurrentNode();
			int nParam = invokeInst.getNumberOfParameters();
			for(int i = (invokeInst.isStatic() ? 0 : 1); i < nParam; ++i)
			{
				int valNum = invokeInst.getUse(i);
				ParamCaller param = new ParamCaller(node, instIdx, valNum);
				ctx.addActivityOutFlow(param);
			}
		}
		finally
		{
			monitor.done();
		}
	}
}
