package org.droidslicer.signature;

import heros.FlowFunction;
import heros.FlowFunctions;
import heros.flowfunc.Identity;
import heros.flowfunc.KillAll;

import org.droidslicer.graph.BehaviorMethod;
import org.droidslicer.graph.BehaviorNode;

public class SensitiveFlowFunctions implements FlowFunctions<BehaviorNode, Object, BehaviorMethod>
{
	public SensitiveFlowFunctions(Object zeroValue)
	{
		if(zeroValue == null)
			throw new IllegalArgumentException();
	}
	@Override
	public FlowFunction<Object> getNormalFlowFunction(BehaviorNode curr,
			BehaviorNode succ)
	{
		return Identity.v();
	}

	@Override
	public FlowFunction<Object> getCallFlowFunction(BehaviorNode callStmt,
			BehaviorMethod destinationMethod)
	{
		return Identity.v();
	}

	@Override
	public FlowFunction<Object> getReturnFlowFunction(BehaviorNode callSite,
			final BehaviorMethod calleeMethod, BehaviorNode exitStmt,
			BehaviorNode returnSite)
	{
		return Identity.v();
	}

	@Override
	public FlowFunction<Object> getCallToReturnFlowFunction(
			BehaviorNode callSite, BehaviorNode returnSite)
	{
		return KillAll.v();
	}
}
