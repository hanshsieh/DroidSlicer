package org.droidslicer.ifds;

import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.SparseIntSet;

public class CondVectorGenFlowFunction implements IUnaryFlowFunction
{
	private final int mCond;
	private final IntSet mGen;
	public CondVectorGenFlowFunction(IntSet gen, int cond)
	{
		mGen = gen;
		mCond = cond;
	}
	@Override
	public IntSet getTargets(int i) 
	{
		return (i == mCond) ? mGen : (mGen.contains(i) ? null : SparseIntSet.singleton(i));
    }
}
