package org.droidslicer.graph.entity;

public class Call2ReturnRelation extends RelationEntity
{

	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitCall2ReturnRelation(this))
			return true;
		else
			return super.visit(visitor);
	}
}
