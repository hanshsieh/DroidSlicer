package org.droidslicer.signature;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.SUseUnit;
import org.droidslicer.graph.entity.UnitEntity;

public class PermUnitSignature extends UnitSignature
{
	private int mHash = 0;
	private final Set<String> mPerms = new HashSet<String>();
	public PermUnitSignature(Class<? extends ComponentUnit> comp, Boolean isSystem)
	{
		super(comp, isSystem);
	}
	public void addPermission(String perm)
	{
		mHash = 0;
		mPerms.add(perm);
	}
	public Collection<String> getPermission()
	{
		return mPerms;
	}
	@Override
	public MatchType isBasicMatched(UnitEntity unit)
	{
		if(!(unit instanceof SUseUnit))
			return MatchType.NOT_MATCHED;
		SUseUnit sUnit = (SUseUnit)unit;
		Collection<String> perms = sUnit.getPermissions();
		if(!perms.containsAll(mPerms))
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
		if(!(other instanceof PermUnitSignature))
			return false;
		PermUnitSignature that = (PermUnitSignature)other;
		return mPerms.equals(that.mPerms);
	}
	@Override
	public int hashCode()
	{		
		if(mHash == 0)
		{
			mHash = mPerms.hashCode();
			if(mHash == 0)
				mHash = 1;
		}
		return super.hashCode() + mHash;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("comp=[");
		builder.append(getComponentType());
		builder.append("], isSystem=");
		builder.append(isSystemComponent());
		builder.append(", perms=[");
		boolean first = true;
		for(String perm : mPerms)
		{
			if(first)
				first = false;
			else
				builder.append(", ");
			builder.append(perm);
		}
		builder.append(']');
		return builder.toString();
	}
}
