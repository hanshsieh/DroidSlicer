package org.droidslicer.signature;

import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.ICCReturnCalleeUnit;
import org.droidslicer.graph.entity.UnitEntity;

public class ICCRetCalleeUnitSignature extends UnitSignature
{
	public ICCRetCalleeUnitSignature(Class<? extends ComponentUnit> comp,
			Boolean isSystem)
	{
		super(comp, isSystem);
	}
	@Override
	public MatchType isBasicMatched(UnitEntity unit)
	{
		if(!(unit instanceof ICCReturnCalleeUnit))
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
		return other instanceof ICCRetCalleeUnitSignature;
	}
	@Override
	public int hashCode()
	{
		return super.hashCode() * 54497;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("comp=[");
		builder.append(getComponentType());
		builder.append("], isSystem=");
		builder.append(isSystemComponent());
		builder.append(", isICCRetCallee=\"true\"");
		return builder.toString();
	}
}
