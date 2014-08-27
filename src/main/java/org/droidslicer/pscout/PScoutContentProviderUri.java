package org.droidslicer.pscout;

public class PScoutContentProviderUri
{
	public enum PScoutPathPatternType
	{
		LITERAL,
		PREFIX, 
		SIMPLE_GLOB,
		NONE
	}
	public enum PScoutAccessType
	{
		READ,
		WRITE,
		GRANT_URI_PERMISSION
	}
	private PScoutAccessType mAccType;
	private String mPermission;
	private PScoutPathPatternType mPatternType;
	private String mPathPattern;
	private String mAuth;
	public String getAuthority()
	{
		return mAuth;
	}
	public void setAuthority(String val)
	{
		mAuth = val;
	}
	public String getPathPattern()
	{
		return mPathPattern;
	}
	public void setPathPattern(String uriPattern)
	{
		this.mPathPattern = uriPattern;
	}
	public PScoutAccessType getAccessType()
	{
		return mAccType;
	}
	public void setAccessType(PScoutAccessType accType)
	{
		this.mAccType = accType;
	}
	public String getPermission()
	{
		return mPermission;
	}
	public void setPermission(String permission)
	{
		this.mPermission = permission;
	}
	public PScoutPathPatternType getPathPatternType()
	{
		return mPatternType;
	}
	public void setPathPatternType(PScoutPathPatternType patternType)
	{
		this.mPatternType = patternType;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append(mPathPattern);
		builder.append(' ');
		switch(mAccType)
		{
		case READ:
			builder.append("R");
			builder.append(' ');
			builder.append(mPermission);
			break;
		case WRITE:
			builder.append("W");
			builder.append(' ');
			builder.append(mPermission);
			break;
		default:
			assert mAccType == PScoutAccessType.GRANT_URI_PERMISSION;
			builder.append("grant-uri-permission");
			break;
		}
		if(mPatternType != null)
		{
			builder.append(' ');
			switch(mPatternType)
			{
			case LITERAL:
				builder.append("path");
				break;
			case PREFIX:
				builder.append("pathPrefix");
				break;
			default: //case PATH_PATTERN:
				assert mPatternType == PScoutPathPatternType.SIMPLE_GLOB;
				builder.append("pathPattern");
				break;
			}
		}
		return builder.toString();
	}
}
