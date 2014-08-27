package org.droidslicer.graph.entity;

import java.util.Iterator;

import org.droidslicer.value.ConcatValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.UnknownValue;

public abstract class FileUnit extends SUseUnit
{
	private final ConcreteValue mPathVal;
	/**
	 * The {@code pathVal} should represent the absolute path of the file.
	 * @param pathVal
	 */
	public FileUnit(ConcreteValue pathVal)
	{
		mPathVal = NullValue.excludeNullValue(pathVal);
	}
	public ConcreteValue getPathValue()
	{
		return mPathVal;
	}
	private static ConcreteValue filterUnknownPath(ConcreteValue path)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(path);
		OrValue result = new OrValue();
		ConcreteValue filterVal = new ConcatValue(new ConstantStringValue("/"), UnknownValue.getInstance());
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(!(singleVal instanceof UnknownValue) && 
				!(singleVal instanceof NullValue) && 
				!singleVal.equals(filterVal))
			{
				result.addValue(singleVal);
			}
		}
		return result.simplify();
	}
	public boolean isPossibleAlias(FileUnit other)
	{
		ConcreteValue oValue = other.getPathValue();
		if(mPathVal == null || oValue == null)
			return false;
		ConcreteValue path1 = filterUnknownPath(mPathVal);
		ConcreteValue path2 = filterUnknownPath(oValue);

		// TODO To reduce false-positive, we use conservative approach
		if(path1 instanceof UnknownValue || path2 instanceof UnknownValue)
			return false;
		return ConstantStringValue.isPossibleMatched(path1, path2);
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitFileUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
