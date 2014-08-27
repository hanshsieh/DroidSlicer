package org.droidslicer.ifds;

import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.util.intset.IntSet;

public class KillAllGenFlowFunction implements IUnaryFlowFunction
{
	private final IntSet mResult;
	public KillAllGenFlowFunction(IntSet result)
	{
		mResult = result;
	}
	@Override
	public IntSet getTargets(int d1)
	{
		return mResult;
	}
}
