package org.droidslicer.android.manifest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.droidslicer.util.TypeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.AXmlResourceParser;
import android.util.TypedValue;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.StringStuff;

public class AndroidManifestParser
{
	private final static Logger mLogger = LoggerFactory.getLogger(AndroidManifestParser.class);
	protected final static String ANDROID_NS_URI = "http://schemas.android.com/apk/res/android";
	protected final static String A_NAME = "name";
	protected final static String A_MAX_SDK_VERSION = "maxSdkVersion";
	protected final static String A_MIN_SDK_VERSION = "minSdkVersion";
	protected final static String A_TARGET_SDK_VERSION = "targetSdkVersion";
	protected final static String A_PACKAGE = "package";
	protected final static String A_VERSION_NAME = "versionName";
	protected final static String A_VERSION_CODE = "versionCode";
	protected final static String A_SCHEME = "scheme";
	protected final static String A_HOST = "host";
	protected final static String A_PORT = "port";
	protected final static String A_PATH = "path";
	protected final static String A_PATH_PATTERN = "pathPattern";
	protected final static String A_PATH_PREFIX = "pathPrefix";
	protected final static String A_MIME_TYPE = "mimeType";
	protected final static String A_AUTHORITIES = "authorities";
	public static enum Element
	{
		ACTION("action"),
		ACTIVITY("activity"),
		ACTIVITY_ALIAS("activity-alias"),
		APPLICATION("application"),
		CATEGORY("category"),
		DATA("data"),
		GRANT_URI_PERMISSION("grant-uri-permission"),
		INSTRUMENTATION("instrumentation"),
		INTENT_FILTER("intent-filter"),
		MANIFEST("manifest"),
		META_DATA("meta-data"),
		PERMISSION("permission"),
		PERMISSION_GROUP("permission-group"),
		PERMISSION_TREE("permission-tree"),
		PROVIDER("provider"),
		RECEIVER("receiver"),
		SERVICE("service"),
		SUPPORTS_SCREENS("supports-screens"),
		USES_CONFIGURATION("uses-configuration"),
		USES_FEATURE("uses-feature"),
		USES_LIBRARY("uses-library"),
		USES_PERMISSION("uses-permission"),
		USES_SDK("uses-sdk");
		private final String mTagName;
		private Element(String name)
		{
			mTagName = name;
		}
		public String getElementName()
		{
			return mTagName;
		}
	}
	protected final Map<String, Element> mTagsMap;
	protected final AndroidManifest mManifest = new AndroidManifest();
	protected final AXmlResourceParser mAXmlParser;
	
