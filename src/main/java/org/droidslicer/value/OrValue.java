package org.droidslicer.value;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Iterators;

public class OrValue extends ConcreteValue 
{
	private Set<ConcreteValue> mValues = new HashSet<ConcreteValue>(2);
	public OrValue(ConcreteValue... values)
	{
		for(ConcreteValue val : values)
			addValue(val);
	}
	public OrValue(ConcreteValue val)
	{
		addValue(val);
	}
	public Iterator<ConcreteValue> iterator()
	{
		return mValues.iterator();
	}
	public boolean contains(ConcreteValue val)
	{
		return mValues.contains(val);
	}
	public boolean remove(ConcreteValue val)
	{
		if(val == null)
			throw new IllegalArgumentException("null");
		return mValues.remove(val);
	}
	@Override
	public ConcreteValue simplify()
	{
		int size = mValues.size();
		switch(size)
		{
		case 0:
			return UnknownValue.getInstance();
		case 1:
			return mValues.iterator().next();
		default:
			return this;
		}			
	}
	public void addValue(ConcreteValue val)
	{
		if(val == null)
			throw new IllegalArgumentException("Value cannot be null");
		if(val instanceof OrValue)
		{
			OrValue that = (OrValue)val;
			for(ConcreteValue ele : that.mValues)
				addValue(ele);
		}
		else
			mValues.add(val);
	}
	public void clear()
	{
		mValues.clear();
	}
	public boolean isEmpty()
	{
		return mValues.isEmpty();
	}
	public int size()
	{
		return mValues.size();
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder("[");
		boolean first = true;
		for(ConcreteValue value : mValues)
		{
			if(first)
				first = false;
			else
				builder.append(" OR ");
			builder.append(value.toString());
		}
		builder.append(']');
		return builder.toString();
	}
	@Override
	public ConcreteValue getStringValue()
	{
		switch(mValues.size())
		{
		case 0:
			return UnknownValue.getInstance();
		case 1:
			return mValues.iterator().next().getStringValue();
		default:
			{
				OrValue result = new OrValue();
				for(ConcreteValue val : mValues)
				{
					result.addValue(val);
				}
				return result;				
			}
		}
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof OrValue))
			return false;
		OrValue that = (OrValue)other;
		return mValues.equals(that.mValues);
	}
	@Override
	public int hashCode()
	{
		return mValues.hashCode();
	}
	public static Iterator<ConcreteValue> getSingleValueIterator(ConcreteValue val)
	{
		if(val instanceof OrValue)
		{
			OrValue orVal = (OrValue)val;
			if(orVal.isEmpty())
				return Iterators.<ConcreteValue>singletonIterator(UnknownValue.getInstance());
			else
				return ((OrValue)val).iterator();
		}
		else
			return Iterators.singletonIterator(val);
	}
}
