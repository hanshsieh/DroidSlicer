package org.droidslicer.graph;

import org.droidslicer.graph.entity.ComponentUnit;

import com.ibm.wala.types.Selector;

public class BehaviorMethod
{
	private final ComponentUnit mComp;
	private final Selector mSelector;
	public BehaviorMethod(ComponentUnit comp, Selector selector)
	{
		if(comp == null || selector == null)
			throw new IllegalArgumentException();
		mComp = comp;
		mSelector = selector;
	}
	public ComponentUnit getComponent()
	{
		return mComp;
	}
	public Selector getSelector()
	{
		return mSelector;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("comp=[");
		builder.append(mComp);
		builder.append("], selector=[");
		builder.append(mSelector);
		builder.append(']');
		return builder.toString();
	}
	@Override
	public int hashCode()
	{
		return mComp.hashCode() * 31 + mSelector.hashCode();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof BehaviorMethod))
			return false;
		BehaviorMethod that = (BehaviorMethod)other;
		return mComp.equals(that.mComp) && mSelector.equals(that.mSelector);
	}
}
