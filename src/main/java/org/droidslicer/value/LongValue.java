package org.droidslicer.value;


public class LongValue extends ConcreteValue
{
	private final long mVal;
	public LongValue(long val)
	{
		mVal = val;
	}
	public long getValue()
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
		if(!(other instanceof LongValue))
			return false;
		LongValue that = (LongValue)other;
		return mVal == that.mVal;
	}
	@Override
	public int hashCode()
	{
		return (int)(mVal * 27361);
	}
}