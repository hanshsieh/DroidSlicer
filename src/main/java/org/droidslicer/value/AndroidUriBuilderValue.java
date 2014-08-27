package org.droidslicer.value;

public class AndroidUriBuilderValue extends ConcreteValue
{
	private final ConcreteValue mUriVal;
	public AndroidUriBuilderValue(ConcreteValue uriVal)
	{
		if(uriVal == null)
			throw new IllegalArgumentException();
		mUriVal = uriVal;
	}
	public ConcreteValue getUriValue()
	{
		return mUriVal;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		return mUriVal.getStringValue();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof AndroidUriBuilderValue))
			return false;
		AndroidUriBuilderValue that = (AndroidUriBuilderValue)other;
		return mUriVal.equals(that.mUriVal);
	}
	@Override
	public int hashCode()
	{
		return mUriVal.hashCode() * 853;
	}
}
