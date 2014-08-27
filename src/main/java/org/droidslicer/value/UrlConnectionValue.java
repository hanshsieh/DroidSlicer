package org.droidslicer.value;

public class UrlConnectionValue extends ConcreteValue
{
	private final ConcreteValue mUrl;
	public UrlConnectionValue(ConcreteValue url)
	{
		if(url == null)
			throw new IllegalArgumentException();
		mUrl = url;
	}
	public ConcreteValue getUrl()
	{
		return mUrl;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Can we do better?
		return this;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof UrlConnectionValue))
			return false;
		UrlConnectionValue that = (UrlConnectionValue)other;
		return mUrl.equals(that.mUrl);
	}
	@Override
	public int hashCode()
	{
		return mUrl.hashCode() * 433;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[URL_CONN url=");
		builder.append(mUrl);
		builder.append("]");
		return builder.toString();
	}
}
