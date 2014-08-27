package org.droidslicer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.droidslicer.util.Utils;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.strings.Atom;

public class APIPermissionParser
{
	public static class APIPermission
	{
		private final MethodReference mMethodRef;
		private final boolean mIsStatic;
		private final MutableIntSet mResolveParams = new BitVectorIntSet();
		private final MutableIntSet mTrackParamsAndRet = new BitVectorIntSet();
		private final MutableIntSet mListenerParams = new BitVectorIntSet();
		private final MutableIntSet mTrackListenerParams = new BitVectorIntSet();
		private final Set<String> mPerms = new HashSet<String>();
		public APIPermission(MethodReference methodRef, boolean isStatic)
		{
			mMethodRef = methodRef;
			mIsStatic = isStatic;
		}
		public MethodReference getMethodReference()
		{
			return mMethodRef;
		}
		public boolean isStatic()
		{
			return mIsStatic;
		}
		/**
		 * Number of parameters of the method, including implicit 'this'.
		 * @return
		 */
		public int getNumberOfParameters()
		{
			int nParam = mMethodRef.getNumberOfParameters();
			return mIsStatic ? nParam : nParam + 1; 
		}
		protected void checkParamIndex(int paramIdx)
		{
			int nParam = getNumberOfParameters();
			if(paramIdx < 0 || paramIdx >= nParam)
				throw new IllegalArgumentException();
		}
		public void setParamResolve(int paramIdx, boolean shouldResolve)
		{
			checkParamIndex(paramIdx);
			if(shouldResolve)
				mResolveParams.add(paramIdx);
			else
				mResolveParams.remove(paramIdx);
		}
		public boolean isParamResolve(int paramIdx)
		{
			checkParamIndex(paramIdx);
			return mResolveParams.contains(paramIdx);
		}
		
		/**
		 * Set whether a parameter should be tracked.
		 * The parameter index starts from 0, and it includes the implicit
		 * 'this' if it is an instance method. 
		 * @param paramIdx the parameter index
		 * @param shouldTrack
		 */
		public void setParamTrack(int paramIdx, boolean shouldTrack)
		{
			checkParamIndex(paramIdx);
			if(shouldTrack)
				mTrackParamsAndRet.add(paramIdx);
			else
				mTrackParamsAndRet.remove(paramIdx);
		}
		public boolean isParamTrack(int paramIdx)
		{
			checkParamIndex(paramIdx);
			return mTrackParamsAndRet.contains(paramIdx);
		}
		public void setReturnTrack(boolean shouldTrack)
		{
			int nParam = getNumberOfParameters();
			if(shouldTrack)
				mTrackParamsAndRet.add(nParam);
			else
				mTrackParamsAndRet.remove(nParam);
		}
		public boolean isReturnTrack()
		{
			int nParam = getNumberOfParameters();
			return mTrackParamsAndRet.contains(nParam);
		}
		public void setParamListener(int paramIdx, boolean isListener)
		{
			checkParamIndex(paramIdx);
			if(isListener)
				mListenerParams.add(paramIdx);
			else
				mListenerParams.remove(paramIdx);
		}
		public boolean isParamListener(int paramIdx)
		{
			checkParamIndex(paramIdx);
			return mListenerParams.contains(paramIdx);
		}
		public void setTrackParamListener(int paramIdx, boolean track)
		{
			checkParamIndex(paramIdx);
			if(track)
				mTrackListenerParams.add(paramIdx);
			else
				mTrackListenerParams.remove(paramIdx);
		}
		public boolean isTrackParamListener(int paramIdx)
		{
			checkParamIndex(paramIdx);
			return mTrackListenerParams.contains(paramIdx);
		}
		public void addPermission(String perm)
		{
			mPerms.add(perm);
		}
		public Collection<String> getPermissions()
		{
			return mPerms;
		}
	}
	private final static boolean DEFAULT_METHOD_STATIC = false;
	protected final static int FLAG_LISTENER = 0x01;
	protected final static int FLAG_TRACK_LISTENER = 0x02;
	protected final static int FLAG_RESOLVE = 0x04;
	protected final static int FLAG_TRACK = 0x08;
	private XMLStreamReader mXmlReader;
	private final Map<String, APIPermissionXMLElement> mEleMap = new HashMap<String, APIPermissionXMLElement>();
	
