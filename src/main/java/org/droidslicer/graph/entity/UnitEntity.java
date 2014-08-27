package org.droidslicer.graph.entity;



public abstract class UnitEntity extends Entity
{
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}