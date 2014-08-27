package org.droidslicer.graph.entity;

import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.SQLiteDbValue;

public abstract class SQLiteDbUnit extends SUseUnit
{
	private ConcreteValue mValue;
	public SQLiteDbUnit()
	{
	}
	public boolean isPossibleAlias(SQLiteDbUnit other)
	{
		ConcreteValue oVal = other.mValue;
		if(mValue == null || oVal == null)
			return false;
		if(mValue == other.mValue)
			return true;
		return SQLiteDbValue.isPossibleMatch(mValue, other.mValue);
	}
	public void setValue(ConcreteValue value)
	{
		mValue = value;
	}
	public ConcreteValue getValue()
	{
		return mValue;
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitSQLiteDbUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
