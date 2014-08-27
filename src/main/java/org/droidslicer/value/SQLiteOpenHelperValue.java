package org.droidslicer.value;

import java.util.Iterator;


public class SQLiteOpenHelperValue extends ConcreteValue
{
	private ConcreteValue mPath;
	public SQLiteOpenHelperValue(ConcreteValue path)
	{
		if(path == null)
			throw new IllegalArgumentException();
		mPath = path;
	}
	
	/**
	 * Get the concrete value of path when the SQLiteOpenHelper is constructed.
	 * If it is NullValue, then it means that it is in-memory database. 
	 * @return the path of the database file
	 */
	public ConcreteValue getPath()
	{
		return mPath;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Maybe we should do better
		return this;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[SQLITE_OPEN_HELPER path=");
		builder.append(mPath);
		builder.append(']');
		return builder.toString();
	}
	public static boolean isPossibleMatch(ConcreteValue val1, ConcreteValue val2)
	{
		Iterator<ConcreteValue> itr1 = OrValue.getSingleValueIterator(val1);
		while(itr1.hasNext())
		{
			ConcreteValue singleVal1 = itr1.next();
			Iterator<ConcreteValue> itr2 = OrValue.getSingleValueIterator(val2);
			while(itr2.hasNext())
			{
				ConcreteValue singleVal2 = itr2.next();
				if(singleVal1 instanceof UnknownValue || singleVal2 instanceof UnknownValue)
					return true;
				if(singleVal1 instanceof SQLiteOpenHelperValue && singleVal2 instanceof SQLiteOpenHelperValue)
				{
					SQLiteOpenHelperValue openHelperVal1 = (SQLiteOpenHelperValue)singleVal1;
					SQLiteOpenHelperValue openHelperVal2 = (SQLiteOpenHelperValue)singleVal2;
					ConcreteValue path1 = openHelperVal1.getPath();
					ConcreteValue path2 = openHelperVal2.getPath();
					if(ConstantStringValue.isPossibleMatched(path1, path2))
						return true;
				}
			}
		}
		return false;
	}
	public static ConcreteValue resolvePath(ConcreteValue val)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof SQLiteOpenHelperValue)
			{
				SQLiteOpenHelperValue helperVal = (SQLiteOpenHelperValue)singleVal;
				result.addValue(helperVal.getPath());
			}
			else
				result.addValue(UnknownValue.getInstance());
		}
		return result.simplify();
	}
}
