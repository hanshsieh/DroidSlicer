package org.droidslicer.value;


public class DoubleValue extends ConcreteValue
{
	private final double mVal;
	public DoubleValue(double val)
	{
		mVal = val;
	}
	public double getValue()
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
		if(!(other instanceof DoubleValue))
			return false;
		DoubleValue that = (DoubleValue)other;
		return mVal == that.mVal;
	}
	@Override
	public int hashCode()
	{
		return (int)Double.doubleToRawLongBits(mVal);
	}
}