package org.droidslicer.value;

import java.util.Iterator;

public class SharedPreferencesValue extends ConcreteValue
{
	private final ConcreteValue mNameVal;
	public SharedPreferencesValue(ConcreteValue name)
	{
		if(name == null)
			throw new IllegalArgumentException();
		mNameVal = name;
	}
	public ConcreteValue getNameValue()
	{
		return mNameVal;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Maybe we should do better
		return this;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof SharedPreferencesValue))
			return false;
		SharedPreferencesValue that = (SharedPreferencesValue)other;
		return mNameVal.equals(that.mNameVal);
	}
	@Override
	public int hashCode()
	{
		return mNameVal.hashCode() * 81199;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[SHARED_PREF name=");
		builder.append(mNameVal);
		builder.append(']');
		return builder.toString();
	}
	public static boolean isPossibleMatch(ConcreteValue val1, ConcreteValue val2)
	{
		Iterator<ConcreteValue> itr1 = OrValue.getSingleValueIterator(val1);
		while(itr1.hasNext())
		{
			ConcreteValue val1Single = itr1.next();
			if(!(val1Single instanceof SharedPreferencesValue))
				return true;
			Iterator<ConcreteValue> itr2 = OrValue.getSingleValueIterator(val2);
			while(itr2.hasNext())
			{
				ConcreteValue val2Single = itr2.next();
				if(!(val2Single instanceof SharedPreferencesValue))
					return true;
				SharedPreferencesValue dbVal1 = (SharedPreferencesValue)val1Single;
				SharedPreferencesValue dbVal2 = (SharedPreferencesValue)val2Single;
				ConcreteValue nameVal1 = dbVal1.mNameVal;
				ConcreteValue nameVal2 = dbVal2.mNameVal;
				if(ConstantStringValue.isPossibleMatched(nameVal1, nameVal2))
					return true;
			}
		}
		return false;
	}
}
