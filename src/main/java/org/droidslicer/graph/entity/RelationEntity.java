package org.droidslicer.graph.entity;

public abstract class RelationEntity extends Entity
{
	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitRelation(this))
			return true;
		else
			return super.visit(visitor);		
	}
}
