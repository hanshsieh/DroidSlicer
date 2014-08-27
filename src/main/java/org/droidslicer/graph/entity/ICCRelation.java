package org.droidslicer.graph.entity;

/**
 * It points from a {@link ICCParamCallerUnit} to a {@link ComponentUnit}.
 * @author someone
 *
 */
public abstract class ICCRelation extends RelationEntity
{
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitICCRelation(this))
			return true;
		else
			return super.visit(visitor);
	}
}
