package org.droidslicer.value;

import java.util.Iterator;


public class NullValue extends ConcreteValue
{
	private static final NullValue mInstance = new NullValue();
	public static NullValue getInstance()
	{
		return mInstance;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		return (other instanceof NullValue);
	}
	@Override
	public int hashCode()
	{
		return 27644437;
	}
	@Override
	public String toString()
	{
		return "null";
	}
	@Override
	public ConcreteValue getStringValue()
	{
		// Invoke null.toString() will cause exception
		return UnknownValue.getInstance();
	}
	private static boolean isPossibleNullSingle(ConcreteValue val)
	{
		return val instanceof NullValue || val instanceof UnknownValue;			
	}
	public static ConcreteValue excludeNullValue(ConcreteValue val)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue valSingle = itr.next();
			if(!(valSingle instanceof NullValue))
				result.addValue(valSingle);
		}
		return result.simplify();
	}
	public static boolean isPossibleNull(ConcreteValue val)
	{
		if(val instanceof OrValue)
		{
			Iterator<ConcreteValue> itr = ((OrValue)val).iterator();
			while(itr.hasNext())
			{
				if(isPossibleNullSingle(itr.next()))
					return true;
			}
			return false;
		}
		else
			return isPossibleNullSingle(val);
	}
	public static boolean isPossibleNotNull(ConcreteValue val)
	{
		if(val instanceof OrValue)
			val = ((OrValue)val).simplify();
		if(val instanceof UnknownValue)
			return true;
		if(val instanceof NullValue)
			return false;
		return true;
	}
	public static boolean isImpossibleNull(ConcreteValue val)
	{
		if(val instanceof OrValue)
		{
			OrValue orVal = (OrValue)val;
			if(orVal.contains(NullValue.getInstance()) || orVal.contains(UnknownValue.getInstance()))
				return false;
			else
				return true;
		}
		if(val instanceof NullValue ||
			val instanceof UnknownValue)
			return false;
		else
			return true;
	}
}
