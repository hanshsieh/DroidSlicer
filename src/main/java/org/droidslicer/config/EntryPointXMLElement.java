package org.droidslicer.config;

public enum EntryPointXMLElement
{
	ENTRY_SPEC("entry-spec"),
	ACTIVITY("activity"),
	RECEIVER("receiver"),
	PROVIDER("provider"),
	SERVICE("service"),
	APPLICATION("application"),
	METHOD("method");
	public final static String A_SIGNATURE = "signature";
	public final static String A_PARAM = "param";
	public final static String A_STATIC = "static";
	
	public final static String V_TRUE = "true";
	public final static String V_FALSE = "false";
	public final static String V_TRACK = "track";
	private final String mTagName;
	private EntryPointXMLElement(String name)
	{
		mTagName = name;
	}
	public String getTagName()
	{
		return mTagName;
	}
}
