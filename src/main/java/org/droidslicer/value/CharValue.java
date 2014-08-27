package org.droidslicer.value;


public class CharValue extends ConcreteValue
{
	private final char mVal;
	public CharValue(char val)
	{
		mVal = val;
	}
	public float getValue()
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
		if(!(other instanceof CharValue))
			return false;
		CharValue that = (CharValue)other;
		return mVal == that.mVal;
	}
	@Override
	public int hashCode()
	{
		return mVal * 27919;
	}
}