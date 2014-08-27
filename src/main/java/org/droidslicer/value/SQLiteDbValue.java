package org.droidslicer.value;

import java.util.Iterator;

public class SQLiteDbValue extends ConcreteValue
{
	// If it is equals to ":memory:" or it is a value from 
	// the static field android.database.sqlite.SQLiteDatabaseConfiguration#MEMORY_DB_PATH
	// , then it is in-memory database
	private final ConcreteValue mPath;
	public SQLiteDbValue(ConcreteValue path)
	{
		if(path == null)
			throw new IllegalArgumentException();
		mPath = path;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof SQLiteDbValue))
			return false;
		SQLiteDbValue that = (SQLiteDbValue)other;
		return mPath.equals(that.mPath);
	}
	@Override
	public int hashCode()
	{
		return mPath.hashCode() * 3187;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Maybe we can do better
		return this;
	}
	public ConcreteValue getPath()
	{
		return mPath;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[SQLITE_DB path=");
		builder.append(mPath.toString());
		builder.append(']');
		return builder.toString();
	}
	public static boolean isPossibleMatch(ConcreteValue val1, ConcreteValue val2)
	{
		if(UnknownValue.isPossiblelUnknown(val1) || UnknownValue.isPossiblelUnknown(val2))
			return true;
		Iterator<ConcreteValue> itr1 = OrValue.getSingleValueIterator(val1);
		while(itr1.hasNext())
		{
			ConcreteValue val1Single = itr1.next();
			if(!(val1Single instanceof SQLiteDbValue))
				return true;
			Iterator<ConcreteValue> itr2 = OrValue.getSingleValueIterator(val2);
			while(itr2.hasNext())
			{
				ConcreteValue val2Single = itr2.next();
				if(!(val2Single instanceof SQLiteDbValue))
					return true;
				SQLiteDbValue dbVal1 = (SQLiteDbValue)val1Single;
				SQLiteDbValue dbVal2 = (SQLiteDbValue)val2Single;
				ConcreteValue pathVal1 = dbVal1.mPath;
				ConcreteValue pathVal2 = dbVal2.mPath;
				ConcreteValue inMemDbPath = new ConstantStringValue(":memory:");
				if(ConstantStringValue.isPossibleMatched(pathVal1, inMemDbPath) &&
					ConstantStringValue.isPossibleMatched(pathVal2, inMemDbPath))
				{
					return true;
				}
				if(ConstantStringValue.isPossibleMatched(pathVal1, pathVal2))
					return true;
			}
		}
		return false;
	}
}
