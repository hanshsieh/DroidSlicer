package org.droidslicer.graph.entity;

import com.ibm.wala.types.TypeReference;

public class ApplicationUnit extends ComponentUnit
{
	public ApplicationUnit(TypeReference type)
	{
		super(type);
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitApplicationUnit(this))
			return true;
		else 
			return super.visit(visitor);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(ApplicationUnit.class.getSimpleName());
		builder.append(" type=[");
		builder.append(getType());
		builder.append("], isSystem=");
		builder.append(isSystemComponent());
		builder.append(']');
		return builder.toString();
	}
}