	// The following variables are for maintaining the current state
	private boolean mReady = false;
	private ClassLoaderReference mClassLoader = null;
	private Atom mPackageName = null;
	private TypeReference mClassRef = null;
	private APIPermission mApiPerm = null;
	public APIPermissionParser(InputStream input)
		throws IOException
	{
		for(APIPermissionXMLElement ele : APIPermissionXMLElement.values())
		{
			mEleMap.put(ele.getTagName(), ele);
		}
		XMLInputFactory factory = XMLInputFactory.newInstance();
		try
		{
			mXmlReader = factory.createXMLStreamReader(input);
		}
		catch(XMLStreamException ex)
		{
			throw new IOException(ex);
		}
	}
	@Override
	protected void finalize()
		throws Throwable
	{
		close();
	}
	/**
	 * It won't close the underlying input source.
	 * @throws IOException
	 */
	public void close()
		throws IOException
	{
		if(mXmlReader != null)
		{
			try
			{
				mXmlReader.close();
			}
			catch(Exception ex)
			{}
			mXmlReader = null;
		}
	}
	protected String getMessagePrefix()
	{
		Location loc = mXmlReader.getLocation();
		int lineNum = loc.getLineNumber();
		int colNum = loc.getColumnNumber();
		StringBuilder builder = new StringBuilder("Line ");
		builder.append(lineNum);
		builder.append(", column ");
		builder.append(colNum);
		builder.append(": ");
		return builder.toString();
	}
	protected void onStartElement()
		throws XMLStreamException
	{
		String eleName = mXmlReader.getLocalName();
		APIPermissionXMLElement ele = mEleMap.get(eleName);
		if(ele == null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid element name " + eleName);
		switch(ele)
		{
		case API_SPEC:
			break;
		case CLASS_LOADER:
			onEleClassLoader();
			break;
		case PACKAGE:
			onElePackage();
			break;
		case CLASS:
			onEleClass();
			break;
		case METHOD:
			onEleMethod();
			break;
		default:
			throw new RuntimeException("Unreachable");
		}
	}
	protected void onEndElement()
		throws XMLStreamException
	{
		String eleName = mXmlReader.getLocalName();
		APIPermissionXMLElement ele = mEleMap.get(eleName);
		if(ele == null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid element name " + eleName);
		switch(ele)
		{
		case API_SPEC:
			break;
		case CLASS_LOADER:
			mClassLoader = null;
			break;
		case PACKAGE:
			mPackageName = null;
			break;
		case CLASS:
			mClassRef = null;
			break;
		case METHOD:
			mApiPerm = null;
			break;
		default:
			throw new RuntimeException("Unreachable");
		}
	}
	protected void onEleClassLoader()
		throws XMLStreamException
	{
		if(mClassLoader != null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid nested <" + APIPermissionXMLElement.CLASS_LOADER.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			if(attrName.equals(APIPermissionXMLElement.A_NAME))
			{
				String attrVal = mXmlReader.getAttributeValue(i);
				mClassLoader = classLoaderName2Ref(attrVal);
			}
		}
		if(mClassLoader == null)
			throw new XMLStreamException(getMessagePrefix() + "Missing attribute \"" + APIPermissionXMLElement.A_NAME + "\"");
	}
	protected void onElePackage()
		throws XMLStreamException
	{
		if(mPackageName != null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid nested <" + APIPermissionXMLElement.PACKAGE.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			if(attrName.equals(APIPermissionXMLElement.A_NAME))
			{
				String attrVal = mXmlReader.getAttributeValue(i);
				mPackageName = Atom.findOrCreateUnicodeAtom(attrVal.replace('.', '/'));
			}
		}
		if(mPackageName == null)
			throw new XMLStreamException(getMessagePrefix() + "Missing attribute \"" + APIPermissionXMLElement.A_NAME + "\"");
	}
	protected void onEleClass()
		throws XMLStreamException
	{
		if(mClassRef != null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid nested <" + APIPermissionXMLElement.CLASS.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			if(attrName.equals(APIPermissionXMLElement.A_NAME))
			{
				String attrVal = mXmlReader.getAttributeValue(i);
				Atom className = Atom.findOrCreateUnicodeAtom(attrVal);
				String clName = "L" + mPackageName + "/" + className;
				mClassRef = className2Ref(clName);
			}
		}
		if(mClassRef == null)
			throw new XMLStreamException(getMessagePrefix() + "Missing attribute \"" + APIPermissionXMLElement.A_NAME + "\"");
	}
	protected int parseParamFlags(String str)
		throws XMLStreamException
	{
		int result = 0;
		String[] toks = str.split(",");
		for(String tok : toks)
		{
			tok = tok.trim();
			if(tok.isEmpty())
				continue;
			if(tok.equals(APIPermissionXMLElement.V_TRACK))
				result |= FLAG_TRACK;
			else if(tok.equals(APIPermissionXMLElement.V_RESOLVE))
				result |= FLAG_RESOLVE;
			else if(tok.equals(APIPermissionXMLElement.V_LISTENER))
				result |= FLAG_LISTENER;
			else if(tok.equals(APIPermissionXMLElement.V_TRACK_LISTENER))
				result |= FLAG_TRACK_LISTENER;
			else
				throw new XMLStreamException(getMessagePrefix() + "Illegal flag \"" + tok + "\"");
		}
		return result;
	}
	protected void onEleMethod()
		throws XMLStreamException
	{
		if(mClassLoader == null || mPackageName == null || mClassRef == null || mApiPerm != null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid location of <" + APIPermissionXMLElement.METHOD.getTagName());
		MethodReference methodRef = null;
		boolean isStatic = DEFAULT_METHOD_STATIC;
		int nAttr = mXmlReader.getAttributeCount();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);;
			if(attrName.equals(APIPermissionXMLElement.A_SIGNATURE))
			{
				methodRef = Utils.parseMethodSignature(Language.JAVA, mClassRef, attrVal);
			}
			else if(attrName.equals(APIPermissionXMLElement.A_STATIC))
			{
				isStatic = parseBooleanAttribute(attrVal);
			}
		}
		if(methodRef == null)
			throw new XMLStreamException(getMessagePrefix() + "Missing attribute \"" + APIPermissionXMLElement.A_SIGNATURE + "\"");
		int nParam = methodRef.getNumberOfParameters();
		if(!isStatic)
			nParam++;
		mApiPerm = new APIPermission(methodRef, isStatic);
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);;
			if(attrName.equals(APIPermissionXMLElement.A_RETURN))
			{
				int retFlag = parseParamFlags(attrVal);
				if((retFlag & ~FLAG_TRACK) != 0)
					throw new XMLStreamException(getMessagePrefix() + "Only value " + APIPermissionXMLElement.V_TRACK + " is allowed for return value");
				mApiPerm.setReturnTrack((retFlag & FLAG_TRACK) != 0);
			}
			else if(attrName.startsWith(APIPermissionXMLElement.A_PARAM))
			{
				int paramIdx;
				try
				{
					paramIdx = Integer.parseInt(attrName.substring(APIPermissionXMLElement.A_PARAM.length()));
					if(paramIdx < 0 || paramIdx >= nParam)
						throw new XMLStreamException(getMessagePrefix() + "Invalid parameter index");
				}
				catch(NumberFormatException ex)
				{
					throw new XMLStreamException(getMessagePrefix() + "Invalid attribute " + attrName);
				}
				int flag = parseParamFlags(attrVal);
				mApiPerm.setParamTrack(paramIdx, (flag & FLAG_TRACK) != 0);
				mApiPerm.setParamListener(paramIdx, (flag & FLAG_LISTENER) != 0);
				mApiPerm.setTrackParamListener(paramIdx, (flag & FLAG_TRACK_LISTENER) != 0);
				mApiPerm.setParamResolve(paramIdx, (flag & FLAG_RESOLVE) != 0);
			}
			else if(attrName.equals(APIPermissionXMLElement.A_PERMISSION))
			{
				String[] toks = attrVal.split(",");
				for(String tok : toks)
				{
					tok = tok.trim();
					if(tok.isEmpty())
						continue;
					mApiPerm.addPermission(tok);
				}
			}
		}
		assert mApiPerm != null;
		mReady = true;
	}
	protected ClassLoaderReference classLoaderName2Ref(String loaderName)
		throws XMLStreamException
	{
		ClassLoaderReference loader;
		if(ClassLoaderReference.Primordial.getName().toString().equals(loaderName))
			loader = ClassLoaderReference.Primordial;
		else if(ClassLoaderReference.Extension.getName().toString().equals(loaderName))
			loader = ClassLoaderReference.Extension;
		else if(ClassLoaderReference.Application.getName().toString().equals(loaderName))
			loader = ClassLoaderReference.Application;
		else			
			throw new XMLStreamException(getMessagePrefix() + "Invalid loader name: " + loaderName);
		return loader;
	}
	protected TypeReference className2Ref(String clName)
		throws XMLStreamException
	{
		return TypeReference.findOrCreate(mClassLoader, clName);
	}
	protected boolean parseBooleanAttribute(String val)
		throws XMLStreamException
	{
		if(val.equals(APIPermissionXMLElement.V_TRUE))
			return true;
		else if(val.equals(APIPermissionXMLElement.V_FALSE))
			return false;
		else
			throw new XMLStreamException(getMessagePrefix() + "The attribute value must be either " + APIPermissionXMLElement.V_TRUE + " or " + APIPermissionXMLElement.V_FALSE);
	}
	
	/**
	 * Return the next API permission, or null if there're no more API permission.
	 * @return
	 * @throws IOException
	 */
	public APIPermission read()
		throws IOException
	{
		mReady = false;
		mApiPerm = null;
		try
		{
			while(!mReady && mXmlReader.hasNext())
			{
				switch(mXmlReader.next())
				{
				case XMLStreamReader.START_ELEMENT:
					onStartElement();
					break;
				case XMLStreamReader.END_ELEMENT:
					onEndElement();
					break;
				}
			}
			if(!mReady)
				return null;
			return mApiPerm;
		}
		catch(XMLStreamException ex)
		{
			throw new IOException(ex);
		}
	}
}
