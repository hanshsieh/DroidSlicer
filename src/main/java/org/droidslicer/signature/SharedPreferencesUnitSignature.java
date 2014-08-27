package org.droidslicer.signature;

import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.SharedPreferencesUnit;
import org.droidslicer.graph.entity.UnitEntity;

public class SharedPreferencesUnitSignature extends UnitSignature
{
	public SharedPreferencesUnitSignature(Class<? extends ComponentUnit> comp, Boolean isSystem)
	{
		super(comp, isSystem);
	}
	@Override
	public MatchType isBasicMatched(UnitEntity unit)
	{
		if(!(unit instanceof SharedPreferencesUnit))
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
		return other instanceof SharedPreferencesUnitSignature;
	}
	@Override
	public int hashCode()
	{
		return super.hashCode() * 54583;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("comp=[");
		builder.append(getComponentType());
		builder.append("], isSystem=");
		builder.append(isSystemComponent());
		builder.append(", isSharedPref=\"true\"");
		return builder.toString();
	}
}
