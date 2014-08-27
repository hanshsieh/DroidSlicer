package org.droidslicer.config;

public enum SemanticSignatureXMLElement
{
	SPEC("spec"),
	DATA_SPEC("data-spec"),
	COMPONENT("component"),
	DATA("data"),
	FLOW_SPEC("flow-spec"),
	FLOW("flow"),
	SIGNATURES("signatures"),
	SIGNATURE("signature"),
	PERMISSION("permission"),
	FILE("file"),
	DATABASE("database"),
	SHARED_PREFERENCES("shared-preferences"),
	SOCKET("socket"),
	URL_CONN("url-conn"),
	ICC_PARAM_CALLER("icc-param-caller"),
	ICC_PARAM_CALLEE("icc-param-callee"),
	ICC_RET_CALLER("icc-ret-caller"),
	ICC_RET_CALLEE("icc-ret-callee");
	public final static String A_TYPE = "type";
	public final static String A_ID = "id";
	public final static String A_PERMISSIONS = "permissions";
	public final static String A_IS_SYSTEM = "isSystem";
	public final static String A_FROM = "from";
	public final static String A_TO = "to";
	public final static String A_DEFINITION = "def";
	public final static String A_ADDR = "addr";
	public final static String A_PORT = "port";
	public final static String A_PATH = "path";
	public final static String V_TRUE = "true";
	public final static String V_FALSE = "false";
	public final static String V_ACTIVITY = "activity";
	public final static String V_RECEIVER = "receiver";
	public final static String V_PROVIDER = "provider";
	public final static String V_SERVICE = "service";
	public final static String V_APPLICATION = "application";
	public final static String V_ANY = "any";
	private final String mTagName;
	private SemanticSignatureXMLElement(String tagName) 
	{
		mTagName = tagName;
	}
	public String getTagName()
	{
		return mTagName;
	}
}
