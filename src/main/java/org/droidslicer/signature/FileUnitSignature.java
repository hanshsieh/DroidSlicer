package org.droidslicer.signature;

import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.FileUnit;
import org.droidslicer.graph.entity.UnitEntity;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.FileValue;

public class FileUnitSignature extends UnitSignature
{
	private final ConcreteValue mPath;
	private int mHash = -1;
	public FileUnitSignature(Class<? extends ComponentUnit> comp, Boolean isSystem, ConcreteValue path)
	{
		super(comp, isSystem);
		if(path == null)
			throw new IllegalArgumentException();
		mPath = path;
	}
	public ConcreteValue getFilePath()
	{
		return mPath;
	}
	@Override
	public MatchType isBasicMatched(UnitEntity unit)
	{
		if(!(unit instanceof FileUnit))
			return MatchType.NOT_MATCHED;
		MatchType matched = super.isBasicMatched(unit);
		FileUnit fileUnit = (FileUnit)unit;
		switch(matched)
		{
		case NOT_MATCHED:
			return MatchType.NOT_MATCHED;
		case MATCHED:
			{
				if(FileValue.isPossibleMatched(fileUnit.getPathValue(), new FileValue(mPath)))
					return MatchType.MATCHED;
				else
					return MatchType.NOT_MATCHED;
			}
		default:
			{
				if(FileValue.isPossibleMatched(fileUnit.getPathValue(), new FileValue(mPath)))
					return MatchType.POSSIBLE_MATCHED;
				else
					return MatchType.NOT_MATCHED;
			}
		}
	}
	@Override
	public boolean equals(Object other)
	{
		if(!super.equals(other))
			return false;
		if(!(other instanceof FileUnitSignature))
			return false;
		FileUnitSignature that = (FileUnitSignature)other;
		return mPath.equals(that.mPath);
	}
	@Override
	public int hashCode()
	{
		if(mHash == -1)
		{
			mHash = mPath.hashCode();
			if(mHash == -1)
				mHash = 8221;
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
		builder.append(", isFile=\"true\"");
		return builder.toString();
	}
}
