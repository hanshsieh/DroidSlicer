package org.droidslicer.signature;

import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.SQLiteDbUnit;
import org.droidslicer.graph.entity.UnitEntity;

public class DatabaseUnitSignature extends UnitSignature 
{
	public DatabaseUnitSignature(Class<? extends ComponentUnit> comp, Boolean isSystem)
	{
		super(comp, isSystem);
	}
	@Override
	public MatchType isBasicMatched(UnitEntity unit)
	{
		if(!(unit instanceof SQLiteDbUnit))
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
		return other instanceof DatabaseUnitSignature;
	}
	@Override
	public int hashCode()
	{
		return super.hashCode() * 52999;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("comp=[");
		builder.append(getComponentType());
		builder.append("], isSystem=");
		builder.append(isSystemComponent());
		builder.append(", isDb=\"true\"");
		return builder.toString();
	}
}
