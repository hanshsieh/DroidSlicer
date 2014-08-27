package org.droidslicer.graph;

import org.droidslicer.graph.entity.UnitEntity;

public class BehaviorNode
{
	private final BehaviorMethod mMethod;
	private final UnitEntity mUnit;
	public BehaviorNode(BehaviorMethod method, UnitEntity unit)
	{
		if(method == null || unit == null)
			throw new IllegalArgumentException();
		mMethod = method;
		mUnit = unit;
	}
	public BehaviorMethod getMethod()
	{
		return mMethod;
	}
	public UnitEntity getUnit()
	{
		return mUnit;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Method = [");
		builder.append(mMethod);
		builder.append("], unit = [");
		builder.append(mUnit);
		builder.append(']');
		return builder.toString();
	}
	@Override
	public int hashCode()
	{
		return mMethod.hashCode() * 31 + mUnit.hashCode();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof BehaviorNode))
			return false;
		BehaviorNode that = (BehaviorNode)other;
		return mMethod.equals(that.mMethod) && mUnit.equals(that.mUnit);
	}
}
