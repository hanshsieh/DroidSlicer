package org.droidslicer.config;

public enum APIPermissionXMLElement
{
	API_SPEC("api-spec"),
	CLASS_LOADER("class-loader"),
	PACKAGE("package"),
	CLASS("class"),
	METHOD("method");
	public final static String A_NAME = "name";
	public final static String A_RETURN = "return";
	public final static String A_SIGNATURE = "signature";
	public final static String A_PARAM = "param";
	public final static String A_PERMISSION = "permission";
	public final static String A_STATIC = "static";
	
	public final static String V_TRUE = "true";
	public final static String V_FALSE = "false";
	public final static String V_TRACK = "track";
	public final static String V_TRACK_LISTENER = "trackListener";
	public final static String V_RESOLVE = "resolve";
	public final static String V_LISTENER = "listener";
	public final static String V_PRIMORDIAL = "Primordial";
	private final String mTagName;
	private APIPermissionXMLElement(String name)
	{
		mTagName = name;
	}
	public String getTagName()
	{
		return mTagName;
	}
}

