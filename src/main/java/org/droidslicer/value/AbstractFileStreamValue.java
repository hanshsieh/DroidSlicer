package org.droidslicer.value;

import java.util.Iterator;

public abstract class AbstractFileStreamValue extends ConcreteValue
{
	public abstract ConcreteValue getPath();
	public static ConcreteValue resolvePathValue(ConcreteValue val)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof AbstractFileStreamValue)
				result.addValue(((AbstractFileStreamValue)singleVal).getPath());
			else
				result.addValue(UnknownValue.getInstance());
		}
		return result.simplify();
	}
}
