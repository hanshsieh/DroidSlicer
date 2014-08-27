package org.droidslicer.value.solver;

import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.types.ClassLoaderReference;

public class InstanceInitFlowFuction implements IPartiallyBalancedFlowFunctions<Statement> 
{
	public InstanceInitFlowFuction()
	{}
	@Override
	public IUnaryFlowFunction getNormalFlowFunction(Statement src,
			Statement dest)
	{
		switch(dest.getKind())
		{
		case PARAM_CALLER:
			return IdentityFlowFunction.identity();
		default:
			return KillEverything.singleton();
		}
	}

	@Override
	public IUnaryFlowFunction getCallFlowFunction(Statement src,
			Statement dest, Statement ret)
	{
		switch(src.getKind())
		{
		case PARAM_CALLER:
			{
				CGNode calleeNode = dest.getNode();
				ClassLoaderReference calleeClassLoaderRef = calleeNode.getMethod().getDeclaringClass().getClassLoader().getReference();
				if(calleeClassLoaderRef.equals(ClassLoaderReference.Primordial))
					return KillEverything.singleton();
				else
					return IdentityFlowFunction.identity();
			}
		default:
			return KillEverything.singleton();
		}
	}

	@Override
	public IFlowFunction getReturnFlowFunction(Statement call, Statement src,
			Statement dest)
	{
		return getUnbalancedReturnFlowFunction(src, dest);
	}

	@Override
	public IUnaryFlowFunction getCallToReturnFlowFunction(Statement src,
			Statement dest)
	{
		return KillEverything.singleton();
	}

	@Override
	public IUnaryFlowFunction getCallNoneToReturnFlowFunction(Statement src,
			Statement dest)
	{
		return KillEverything.singleton();
	}

	@Override
	public IFlowFunction getUnbalancedReturnFlowFunction(Statement src,
			Statement dest)
	{
		return KillEverything.singleton();
	}

}
