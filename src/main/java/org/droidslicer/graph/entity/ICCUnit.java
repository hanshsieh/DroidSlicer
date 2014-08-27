package org.droidslicer.graph.entity;

public abstract class ICCUnit extends UnitEntity
{
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitICCUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
