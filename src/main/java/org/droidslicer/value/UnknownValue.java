package org.droidslicer.value;

import java.util.Iterator;


public class UnknownValue extends ConcreteValue 
{
	private final static UnknownValue mInstance = new UnknownValue();
	public static UnknownValue getInstance()
	{
		return mInstance;
	}
	@Override
	public boolean equals(Object other)
	{
		return other instanceof UnknownValue;
	}
	@Override
	public int hashCode()
	{
		return 16769023;
	}
	@Override
	public String toString()
	{
		return "?";
	}
	@Override
	public ConcreteValue getStringValue()
	{
		return this;
	}
	public static ConcreteValue excludeUnknownValue(ConcreteValue val)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(!(singleVal instanceof UnknownValue))
				result.addValue(singleVal);
		}
		return result.simplify();
	}
	public static boolean isPossiblelUnknown(ConcreteValue val)
	{
		if(val instanceof UnknownValue)
			return true;
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		boolean hasVal = false;
		while(itr.hasNext())
		{
			hasVal = true;
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof UnknownValue)
				return true;
		}
		return hasVal ? false : true;
	}
}
