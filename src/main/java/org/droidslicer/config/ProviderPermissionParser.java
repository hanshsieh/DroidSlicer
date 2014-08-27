package org.droidslicer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.droidslicer.config.PathPermission.PathPatternType;

public class ProviderPermissionParser
{
	private final Map<String, ProviderPermissionXMLElement> mEleMap = new HashMap<String, ProviderPermissionXMLElement>();
	private XMLStreamReader mXmlReader;
	private boolean mReady;
	private ProviderPermission mProviderPerm;
	public ProviderPermissionParser(InputStream input)
			throws IOException
	{
		for(ProviderPermissionXMLElement ele : ProviderPermissionXMLElement.values())
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
	public void close()
		throws IOException
	{
		if(mXmlReader != null)
		{
			try
			{
				mXmlReader.close();
			}
			catch(XMLStreamException ex)
			{
				throw new IOException(ex);
			}
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
	protected void onEleProvider()
		throws XMLStreamException
	{
		if(mProviderPerm != null)
			throw new XMLStreamException(getMessagePrefix() + "Illegal nested element <" + ProviderPermissionXMLElement.PROVIDER.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		String[] auths = null;
		String perm = null, readPerm = null, writePerm = null;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);
			if(attrName.equals(ProviderPermissionXMLElement.A_AUTHORITIES))
			{
				if(auths != null)
					throw new XMLStreamException(getMessagePrefix() + "Multiple " + ProviderPermissionXMLElement.A_AUTHORITIES + " attribute");
				auths = attrVal.split(";");
				for(int j = 0; j < auths.length; ++j)
				{
					auths[j] = auths[j].trim();
					if(auths[j].isEmpty())
						throw new XMLStreamException(getMessagePrefix() + "Illegal empty authority");
				}
			}
			else if(attrName.equals(ProviderPermissionXMLElement.A_PERMISSION))
			{
				perm = attrVal.trim();
			}
			else if(attrName.equals(ProviderPermissionXMLElement.A_READ_PERMISSION))
			{
				readPerm = attrVal.trim();
			}
			else if(attrName.equals(ProviderPermissionXMLElement.A_WRITE_PERMISSION))
			{
				writePerm = attrVal.trim();
			}
			else
				throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		if(auths == null)
			throw new XMLStreamException(getMessagePrefix() + "Missing attribute " + ProviderPermissionXMLElement.A_AUTHORITIES);
		mProviderPerm = new ProviderPermission();
		for(String auth : auths)
			mProviderPerm.addAuthority(auth);
		if(perm != null)
		{
			mProviderPerm.setReadPermission(perm);
			mProviderPerm.setWritePermission(perm);
		}
		if(readPerm != null)
			mProviderPerm.setReadPermission(readPerm);
		if(writePerm != null)
			mProviderPerm.setWritePermission(writePerm);
	}
	protected void onElePathPerm()
		throws XMLStreamException
	{
		if(mProviderPerm == null)
			throw new XMLStreamException(getMessagePrefix() + "Illegal position of element <" + ProviderPermissionXMLElement.PATH_PERMISSION.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		int nPathAttr = 0;
		String path = null, perm = null, readPerm = null, writePerm = null;
		PathPatternType pathPatType = null;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);;
			if(attrName.equals(ProviderPermissionXMLElement.A_PATH))
			{
				path = attrVal;
				++nPathAttr;
				pathPatType = PathPatternType.LITERAL;
			}
			else if(attrName.equals(ProviderPermissionXMLElement.A_PATH_PREFIX))
			{
				path = attrVal;
				++nPathAttr;
				pathPatType = PathPatternType.PREFIX;
			}
			else if(attrName.equals(ProviderPermissionXMLElement.A_PATH_PATTERN))
			{
				path = attrVal;
				++nPathAttr;
				pathPatType = PathPatternType.SIMPLE_GLOB;
			}
			else if(attrName.equals(ProviderPermissionXMLElement.A_PERMISSION))
			{
				perm = attrVal.trim();
			}
			else if(attrName.equals(ProviderPermissionXMLElement.A_READ_PERMISSION))
			{
				readPerm = attrVal.trim();
			}
			else if(attrName.equals(ProviderPermissionXMLElement.A_WRITE_PERMISSION))
			{
				writePerm = attrVal.trim();
			}
			else
				throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		if(nPathAttr > 1)
		{
			throw new XMLStreamException(getMessagePrefix() + "There can be only one of the attributes " + 
					ProviderPermissionXMLElement.A_PATH + ", " + 
					ProviderPermissionXMLElement.A_PATH_PREFIX + ", and" +
					ProviderPermissionXMLElement.A_PATH_PATTERN);
		}
		if(readPerm == null)
			readPerm = perm;
		if(writePerm == null)
			writePerm = perm;
		if(path == null || pathPatType == null)
			throw new XMLStreamException(getMessagePrefix() + "Missing attribute for path pattern");
		PathPermission pathPerm = new PathPermission(path, pathPatType);
		pathPerm.setReadPermission(readPerm);
		pathPerm.setWritePermission(writePerm);
		mProviderPerm.addPathPermission(pathPerm);
	}
	protected void onStartElement()
		throws XMLStreamException
	{
		String eleName = mXmlReader.getLocalName();
		ProviderPermissionXMLElement ele = mEleMap.get(eleName);
		if(ele == null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid element name " + eleName);
		switch(ele)
		{
		case PROVIDER_SPEC:
			break;
		case PROVIDER:
			onEleProvider();
			break;
		case PATH_PERMISSION:
			onElePathPerm();
			break;
		default:
			throw new RuntimeException("Unreachable");
		}
	}
	protected void onEndElement()
		throws XMLStreamException
	{
		String eleName = mXmlReader.getLocalName();
		ProviderPermissionXMLElement ele = mEleMap.get(eleName);
		if(ele == null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid element name " + eleName);
		switch(ele)
		{
		case PROVIDER_SPEC:
			break;
		case PROVIDER:
			mReady = true;
			break;
		case PATH_PERMISSION:
			break;
		default:
			throw new RuntimeException("Unreachable");
		}
	}
	public ProviderPermission read()
		throws IOException
	{
		mProviderPerm = null;
		mReady = false;
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
			ProviderPermission result = mProviderPerm;
			mProviderPerm = null;
			return result;
		}
		catch(XMLStreamException ex)
		{
			throw new IOException(ex);
		}
	}
}
