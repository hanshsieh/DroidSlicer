package org.droidslicer.graph.entity;

import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.SharedPreferencesValue;

public class SharedPreferencesUnit extends SUseUnit
{
	private final ConcreteValue mSharedPrefVal;
	public SharedPreferencesUnit(ConcreteValue sharedPrefVal)
	{
		if(sharedPrefVal == null)
			throw new IllegalArgumentException();
		mSharedPrefVal = sharedPrefVal;
	}
	public ConcreteValue getSharedPreferencesValue()
	{
		return mSharedPrefVal;
	}
	public boolean isPossibleAlias(SharedPreferencesUnit that)
	{
		return SharedPreferencesValue.isPossibleMatch(mSharedPrefVal, that.mSharedPrefVal);
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitSharedPreferencesUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(SharedPreferencesUnit.class.getSimpleName());
		builder.append(" value=");
		builder.append(getSharedPreferencesValue());
		builder.append(']');
		return builder.toString();
	}
}
