package org.droidslicer.value;

import java.util.Iterator;

public class SocketValue extends ConcreteValue
{
	private final ConcreteValue mSocketAddrVal;
	private final boolean mIsListen;
	public SocketValue(ConcreteValue socketAddrVal, boolean isListen)
	{
		if(socketAddrVal == null)
			throw new IllegalArgumentException();
		mSocketAddrVal = socketAddrVal;
		mIsListen = isListen;
	}
	public boolean isListen()
	{
		return mIsListen;
	}
	public ConcreteValue getAddressValue()
	{
		return mSocketAddrVal;
	}
	@Override
	public int hashCode()
	{
		return (mSocketAddrVal.hashCode() << 1) | (mIsListen ? 1 : 0);
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof SocketValue))
			return false;
		SocketValue that = (SocketValue)other;
		return mSocketAddrVal.equals(that.mSocketAddrVal) && mIsListen == that.mIsListen;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[SOCKET addr=");
		builder.append(mSocketAddrVal);
		builder.append(", listen=");
		builder.append(mIsListen);
		builder.append("]");
		return builder.toString();
	}
	@Override
	public ConcreteValue getStringValue()
	{
		return mSocketAddrVal.getStringValue();
	}
	protected static boolean isPossibleMatchSingle(ConcreteValue val1, ConcreteValue val2)
	{
		if(val1 instanceof UnknownValue || val2 instanceof UnknownValue)
			return true;
		if(!(val1 instanceof SocketValue) || !(val2 instanceof SocketValue))
			return false;
		SocketValue sockVal1 = (SocketValue)val1;
		SocketValue sockVal2 = (SocketValue)val2;
		return sockVal1.mIsListen == sockVal2.mIsListen && 
				InetSocketAddressValue.isPossibleMatch(sockVal1.mSocketAddrVal, sockVal2.mSocketAddrVal);
	}
	public static boolean isPossibleMatch(ConcreteValue val1, ConcreteValue val2)
	{
		Iterator<ConcreteValue> itr1 = OrValue.getSingleValueIterator(val1);
		while(itr1.hasNext())
		{
			ConcreteValue val1Single = itr1.next();
			Iterator<ConcreteValue> itr2 = OrValue.getSingleValueIterator(val2);
			while(itr2.hasNext())
			{
				ConcreteValue val2Single = itr2.next();
				if(isPossibleMatchSingle(val1Single, val2Single))
					return true;
			}
		}
		return false;
	}
}
