package org.droidslicer.value;

public class HttpRequestValue extends ConcreteValue
{
	private final ConcreteValue mMethod;
	private final ConcreteValue mUri;
	public HttpRequestValue(ConcreteValue method, ConcreteValue uri)
	{
		if(method == null || uri == null)
			throw new IllegalArgumentException();
		mMethod = method;
		mUri = uri;
	}
	public ConcreteValue getHttpMethod()
	{
		return mMethod;
	}
	public ConcreteValue getHttpUri()
	{
		return mUri;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof HttpRequestValue))
			return false;
		HttpRequestValue that = (HttpRequestValue)other;
		return mMethod.equals(that.mMethod) && mUri.equals(that.mUri);
	}
	@Override
	public int hashCode()
	{
		return mMethod.hashCode() * 31 + mUri.hashCode();
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[HTTP_REQ method=");
		builder.append(mMethod);
		builder.append(", uri=");
		builder.append(mUri);
		builder.append(']');
		return builder.toString();
	}
	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Maybe we can do better
		return this;
	}

}
