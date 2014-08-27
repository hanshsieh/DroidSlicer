package org.droidslicer.graph.entity;

public class SQLiteDbOutputUnit extends SQLiteDbUnit
{
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(SQLiteDbOutputUnit.class.getSimpleName());
		builder.append(" value=");
		builder.append(getValue());
		builder.append(']');
		return builder.toString();
	}
}
