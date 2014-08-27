package org.droidslicer.graph.entity;

public class UriCommRelation extends ICCRelation
{
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitUriCommRelation(this))
			return true;
		else
			return super.visit(visitor);
	}
}
