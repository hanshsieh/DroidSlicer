package org.droidslicer.graph.entity;

public class IntentCommRelation extends ICCRelation
{
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitIntentCommRelation(this))
			return true;
		else
			return super.visit(visitor);
	}
}