	// The following variables are for maintaining the current state during parsing
	protected AndroidAppComponent mAppComp = null;
	protected AndroidIntentFilter mIntentFilter = null;
	protected int mIgnoreDepth = -1;
	protected AndroidManifestParser(AXmlResourceParser parser)
	{
		mTagsMap = buildElementsMap();
		mAXmlParser = parser;
	}
	public AndroidManifest getManifest()
	{
		return mManifest;
	}
	protected void onStartTag()
		throws AndroidManifestSyntaxException
	{
		String tagName = mAXmlParser.getName();
		Element tag = mTagsMap.get(tagName);
		if(tag != null && mIgnoreDepth < 0)
		{
			switch(tag)
			{
			case MANIFEST:
				processEleManifest();
				break;
			case APPLICATION:
				processEleApplication();
				break;
			case USES_SDK:
				processEleUsesSDK();
				break;
			case USES_PERMISSION:
				processEleUsesPerm();
				break;
			case ACTIVITY:
				try
				{
					processEleActivity();
				}
				catch(AndroidManifestSyntaxException ex)
				{
					mLogger.warn(getMessagePrefix() + "Fail to parse a activity: ", ex);
				}
				break;
			case RECEIVER:
				try
				{
					processEleReceiver();
				}
				catch(AndroidManifestSyntaxException ex)
				{
					mLogger.warn(getMessagePrefix() + "Fail to parse a receiver: ", ex);
				}
				break;
			case SERVICE:
				try
				{
					processEleService();
				}
				catch(AndroidManifestSyntaxException ex)
				{
					mLogger.warn(getMessagePrefix() + "Fail to parse a service: ", ex);
				}
				break;
			case PROVIDER:
				try
				{
					processEleProvider();
				}
				catch(AndroidManifestSyntaxException ex)
				{
					mLogger.warn(getMessagePrefix() + "Fail to parse a provider: ", ex);
				}
				break;
			case INTENT_FILTER:
				processEleIntentFilter();
				break;
			case ACTION:
				processEleAction();
				break;
			case CATEGORY:
				processEleCategory();
				break;
			case DATA:
				processEleData();
				break;
			default:
				if(mIgnoreDepth < 0)
					mIgnoreDepth = mAXmlParser.getDepth();
				break;
			}
		}
	}
	protected void onEndTag()
			throws AndroidManifestSyntaxException
	{
		String tagName = mAXmlParser.getName();
		Element tag = mTagsMap.get(tagName);
		if(tag != null && mIgnoreDepth < 0)
		{
			switch(tag)
			{
			case ACTIVITY:
			case RECEIVER:
			case SERVICE:
			case PROVIDER:
				mAppComp = null;
				break;
			case INTENT_FILTER:
				if(mAppComp != null && (mAppComp instanceof AndroidComponentWithIntentFilter) && mIntentFilter != null)
				{
					((AndroidComponentWithIntentFilter)mAppComp).addIntentFilter(mIntentFilter);
				}
				else
					mLogger.warn("An <" + Element.INTENT_FILTER.getElementName() + "> is found in an unexpected position");
				mIntentFilter = null;
				break;
			default:
				break;
			}
		}
		if(mIgnoreDepth >= 0 && mAXmlParser.getDepth() <= mIgnoreDepth)
			mIgnoreDepth = -1;
	}
	protected String getMessagePrefix()
	{
		return "Line " + mAXmlParser.getLineNumber() + ": ";
	}
	protected int getAttrIntValue(int attrIdx)
		throws AndroidManifestSyntaxException
	{
		int valueType = mAXmlParser.getAttributeValueType(attrIdx);
		if(valueType >= TypedValue.TYPE_FIRST_INT && valueType <= TypedValue.TYPE_LAST_INT)
			return mAXmlParser.getAttributeIntValue(attrIdx, -1);
		else
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "Expecting attribute " + mAXmlParser.getAttributeName(attrIdx) + " to be an integer");
	}
	protected String getAttrStringValue(int attrIdx)
		throws AndroidManifestSyntaxException
	{
		int valueType = mAXmlParser.getAttributeValueType(attrIdx);
		switch(valueType)
		{
		case TypedValue.TYPE_STRING:
			return mAXmlParser.getAttributeValue(attrIdx);
		case TypedValue.TYPE_REFERENCE:
			// TODO Support reference type
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "Attribute \"" + mAXmlParser.getAttributeName(attrIdx) + "\" is a reference type, but currently we don't support it");
		default:
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "Expecting attribute \"" + mAXmlParser.getAttributeName(attrIdx) + "\" to be a string");
		}
	}
	protected void processEleApplication()
			throws AndroidManifestSyntaxException
	{
		if(mManifest.getApplication() != null)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "Multiple <application> element");
		int nAttr = mAXmlParser.getAttributeCount();
		TypeReference typeRef = null;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mAXmlParser.getAttributeName(i);
			String ns = mAXmlParser.getAttributeNamespace(i);
			if(ANDROID_NS_URI.equals(ns))
			{
				if(A_NAME.equals(attrName))
				{
					String name = getComponentClassName(getAttrStringValue(i).trim());
					typeRef = TypeReference.findOrCreate(ClassLoaderReference.Application, StringStuff.deployment2CanonicalTypeString(name));
				}
			}
		}
		if(typeRef != null)
		{
			// The definition of custom Application implementation is optional
			mManifest.setApplication(new AndroidApplication(typeRef));
		}
		else
		{
			// Use default application
			mManifest.setApplication(new AndroidApplication(TypeId.ANDROID_APPLICATION.getTypeReference()));
		}
	}
	protected void processEleUsesPerm()
			throws AndroidManifestSyntaxException
	{
		int nAttr = mAXmlParser.getAttributeCount();
		String name = null;
		int maxSdk = -1;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mAXmlParser.getAttributeName(i);
			if(ANDROID_NS_URI.equals(mAXmlParser.getAttributeNamespace(i)))
			{
				if(A_NAME.equals(attrName))
					name = getAttrStringValue(i);
				else if(A_MAX_SDK_VERSION.equals(attrName))
				{
					try
					{
						maxSdk = Integer.parseInt(getAttrStringValue(i));
					}
					catch(NumberFormatException ex)
					{
						throw new AndroidManifestSyntaxException(getMessagePrefix() + "Expecting integer for attribute " + A_MAX_SDK_VERSION);
					}
				}
			}
		}
		if(name == null)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "Missing attribute \"" + A_NAME + "\"");
		AndroidManifest.Permission perm = new AndroidManifest.Permission(name, maxSdk);
		mManifest.addPermission(perm);
	}
	protected String getComponentClassName(String name)
		throws AndroidManifestSyntaxException
	{
		if(name.length() == 0)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "Invalid value for \"name\" attribute of <provider> element");
		if(name.charAt(0) == '.')
		{
			if(mManifest.getPackageName() == null)
				throw new AndroidManifestSyntaxException(getMessagePrefix() + "Illegal position of <" + Element.PROVIDER.getElementName() + "> element");
			name = mManifest.getPackageName() + name;
		}
		else if(!name.contains("."))
		{
			if(mManifest.getPackageName() == null)
				throw new AndroidManifestSyntaxException(getMessagePrefix() + "Illegal position of <" + Element.PROVIDER.getElementName() + "> element");
			name = mManifest.getPackageName() + "." + name;
		}
		return name;
	}
	protected void processEleProvider()
			throws AndroidManifestSyntaxException
	{
		int nAttr = mAXmlParser.getAttributeCount();
		TypeReference typeRef = null;
		HashSet<String> auths = new HashSet<String>();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mAXmlParser.getAttributeName(i);
			if(ANDROID_NS_URI.equals(mAXmlParser.getAttributeNamespace(i)))
			{
				if(A_NAME.equals(attrName))
				{
					String name = getComponentClassName(getAttrStringValue(i).trim());
					typeRef = TypeReference.findOrCreate(ClassLoaderReference.Application, StringStuff.deployment2CanonicalTypeString(name));
				}
				else if(A_AUTHORITIES.equals(attrName))
				{
					String[] toks = getAttrStringValue(i).split(";");
					for(String tok : toks)
					{
						tok = tok.trim();
						if(tok.isEmpty())
							continue;
						auths.add(tok);
					}
				}
			}
		}
		if(typeRef == null)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "No class name specified for <provider>");
		
		AndroidProvider provider = new AndroidProvider(typeRef);
		if(auths.isEmpty())
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "provider must have at least one authority");
		for(String auth : auths)
			provider.addAuthority(auth);
		mAppComp = provider;
		mManifest.addProvider(provider);
	}
	protected void processEleService()
			throws AndroidManifestSyntaxException
	{
		int nAttr = mAXmlParser.getAttributeCount();
		TypeReference typeRef = null;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mAXmlParser.getAttributeName(i);
			String ns = mAXmlParser.getAttributeNamespace(i);
			if(ANDROID_NS_URI.equals(ns))
			{
				if(A_NAME.equals(attrName))
				{
					String name = getComponentClassName(getAttrStringValue(i).trim());
					typeRef = TypeReference.findOrCreate(ClassLoaderReference.Application, StringStuff.deployment2CanonicalTypeString(name));
				}
			}
		}
		if(typeRef == null)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "No class name specified for <service>");
		AndroidService service = new AndroidService(typeRef);
		mAppComp = service;
		mManifest.addService(service);		
	}
	protected void processEleReceiver()
			throws AndroidManifestSyntaxException
	{
		int nAttr = mAXmlParser.getAttributeCount();
		TypeReference typeRef = null;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mAXmlParser.getAttributeName(i);
			if(ANDROID_NS_URI.equals(mAXmlParser.getAttributeNamespace(i)))
			{
				if(A_NAME.equals(attrName))
				{
					String name = getComponentClassName(getAttrStringValue(i).trim());
					typeRef = TypeReference.findOrCreate(ClassLoaderReference.Application, StringStuff.deployment2CanonicalTypeString(name));
				}
			}
		}
		if(typeRef == null)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "No class name specified for <receiver>");
		AndroidReceiver receiver = new AndroidReceiver(typeRef);
		mAppComp = receiver;
		mManifest.addReceiver(receiver);
	}
	protected void processEleActivity()
		throws AndroidManifestSyntaxException
	{
		assert mManifest.getPackageName() != null;
		int nAttr = mAXmlParser.getAttributeCount();
		TypeReference typeRef = null;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mAXmlParser.getAttributeName(i);
			if(ANDROID_NS_URI.equals(mAXmlParser.getAttributeNamespace(i)))
			{
				if(A_NAME.equals(attrName))
				{
					String name = getComponentClassName(getAttrStringValue(i).trim());
					typeRef = TypeReference.findOrCreate(ClassLoaderReference.Application, StringStuff.deployment2CanonicalTypeString(name));
				}
			}
		}
		if(typeRef == null)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "No class name specified for <activity>");
		AndroidActivity activity = new AndroidActivity(typeRef);
		mAppComp = activity;
		mManifest.addActivity(activity);
	}
	protected void processEleUsesSDK()
		throws AndroidManifestSyntaxException
	{
		int nAttr = mAXmlParser.getAttributeCount();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mAXmlParser.getAttributeName(i);
			String attrNs = mAXmlParser.getAttributeNamespace(i);
			if(ANDROID_NS_URI.equals(attrNs))
			{
				if(A_MAX_SDK_VERSION.equals(attrName))
					mManifest.setMaxSDKVersion(getAttrIntValue(i));
				else if(A_MIN_SDK_VERSION.equals(attrName))
					mManifest.setMinSDKVersion(getAttrIntValue(i));
				else if(A_TARGET_SDK_VERSION.equals(attrName))
					mManifest.setTargetSDKVersion(getAttrIntValue(i));
			}
		}
	}
	protected void processEleManifest()
		throws AndroidManifestSyntaxException
	{
		if(mManifest.getPackageName() != null)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "Multiple <manifest> tags");
		int nAttr = mAXmlParser.getAttributeCount();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mAXmlParser.getAttributeName(i);
			String attrNs = mAXmlParser.getAttributeNamespace(i);
			if(attrNs.length() == 0)
			{
				if(A_PACKAGE.equals(attrName))
					mManifest.setPackageName(getAttrStringValue(i));
			}
			else if(ANDROID_NS_URI.equals(attrNs))
			{
				if(A_VERSION_NAME.equals(attrName))
				{
					if(TypedValue.TYPE_REFERENCE != mAXmlParser.getAttributeValueType(i))
						mManifest.setVersionName(getAttrStringValue(i));
					else
						mLogger.warn("Reference type is used in version name, but it isn't supported currently");
				}
				else if(A_VERSION_CODE.equals(attrName))
				{
					mManifest.setVersionCode(getAttrIntValue(i));
				}
			}
		}
		if(mManifest.getPackageName() == null)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "Missing \"package\" attribute in <" + Element.MANIFEST.getElementName() + ">");
		if(mManifest.getVersionCode() < 0)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "Missing " + A_VERSION_CODE + " attribute in <" + Element.MANIFEST.getElementName() + ">");
		// version name isn't a necessary attribute
	}
	protected void processEleIntentFilter()
		throws AndroidManifestSyntaxException
	{
		if(mIntentFilter != null)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "Illegal nested <" + Element.INTENT_FILTER.getElementName() + ">");
		if(mAppComp == null || !(mAppComp instanceof AndroidComponentWithIntentFilter))
		{
			mLogger.warn(getMessagePrefix() + "Tag <" + Element.INTENT_FILTER.getElementName() + "> should be under an app component element which accepts IntentFilter. Ignore it");
			return;
		}
		mIntentFilter = new AndroidIntentFilter();
	}
	protected void processEleAction()
		throws AndroidManifestSyntaxException
	{
		if(mAppComp == null || mIntentFilter == null)
		{
			mLogger.warn(getMessagePrefix() + "Illegal position of element <" + Element.ACTION.getElementName() + ">. Ignore it.");
			return;
		}
		int nAttr = mAXmlParser.getAttributeCount();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mAXmlParser.getAttributeName(i);
			String attrNs = mAXmlParser.getAttributeNamespace(i);
			if(ANDROID_NS_URI.equals(attrNs))
			{
				if(A_NAME.equals(attrName))
					mIntentFilter.addActionName(getAttrStringValue(i));
			}
		}
	}
	protected void processEleCategory()
		throws AndroidManifestSyntaxException
	{
		if(mAppComp == null || mIntentFilter == null)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "Illegal position of element <" + Element.CATEGORY.getElementName() + ">");
		int nAttr = mAXmlParser.getAttributeCount();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mAXmlParser.getAttributeName(i);
			String attrNs = mAXmlParser.getAttributeNamespace(i);
			if(ANDROID_NS_URI.equals(attrNs))
			{
				if(A_NAME.equals(attrName))
					mIntentFilter.addCategoryName(getAttrStringValue(i));
			}
		}
	}
	protected void processEleData()
		throws AndroidManifestSyntaxException
	{
		if(mAppComp == null || mIntentFilter == null)
			throw new AndroidManifestSyntaxException(getMessagePrefix() + "Illegal position of element <" + Element.CATEGORY.getElementName() + ">");
		int nAttr = mAXmlParser.getAttributeCount();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mAXmlParser.getAttributeName(i);
			String attrNs = mAXmlParser.getAttributeNamespace(i);
			if(ANDROID_NS_URI.equals(attrNs))
			{
				if(A_SCHEME.equals(attrName))
					mIntentFilter.addDataScheme(getAttrStringValue(i));
				else if(A_HOST.equals(attrName))
					mIntentFilter.addDataHost(getAttrStringValue(i));
				else if(A_PORT.equals(attrName))
				{
					int port;
					try
					{
						// TODO I haven't checked whether this format is readlly allowed
						// in manifest
						String val = getAttrStringValue(i);
						if(val.startsWith("0x"))
							port = Integer.parseInt(val.substring(2), 16);
						else
							port = Integer.parseInt(val);
					}
					catch(NumberFormatException ex)
					{
						throw new AndroidManifestSyntaxException(getMessagePrefix() + "Illegal format for \"" + A_PORT + "\" attribute");
					}
					mIntentFilter.addDataPort(port);
				}
				else if(A_PATH.equals(attrName))
				{
					mIntentFilter.addDataPath(getAttrStringValue(i));
				}
				else if(A_PATH_PATTERN.equals(attrName))
				{
					mIntentFilter.addDataPathPattern(getAttrStringValue(i));
					// TODO Do we need to do unescape? e.g. "//" --> "/"
					// Or, the XML parser library already do it for us
				}
				else if(A_PATH_PREFIX.equals(attrName))
				{
					mIntentFilter.addDataPathPrefix(getAttrStringValue(i));
				}
				else if(A_MIME_TYPE.equals(attrName))
				{
					mIntentFilter.addDataMimeType(getAttrStringValue(i));
				}
			}
		}
	}
	protected static HashMap<String, Element> buildElementsMap()
	{
		HashMap<String, Element> eleMap = new HashMap<String, Element>();
		Element[] eles = Element.values();
		for(Element ele : eles)
			eleMap.put(ele.getElementName(), ele);
		return eleMap;
	}
	public static AndroidManifest parseFromCompressed(InputStream input)
			throws IOException, AndroidManifestSyntaxException
	{
		return parseFromCompressed(input, true);
	}
	/**
	 * Notice that it won't close the input stream.
	 * @param input
	 * @return
	 * @throws IOException
	 * @throws AndroidManifestSyntaxException
	 */
	public static AndroidManifest parseFromCompressed(InputStream input, boolean autoClose)
		throws IOException, AndroidManifestSyntaxException
	{
		try
		{
			AXmlResourceParser xpp = new AXmlResourceParser();
			xpp.open(input);
			AndroidManifestParser parser = new AndroidManifestParser(xpp);
			int eventType = -1;
			// Reference https://github.com/tony19/apktool-lib for usage
			while((eventType = xpp.next()) != AXmlResourceParser.END_DOCUMENT)
			{
				switch(eventType)
				{
				case XmlPullParser.START_TAG:
					parser.onStartTag();
					break;
				case XmlPullParser.END_TAG:
					parser.onEndTag();
					break;
				}
			}
			AndroidManifest manifest = parser.getManifest();
			if(manifest.getApplication() == null)
				throw new AndroidManifestSyntaxException("Missing tag <" + Element.APPLICATION.getElementName() + ">");
			return manifest;
		}
		catch(XmlPullParserException ex)
		{
			throw new AndroidManifestSyntaxException(ex);
		}
		finally
		{
			if(autoClose)
				input.close();
		}
	}
}
