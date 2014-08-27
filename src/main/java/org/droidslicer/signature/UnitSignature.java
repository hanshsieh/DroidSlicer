package org.droidslicer.signature;

import org.droidslicer.graph.BehaviorMethod;
import org.droidslicer.graph.BehaviorNode;
import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.ICCUnit;
import org.droidslicer.graph.entity.SUseUnit;
import org.droidslicer.graph.entity.UnitEntity;

public class UnitSignature extends Signature
{
	private final Class<? extends ComponentUnit> mComp;
	private final Boolean mIsSystem;
	
	/**
	 * If {@code comp} is null, no requirement of the component is applied.
	 * @param comp
	 */
	public UnitSignature(Class<? extends ComponentUnit> comp, Boolean isSystem)
	{
		mComp = comp;
		mIsSystem = isSystem;
	}
	public Boolean isSystemComponent()
	{
		return mIsSystem;
	}
	public Class<? extends ComponentUnit> getComponentType()
	{
		return mComp;
	}
	public MatchType isBasicMatched(UnitEntity unit)
	{
		if(unit instanceof ICCUnit || unit instanceof SUseUnit)
		{
			if(mIsSystem == null && mComp == null)
				return MatchType.MATCHED;
			else
				return MatchType.POSSIBLE_MATCHED;
		}
		else
			return MatchType.NOT_MATCHED;
	}
	public boolean isMatched(BehaviorNode node)
	{
		BehaviorMethod method = node.getMethod();
		ComponentUnit comp = method.getComponent();
		if(mIsSystem != null)
		{
			if(!mIsSystem.equals(comp.isSystemComponent()))
				return false;
		}
		if(mComp == null)
			return true;
		return mComp.isAssignableFrom(comp.getClass());
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("comp=[");
		builder.append(mComp);
		builder.append("], isSystem=");
		builder.append(mIsSystem == null ? "any" : mIsSystem);
		return builder.toString();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof UnitSignature))
			return false;
		UnitSignature that = (UnitSignature)other;
		if(mIsSystem == null)
		{
			if(that.mIsSystem != null)
				return false;
		}
		else if(!mIsSystem.equals(that.mIsSystem))
			return false;
		if(mComp == null)
		{
			return that.mComp == null;
		}
		else
			return mComp.equals(that.mComp);
	}
	@Override
	public int hashCode()
	{
		int hash;
		if(mIsSystem == null)
			hash = 29129;
		else if(mIsSystem)
			hash = 16057; 
		else
			hash = 16319;
		return hash + (mComp == null ? -1 : mComp.hashCode());
	}
}
