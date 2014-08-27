package org.droidslicer.signature;

import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.ICCParamCalleeUnit;
import org.droidslicer.graph.entity.UnitEntity;

public class ICCParamCalleeUnitSignature extends UnitSignature
{
	public ICCParamCalleeUnitSignature(Class<? extends ComponentUnit> comp,
			Boolean isSystem)
	{
		super(comp, isSystem);
	}
	@Override
	public MatchType isBasicMatched(UnitEntity unit)
	{
		if(!(unit instanceof ICCParamCalleeUnit))
			return MatchType.NOT_MATCHED;
		MatchType matched = super.isBasicMatched(unit);
		switch(matched)
		{
		case NOT_MATCHED:
		case MATCHED:
			return matched;
		default:
			return MatchType.POSSIBLE_MATCHED;
		}
	}
	@Override
	public boolean equals(Object other)
	{
		if(!super.equals(other))
			return false;
		return other instanceof ICCParamCalleeUnitSignature;
	}
	@Override
	public int hashCode()
	{
		return super.hashCode() * 53279;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("comp=[");
		builder.append(getComponentType());
		builder.append("], isSystem=");
		builder.append(isSystemComponent());
		builder.append(", isICCParamCallee=\"true\"");
		return builder.toString();
	}
}
