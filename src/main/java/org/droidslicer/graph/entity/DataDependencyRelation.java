package org.droidslicer.graph.entity;

import java.util.Collections;
import java.util.Set;

import org.droidslicer.graph.BehaviorMethod;

public class DataDependencyRelation extends RelationEntity
{
	private final Set<BehaviorMethod> mCrossCompConds;
	public DataDependencyRelation()
	{
		mCrossCompConds = Collections.emptySet();
	}
	public DataDependencyRelation(Set<BehaviorMethod> crossCompDepends)
	{
		mCrossCompConds = crossCompDepends;
	}
	public Set<BehaviorMethod> getCrossComponentConditions()
	{
		return mCrossCompConds;
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitDataDependencyRelation(this))
			return true;
		else
			return super.visit(visitor);
	}
}
