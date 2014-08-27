package org.droidslicer.value;

import java.util.Iterator;

public class InetAddressValue extends ConcreteValue
{
	private final ConcreteValue mAddrVal;
	public InetAddressValue(ConcreteValue addrVal)
	{
		if(addrVal == null)
			throw new IllegalArgumentException();
		mAddrVal = addrVal;
	}
	public ConcreteValue getAddressValue()
	{
		return mAddrVal;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		return mAddrVal.getStringValue();
	}
	@Override
	public int hashCode()
	{
		return mAddrVal.hashCode() * 983;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof InetAddressValue))
			return false;
		InetAddressValue that = (InetAddressValue)other;
		return mAddrVal.equals(that.mAddrVal);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[INET_ADDR addr=");
		builder.append(mAddrVal);
		builder.append("]");
		return builder.toString();
	}
	protected static boolean isPossibleMatchSingle(ConcreteValue val1, ConcreteValue val2)
	{
		if(val1 instanceof UnknownValue || val2 instanceof UnknownValue)
			return true;
		ConcreteValue addrVal1, addrVal2;
		if(val1 instanceof InetAddressValue)
			addrVal1 = ((InetAddressValue)val1).mAddrVal;
		else
			addrVal1 = val1;
		if(val2 instanceof InetAddressValue)
			addrVal2 = ((InetAddressValue)val2).mAddrVal;
		else
			addrVal2 = val2;
		return ConstantStringValue.isPossibleMatched(addrVal1, addrVal2);
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
