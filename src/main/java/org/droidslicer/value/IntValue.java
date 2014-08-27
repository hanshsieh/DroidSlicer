package org.droidslicer.value;

import java.util.Iterator;

public class IntValue extends ConcreteValue
{
	private final int mVal;
	public IntValue(int val)
	{
		mVal = val;
	}
	public int getValue()
	{
		return mVal;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		return new ConstantStringValue(String.valueOf(mVal));
	}
	public static boolean isPossibleNotNegative(ConcreteValue val)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue valSingle = itr.next();
			if(valSingle instanceof IntValue)
			{
				IntValue intVal = (IntValue)valSingle;
				if(intVal.getValue() >= 0)
					return true;
			}
			else if(valSingle instanceof UnknownValue)
				return true;
		}
		return false;
	}
	public static boolean isImpossibleNegative(ConcreteValue val)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue valSingle = itr.next();
			if(valSingle instanceof IntValue)
			{
				IntValue intVal = (IntValue)valSingle;
				if(intVal.getValue() < 0)
					return false;
			}
			else
				return false;
		}
		return true;
	}
	public static boolean isPossibleNegative(ConcreteValue val)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue valSingle = itr.next();
			if(valSingle instanceof IntValue)
			{
				IntValue intVal = (IntValue)valSingle;
				if(intVal.getValue() < 0)
					return true;
			}
			else if(valSingle instanceof UnknownValue)
				return true;
		}
		return false;
	}
	/**
	 * Create a new value, and do its best to exclude negative integer values.
	 * @param val
	 * @return the value excluding negative integer values
	 */
	public static ConcreteValue excludeNegative(ConcreteValue val)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue valSingle = itr.next();
			if(valSingle instanceof IntValue)
			{
				IntValue intVal = (IntValue)valSingle;
				if(intVal.getValue() >= 0)
					result.addValue(intVal);
			}
			else
				result.addValue(valSingle);
		}
		return result.simplify();
	}
	@Override
	public String toString()
	{
		return String.valueOf(mVal);
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof IntValue))
			return false;
		IntValue that = (IntValue)other;
		return mVal == that.mVal;
	}
	@Override
	public int hashCode()
	{
		return mVal * 9497;
	}
	protected static boolean isPossibleMatchSingle(ConcreteValue val1, ConcreteValue val2)
	{
		if(val1 instanceof UnknownValue || val2 instanceof UnknownValue)
			return true;
		if(!(val1 instanceof IntValue) || !(val2 instanceof IntValue))
			return false;
		IntValue intVal1 = (IntValue)val1;
		IntValue intVal2 = (IntValue)val2;
		return intVal1.mVal == intVal2.mVal;
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
