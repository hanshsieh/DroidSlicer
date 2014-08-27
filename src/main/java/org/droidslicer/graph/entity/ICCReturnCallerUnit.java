package org.droidslicer.graph.entity;

public abstract class ICCReturnCallerUnit extends ICCUnit
{
	@Override
	public boolean equals(Object other)
	{
		return this == other;
	}
	@Override
	public int hashCode()
	{
		return super.hashCode();
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitICCReturnCallerUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
