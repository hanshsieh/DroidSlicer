package org.droidslicer.graph.entity;

public abstract class ICCParamCallerUnit extends ICCUnit
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
		if(visitor.visitICCParamCallerUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
