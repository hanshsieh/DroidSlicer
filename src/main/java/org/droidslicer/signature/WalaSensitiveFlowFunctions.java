package org.droidslicer.signature;

import org.droidslicer.graph.BehaviorNode;

import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;

public class WalaSensitiveFlowFunctions implements IPartiallyBalancedFlowFunctions<BehaviorNode>
{
	@Override
	public IUnaryFlowFunction getNormalFlowFunction(BehaviorNode src,
			BehaviorNode dest)
	{
		return IdentityFlowFunction.identity();
	}

	@Override
	public IUnaryFlowFunction getCallFlowFunction(BehaviorNode src,
			BehaviorNode dest, BehaviorNode ret)
	{
		return IdentityFlowFunction.identity();
	}

	@Override
	public IFlowFunction getReturnFlowFunction(BehaviorNode call,
			BehaviorNode src, BehaviorNode dest)
	{
		return IdentityFlowFunction.identity();
	}

	@Override
	public IUnaryFlowFunction getCallToReturnFlowFunction(BehaviorNode src,
			BehaviorNode dest)
	{
		return KillEverything.singleton();
	}

	@Override
	public IUnaryFlowFunction getCallNoneToReturnFlowFunction(BehaviorNode src,
			BehaviorNode dest)
	{
		return KillEverything.singleton();
	}

	@Override
	public IFlowFunction getUnbalancedReturnFlowFunction(BehaviorNode src,
			BehaviorNode dest)
	{
		return IdentityFlowFunction.identity();
	}

}
