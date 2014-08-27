package org.droidslicer.value;

/**
 * @see <a href="https://developer.android.com/reference/org/apache/http/HttpHost.html">HttpHost</a>
 *
 */
public class HttpHostValue extends ConcreteValue
{
	private final ConcreteValue mHostName, mPort, mScheme;
	public HttpHostValue(ConcreteValue hostName, ConcreteValue port, ConcreteValue scheme)
	{
		if(hostName == null || port == null || scheme == null)
			throw new IllegalArgumentException();
		mHostName = hostName;
		mPort = port;
		mScheme = scheme;
	}
	public ConcreteValue getHostName()
	{
		return mHostName;
	}
	public ConcreteValue getPort()
	{
		return mPort;
	}
	public ConcreteValue getScheme()
	{
		return mScheme;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Maybe we can do better
		return this;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof HttpHostValue))
			return false;
		HttpHostValue that = (HttpHostValue)other;
		return mHostName.equals(that.mHostName) && mPort.equals(that.mPort) && mScheme.equals(that.mScheme);
	}
	@Override
	public int hashCode()
	{
		return mHostName.hashCode() * 961 + mPort.hashCode() * 31 + mScheme.hashCode();
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[HTTP_HOST host=");
		builder.append(mHostName);
		builder.append(", port=");
		builder.append(mPort);
		builder.append(", scheme=");
		builder.append(mScheme);
		builder.append(']');
		return builder.toString();
	}
}
