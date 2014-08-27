package org.droidslicer.value;


public class StringBuilderValue extends ConcreteValue
{
	private ConcreteValue mVal;
	public StringBuilderValue(ConcreteValue val)
	{
		if(val == null)
			throw new IllegalArgumentException("null");
		mVal = val;
	}
	public ConcreteValue getContent()
	{
		return mVal;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		return mVal.getStringValue();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof StringBuilderValue))
			return false;
		StringBuilderValue that = (StringBuilderValue)other;
		return mVal.equals(that.mVal);
	}
}
