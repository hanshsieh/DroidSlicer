package org.droidslicer.signature;

import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.ICCReturnCallerUnit;
import org.droidslicer.graph.entity.UnitEntity;

public class ICCRetCallerUnitSignature extends UnitSignature
{
	public ICCRetCallerUnitSignature(Class<? extends ComponentUnit> comp,
			Boolean isSystem)
	{
		super(comp, isSystem);
	}
	@Override
	public MatchType isBasicMatched(UnitEntity unit)
	{
		if(!(unit instanceof ICCReturnCallerUnit))
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
		return other instanceof ICCRetCallerUnitSignature;
	}
	@Override
	public int hashCode()
	{
		return super.hashCode() * 54581;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("comp=[");
		builder.append(getComponentType());
		builder.append("], isSystem=");
		builder.append(isSystemComponent());
		builder.append(", isICCRetCaller=\"true\"");
		return builder.toString();
	}
}
