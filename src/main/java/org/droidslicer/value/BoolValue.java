package org.droidslicer.value;


public class BoolValue extends ConcreteValue
{
	private final boolean mVal;
	public BoolValue(boolean val)
	{
		mVal = val;
	}
	public boolean getValue()
	{
		return mVal;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		return new ConstantStringValue(String.valueOf(mVal));
	}
	@Override
	public String toString()
	{
		return String.valueOf(mVal);
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof BoolValue))
			return false;
		BoolValue that = (BoolValue)other;
		return mVal == that.mVal;
	}
	@Override
	public int hashCode()
	{
		return (mVal ? 1 : 2) * 9749;
	}
}