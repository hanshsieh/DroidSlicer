package org.droidslicer.value;

import java.util.Iterator;


public class InetSocketAddressValue extends ConcreteValue
{
	private final ConcreteValue mAddrVal;
	private final ConcreteValue mPortVal;
	public InetSocketAddressValue(ConcreteValue addrVal, ConcreteValue portVal)
	{
		if(addrVal == null || portVal == null)
			throw new IllegalArgumentException();
		mAddrVal = addrVal;
		mPortVal = portVal;
	}
	public ConcreteValue getAddressValue()
	{
		return mAddrVal;
	}
	public ConcreteValue getPortValue()
	{
		return mPortVal;
	}
	@Override
	public boolean equals(Object other) 
	{
		if(this == other)
			return true;
		if(!(other instanceof InetSocketAddressValue))
			return false;
		InetSocketAddressValue that = (InetSocketAddressValue)other;
		return mAddrVal.equals(that.mAddrVal) && mPortVal.equals(that.mPortVal);
	}
	@Override
	public int hashCode()
	{
		return mAddrVal.hashCode() * 31 + mPortVal.hashCode();
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[INET_SOCKET_ADDR addr=");
		builder.append(mAddrVal);
		builder.append(", port=");
		builder.append(mPortVal);
		builder.append("]");
		return builder.toString();
	}
	@Override
	public ConcreteValue getStringValue()
	{
		return new ConcatValue(mAddrVal.getStringValue(), new ConstantStringValue(":"), mPortVal.getStringValue());
	}
	protected static boolean isPossibleMatchSingle(ConcreteValue val1, ConcreteValue val2)
	{
		if(val1 instanceof UnknownValue || val2 instanceof UnknownValue)
			return true;
		if(!(val1 instanceof InetSocketAddressValue) || !(val2 instanceof InetSocketAddressValue))
			return false;
		InetSocketAddressValue addrVal1 = (InetSocketAddressValue)val1;
		InetSocketAddressValue addrVal2 = (InetSocketAddressValue)val2;
		return IntValue.isPossibleMatch(addrVal1.mPortVal, addrVal2.mPortVal) && 
				InetAddressValue.isPossibleMatch(addrVal1.mAddrVal, addrVal2.mAddrVal);
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
