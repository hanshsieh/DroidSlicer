package org.droidslicer.graph.entity;

public abstract class Entity
{
	public boolean visit(IEntityVisitor visitor)
	{
		return visitor.visitEntity(this);
	}
}
