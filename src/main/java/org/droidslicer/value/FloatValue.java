package org.droidslicer.value;


public class FloatValue extends ConcreteValue
{
	private final float mVal;
	public FloatValue(float val)
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
		if(!(other instanceof FloatValue))
			return false;
		FloatValue that = (FloatValue)other;
		return mVal == that.mVal;
	}
	@Override
	public int hashCode()
	{
		return Float.floatToIntBits(mVal);
	}
}