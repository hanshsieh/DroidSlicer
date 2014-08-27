package org.droidslicer.ifds;

import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.SparseIntSet;
 
public class VectorAddFlowFunction implements IUnaryFlowFunction
{
	private final IntSet mAdd;
	private VectorAddFlowFunction(IntSet add)
	{
		mAdd = add;
	}
	public static VectorAddFlowFunction make(IntSet add)
	{
		// TODO Maybe we should do some caching
		return new VectorAddFlowFunction(add);
	}
	@Override
	public IntSet getTargets(int d1)
	{
		return mAdd.union(SparseIntSet.singleton(d1));
	}
}
