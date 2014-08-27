package org.droidslicer.graph.entity;

import org.droidslicer.value.ConcreteValue;

public class FileOutputUnit extends FileUnit
{
	public FileOutputUnit(ConcreteValue pathVal)
	{
		super(pathVal);
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitFileOutputUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(FileOutputUnit.class.getSimpleName());
		builder.append(" val=");
		builder.append(getPathValue());
		builder.append("]");
		return builder.toString();
	}
}
