package org.droidslicer.graph.entity;

import com.ibm.wala.types.TypeReference;

public abstract class AppComponentUnit extends ComponentUnit
{
	public AppComponentUnit(TypeReference type)
	{
		super(type);
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitAppComponentUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
