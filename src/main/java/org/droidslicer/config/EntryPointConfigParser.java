package org.droidslicer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.MutableIntSet;

public class EntryPointConfigParser
{
	public static class EntryMethodConfig
	{
		private final MethodReference mMethodRef;
		private final boolean mIsStatic;
		private final MutableIntSet mTrackParamsAndRet = new BitVectorIntSet();
		public EntryMethodConfig(MethodReference methodRef, boolean isStatic)
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
	}
	private final static boolean DEFAULT_METHOD_STATIC = false;
	protected final static int FLAG_TRACK = 0x01;
	private XMLStreamReader mXmlReader;
	private final Map<String, EntryPointXMLElement> mEleMap = new HashMap<String, EntryPointXMLElement>();
	
	// The following variables are for maintaining the current state
	private boolean mReady = false;
	private TypeReference mClassType = null;
	private EntryMethodConfig mEntryMethod = null;
	public EntryPointConfigParser(InputStream input)
		throws IOException
	{
		for(EntryPointXMLElement ele : EntryPointXMLElement.values())
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
		EntryPointXMLElement ele = mEleMap.get(eleName);
		if(ele == null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid element name " + eleName);
		switch(ele)
		{
		case ENTRY_SPEC:
			break;
		case ACTIVITY:
			mClassType = TypeId.ANDROID_ACTIVITY.getTypeReference();
			break;
		case RECEIVER:
			mClassType = TypeId.ANDROID_RECEIVER.getTypeReference();
			break;
		case PROVIDER:
			mClassType = TypeId.ANDROID_PROVIDER.getTypeReference();
			break;
		case SERVICE:
			mClassType = TypeId.ANDROID_SERVICE.getTypeReference();
			break;
		case APPLICATION:
			mClassType = TypeId.ANDROID_APPLICATION.getTypeReference();
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
		EntryPointXMLElement ele = mEleMap.get(eleName);
		if(ele == null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid element name " + eleName);
		switch(ele)
		{
		case ENTRY_SPEC:
			break;
		case ACTIVITY:
		case RECEIVER:
		case PROVIDER:
		case SERVICE:
		case APPLICATION:
			mClassType = null;
			break;
		case METHOD:
			mEntryMethod = null;
			break;
		default:
			throw new RuntimeException("Unreachable");
		}
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
			if(tok.equals(EntryPointXMLElement.V_TRACK))
				result |= FLAG_TRACK;
			else
				throw new XMLStreamException(getMessagePrefix() + "Illegal flag \"" + tok + "\"");
		}
		return result;
	}
	protected void onEleMethod()
		throws XMLStreamException
	{
		if(mClassType == null || mEntryMethod != null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid location of <" + EntryPointXMLElement.METHOD.getTagName());
		MethodReference methodRef = null;
		boolean isStatic = DEFAULT_METHOD_STATIC;
		int nAttr = mXmlReader.getAttributeCount();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);;
			if(attrName.equals(EntryPointXMLElement.A_SIGNATURE))
			{
				methodRef = Utils.parseMethodSignature(Language.JAVA, mClassType, attrVal);
			}
			else if(attrName.equals(EntryPointXMLElement.A_STATIC))
			{
				isStatic = parseBooleanAttribute(attrVal);
			}
		}
		if(methodRef == null)
			throw new XMLStreamException(getMessagePrefix() + "Missing attribute \"" + EntryPointXMLElement.A_SIGNATURE + "\"");
		int nParam = methodRef.getNumberOfParameters();
		if(!isStatic)
			nParam++;
		mEntryMethod = new EntryMethodConfig(methodRef, isStatic);
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);;
			if(attrName.startsWith(EntryPointXMLElement.A_PARAM))
			{
				int paramIdx;
				try
				{
					paramIdx = Integer.parseInt(attrName.substring(EntryPointXMLElement.A_PARAM.length()));
					if(paramIdx < 0 || paramIdx >= nParam)
						throw new XMLStreamException(getMessagePrefix() + "Invalid parameter index");
				}
				catch(NumberFormatException ex)
				{
					throw new XMLStreamException(getMessagePrefix() + "Invalid attribute " + attrName);
				}
				int flag = parseParamFlags(attrVal);
				if((flag & FLAG_TRACK) != 0)
					mEntryMethod.setParamTrack(paramIdx, true);
			}
		}
		assert mEntryMethod != null;
		mReady = true;
	}
	protected boolean parseBooleanAttribute(String val)
		throws XMLStreamException
	{
		if(val.equals(EntryPointXMLElement.V_TRUE))
			return true;
		else if(val.equals(EntryPointXMLElement.V_FALSE))
			return false;
		else
			throw new XMLStreamException(getMessagePrefix() + "The attribute value must be either " + EntryPointXMLElement.V_TRUE + " or " + EntryPointXMLElement.V_FALSE);
	}
	
	/**
	 * Return the next API permission, or null if there're no more API permission.
	 * @return
	 * @throws IOException
	 */
	public EntryMethodConfig read()
		throws IOException
	{
		mReady = false;
		mEntryMethod = null;
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
			return mEntryMethod;
		}
		catch(XMLStreamException ex)
		{
			throw new IOException(ex);
		}
	}
}
