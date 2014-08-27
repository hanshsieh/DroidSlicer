package org.droidslicer.graph.entity;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.types.Selector;

/**
 * It points from a {@link ComponentUnit} to a {@link SUseUnit}, {@link ICCParamCallerUnit}, 
 * {@link ICCReturnCallerUnit}, {@link ICCParamCalleeUnit}, or {@link ICCReturnCalleeUnit}.
 * 
 * @author someone
 *
 */
public class ComponentReachRelation extends RelationEntity
{
	private final Set<Selector> mSelectors = new HashSet<Selector>();
	public ComponentReachRelation()
	{}
	public void addSelector(Selector selector)
	{
		mSelectors.add(selector);
	}
	public Set<Selector> getSelectors()
	{
		return mSelectors;
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitComponentReachRelation(this))
			return true;
		else
			return super.visit(visitor);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(", selectors=[");
		boolean first = true;
		for(Selector sel : mSelectors)
		{
			if(first)
				first = false;
			else
				builder.append(", ");
			builder.append(sel.getName().toString());
		}
		builder.append(']');
		return builder.toString();
	}
}
