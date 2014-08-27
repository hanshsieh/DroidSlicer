package org.droidslicer.graph.entity;

import org.droidslicer.value.ConcreteValue;

public class FileInputUnit extends FileUnit
{
	public FileInputUnit(ConcreteValue pathVal)
	{
		super(pathVal);
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitFileInputUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(FileInputUnit.class.getSimpleName());
		builder.append(" val=");
		builder.append(getPathValue());
		builder.append("]");
		return builder.toString();
	}
}
