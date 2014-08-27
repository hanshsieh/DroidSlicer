package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.Collections;

public abstract class SUseUnit extends UnitEntity 
{
	public Collection<String> getPermissions()
	{
		return Collections.emptySet();
	}
	public final boolean equals(Object other)
	{
		return this == other;
	}
	public final int hashCode()
	{
		return super.hashCode();
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitSUseUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}

