package org.droidslicer.signature;

import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.UnitEntity;
import org.droidslicer.graph.entity.UrlConnectionUnit;

public class UrlConnUnitSignature extends UnitSignature
{
	public UrlConnUnitSignature(Class<? extends ComponentUnit> comp, Boolean isSystem)
	{
		super(comp, isSystem);
	}
	@Override
	public MatchType isBasicMatched(UnitEntity unit)
	{
		if(!(unit instanceof UrlConnectionUnit))
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
		return other instanceof UrlConnUnitSignature;
	}
	@Override
	public int hashCode()
	{
		return super.hashCode() * 54617;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("comp=[");
		builder.append(getComponentType());
		builder.append("], isSystem=");
		builder.append(isSystemComponent());
		builder.append(", isUrlConn=\"true\"");
		return builder.toString();
	}
}
