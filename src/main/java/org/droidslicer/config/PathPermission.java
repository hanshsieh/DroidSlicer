package org.droidslicer.config;

public class PathPermission
{
	public static enum PathPatternType
	{
		LITERAL,
		SIMPLE_GLOB,
		PREFIX
	}
	private final PathPatternType mPatternType;
	private final String mPathPat;
	private String mReadPerm;
	private String mWritePerm;
	public PathPermission(String pathPat, PathPatternType patternType)
	{
		if(pathPat == null || patternType == null)
			throw new IllegalArgumentException();
		mPathPat = pathPat;
		mPatternType = patternType;
	}
	public String getPathPattern()
	{
		return mPathPat;
	}
	public void setReadPermission(String perm)
	{
		mReadPerm = perm;
	}
	public void setWritePermission(String perm)
	{
		mWritePerm = perm;
	}
	public String getReadPermission()
	{
		return mReadPerm;
	}
	public String getWritePermission()
	{
		return mWritePerm;
	}
	public PathPatternType getPathPatternType()
	{
		return mPatternType;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof PathPermission))
			return false;
		PathPermission that = (PathPermission)other;
		if(!mPatternType.equals(that.mPatternType) ||
			!mPathPat.equals(that.mPathPat))
			return false;
		if(mReadPerm == null)
		{
			if(that.mReadPerm != null)
				return false;
		}
		else if(!mReadPerm.equals(that.mReadPerm))
			return false;
		if(mWritePerm == null)
		{
			if(that.mWritePerm != null)
				return false;
		}
		else if(!mWritePerm.equals(that.mWritePerm))
			return false;
		return true;
	}
	@Override
	public int hashCode()
	{
		int hash = 0;
		hash = mPatternType.hashCode();
		hash = hash * 31 + mPathPat.hashCode();
		hash = hash * 31 + (mReadPerm == null ? 0 : mReadPerm.hashCode());
		hash = hash * 31 + (mWritePerm == null ? 0 : mWritePerm.hashCode());
		return hash;
	}
}
