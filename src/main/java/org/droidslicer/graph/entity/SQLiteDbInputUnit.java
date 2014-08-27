package org.droidslicer.graph.entity;

public class SQLiteDbInputUnit extends SQLiteDbUnit
{
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(SQLiteDbInputUnit.class.getSimpleName());
		builder.append(" value=");
		builder.append(getValue());
		builder.append(']');
		return builder.toString();
	}
}
