package org.droidslicer.config;

public enum ProviderPermissionXMLElement
{
	PROVIDER_SPEC("provider-spec"),
	PROVIDER("provider"),
	PATH_PERMISSION("path-permission");
	public final static String A_AUTHORITIES = "authorities";
	public final static String A_PERMISSION = "permission";
	public final static String A_READ_PERMISSION = "readPermission";
	public final static String A_WRITE_PERMISSION = "writePermission";
	public final static String A_PATH = "path";
	public final static String A_PATH_PREFIX = "pathPrefix";
	public final static String A_PATH_PATTERN = "pathPattern";
	
	private final String mTagName;
	private ProviderPermissionXMLElement(String tagName)
	{
		mTagName = tagName;
	}
	public String getTagName()
	{
		return mTagName;
	}
}
