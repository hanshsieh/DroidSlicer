package org.droidslicer.config;

public enum IntentPermissionXMLElement
{
	INTENT_SPEC("intent-spec"),
	INTENT("intent"),
	SENDER("sender"),
	RECEIVER("receiver");
	public final static String A_ACTION = "action";
	public final static String A_PERMISSION = "permission";
	private final String mTagName;
	private IntentPermissionXMLElement(String name)
	{
		mTagName = name;
	}
	public String getTagName()
	{
		return mTagName;
	}
}
