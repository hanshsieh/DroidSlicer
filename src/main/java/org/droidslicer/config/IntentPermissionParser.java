package org.droidslicer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class IntentPermissionParser
{
	private boolean mReady = false;
	private XMLStreamReader mXmlReader;
	private final Map<String, IntentPermissionXMLElement> mEleMap = new HashMap<String, IntentPermissionXMLElement>();
	
	// The following variables are for maintaining the current state
	private IntentPermission mIntentPerm;
	
	public IntentPermissionParser(InputStream input)
		throws IOException
	{
		for(IntentPermissionXMLElement ele : IntentPermissionXMLElement.values())
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
	protected void onEleIntent()
		throws XMLStreamException
	{
		mIntentPerm = null;
		int nAttr = mXmlReader.getAttributeCount();
		String action = null;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			if(attrName.equals(IntentPermissionXMLElement.A_ACTION))
			{
				action = mXmlReader.getAttributeValue(i);
			}
		}
		if(action == null)
			throw new XMLStreamException(getMessagePrefix() + "Missing attribute \"" + IntentPermissionXMLElement.A_ACTION + "\"");
		mIntentPerm = new IntentPermission(action);
	}
	public void onEleSender()
		throws XMLStreamException
	{
		if(mIntentPerm == null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid position of <" + IntentPermissionXMLElement.SENDER.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		String perm = null;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			if(attrName.equals(IntentPermissionXMLElement.A_PERMISSION))
			{
				perm = mXmlReader.getAttributeValue(i);
			}
		}
		if(perm == null)
			throw new XMLStreamException(getMessagePrefix() + "Missing attribute \"" + IntentPermissionXMLElement.A_PERMISSION + "\"");
		mIntentPerm.addSenderPermission(perm);
	}
	public void onEleReceiver()
			throws XMLStreamException
	{
		if(mIntentPerm == null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid position of <" + IntentPermissionXMLElement.SENDER.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		String perm = null;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			if(attrName.equals(IntentPermissionXMLElement.A_PERMISSION))
			{
				perm = mXmlReader.getAttributeValue(i);
			}
		}
		if(perm == null)
			throw new XMLStreamException(getMessagePrefix() + "Missing attribute \"" + IntentPermissionXMLElement.A_PERMISSION + "\"");
		mIntentPerm.addReceiverPermission(perm);
	}
	protected void onStartElement()
			throws XMLStreamException
	{
		String eleName = mXmlReader.getLocalName();
		IntentPermissionXMLElement ele = mEleMap.get(eleName);
		if(ele == null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid element name " + eleName);
		switch(ele)
		{
		case INTENT_SPEC:
			break;
		case INTENT:
			onEleIntent();
			break;
		case SENDER:
			onEleSender();
			break;
		case RECEIVER:
			onEleReceiver();
			break;
		default:
			throw new RuntimeException("Unreachable");
		}
	}
	protected void onEndElement()
		throws XMLStreamException
	{
		String eleName = mXmlReader.getLocalName();
		IntentPermissionXMLElement ele = mEleMap.get(eleName);
		if(ele == null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid element name " + eleName);
		switch(ele)
		{
		case INTENT_SPEC:
			break;
		case INTENT:
			mReady = true;
			break;
		case SENDER:
			break;
		case RECEIVER:
			break;
		default:
			throw new RuntimeException("Unreachable");
		}
	}
	/**
	 * Return the next API permission, or null if there're no more API permission.
	 * @return
	 * @throws IOException
	 */
	public IntentPermission read()
		throws IOException
	{
		mReady = false;
		mIntentPerm = null;
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
			return mIntentPerm;
		}
		catch(XMLStreamException ex)
		{
			throw new IOException(ex);
		}
	}
}
