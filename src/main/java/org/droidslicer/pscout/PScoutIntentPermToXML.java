package org.droidslicer.pscout;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.droidslicer.config.IntentPermissionXMLElement;

import com.ibm.wala.util.io.CommandLine;

public class PScoutIntentPermToXML
{
	private final static String USAGE = "Usage: -p <pscout_file> -o <output>";
	private final static String CMD_PSCOUT = "p";
	private final static String CMD_OUTPUT = "o";
	protected XMLStreamWriter mXmlWriter = null;
	private int mIndent = 0;
	public static void main(String[] args)
		throws Exception
	{
		Properties props = CommandLine.parse(args);
		String pscoutFilePath = props.getProperty(CMD_PSCOUT);
		String outputFilePath = props.getProperty(CMD_OUTPUT);
		if(pscoutFilePath == null || outputFilePath == null)
		{
			System.out.println(USAGE);
			System.exit(1);
			return;
		}
		new PScoutIntentPermToXML(pscoutFilePath, outputFilePath);
	}
	protected void writeStartElement(String localName)
			throws XMLStreamException
	{
		mXmlWriter.writeCharacters("\n");
		for(int i = 0; i < mIndent; ++i)
		{
			mXmlWriter.writeCharacters("\t");
		}
		mXmlWriter.writeStartElement(localName);
		++mIndent;
	}
	protected void writeEmptyElement(String localName)
			throws XMLStreamException
	{
		mXmlWriter.writeCharacters("\n");
		for(int i = 0; i < mIndent; ++i)
			mXmlWriter.writeCharacters("\t");
		mXmlWriter.writeEmptyElement(localName);
	}
	protected void writeEndElement()
			throws XMLStreamException
	{
		mXmlWriter.writeCharacters("\n");
		--mIndent;
		for(int i = 0; i < mIndent; ++i)
			mXmlWriter.writeCharacters("\t");
		mXmlWriter.writeEndElement();
	}
	protected void writeComment(String comment)
			throws XMLStreamException
	{
		mXmlWriter.writeCharacters("\n");
		StringBuilder builder = new StringBuilder("\n");
		String[] toks = comment.split("\n");
		for(String tok : toks)
		{
			for(int i = 0; i < mIndent; ++i)
			{
				builder.append('\t');
			}
			builder.append(tok);
			builder.append('\n');
		}
		mXmlWriter.writeComment(builder.toString());		
	}
	public PScoutIntentPermToXML(String pscoutFilePath, String outputFilePath)
		throws IOException
	{
		OutputStream output = null;
		Reader reader = null;
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		try
		{
			output = new FileOutputStream(outputFilePath);
			reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(pscoutFilePath)));
			PScoutIntentPermParser parser = new PScoutIntentPermParser(reader);
			Map<String, Set<PScoutIntentPerm>> intentPermsMap = new HashMap<String, Set<PScoutIntentPerm>>();
			{
				PScoutIntentPerm intentPerm;
				while((intentPerm = parser.read()) != null)
				{
					String action = intentPerm.getAction();
					Set<PScoutIntentPerm> intentPerms = intentPermsMap.get(action);
					if(intentPerms == null)
					{
						intentPerms = new HashSet<PScoutIntentPerm>();
						intentPermsMap.put(action, intentPerms);
					}
					intentPerms.add(intentPerm);
				}
			}
			mXmlWriter = xmlOutputFactory.createXMLStreamWriter(output, "UTF-8");
			mXmlWriter.writeStartDocument();
			{
				String comment = 
					"Auto-generated file from PScout intent-permission file.\n" + 
					"You may need to modify the intent-permission associated with android.permission.NFC.\n" + 
					"For example: \n" +
					"<intent action=\"android.nfc.action.NDEF_DISCOVERED\">\n" + 
					"	<receiver permission=\"android.permission.NFC\"/>\n" + 
					"</intent>\n" + 
					"<intent action=\"android.nfc.action.TECH_DISCOVERED\">\n" + 
					"	<receiver permission=\"android.permission.NFC\"/>\n" + 
					"</intent>\n" + 
					"<intent action=\"android.nfc.action.TAG_DISCOVERED\">\n" + 
					"	<receiver permission=\"android.permission.NFC\"/>\n" + 
					"</intent>\n\n" +
					"Some intent broadcasted by system do not require permissions. These Intent's are not\n" +
					"included by PScout. For example: \n" +
					"<intent action=\"android.intent.action.USER_PRESENT\">\n" +
					"</intent>\n" + 
					"<intent action=\"android.intent.action.USER_INITIALIZE\">\n" +
					"</intent>\n" + 
					"<intent action=\"android.intent.action.USER_FOREGROUND\">\n" +
					"</intent>\n" + 
					"<intent action=\"android.intent.action.USER_BACKGROUND\">\n" +
					"</intent>\n" + 
					"<intent action=\"android.intent.action.SCREEN_ON\">\n" +
					"</intent>\n" + 
					"<intent action=\"android.intent.action.SCREEN_OFF\">\n" +
					"</intent>\n" + 
					"<intent action=\"android.intent.action.SIG_STR\">\n" + 
					"</intent>";
				writeComment(comment);
			}
			writeStartElement(IntentPermissionXMLElement.INTENT_SPEC.getTagName());
			for(Map.Entry<String, Set<PScoutIntentPerm>> entry : intentPermsMap.entrySet())
			{
				String action = entry.getKey();
				writeStartElement(IntentPermissionXMLElement.INTENT.getTagName());
				mXmlWriter.writeAttribute(IntentPermissionXMLElement.A_ACTION, action);
				for(PScoutIntentPerm intentPerm : entry.getValue())
				{
					if(intentPerm.isSender())
						writeEmptyElement(IntentPermissionXMLElement.SENDER.getTagName());
					else
						writeEmptyElement(IntentPermissionXMLElement.RECEIVER.getTagName());
					mXmlWriter.writeAttribute(IntentPermissionXMLElement.A_PERMISSION, intentPerm.getPermission());
				}
				writeEndElement();
			}
			writeEndElement();
			mXmlWriter.writeEndDocument();
			System.out.println("Finished");
		}
		catch(XMLStreamException ex)
		{
			throw new IOException(ex);
		}
		finally
		{
			if(output != null)
			{
				try
				{
					output.close();
				}
				catch(Exception ex)
				{}
			}
			if(reader != null)
			{
				try
				{
					reader.close();
				}
				catch(Exception ex)
				{}
			}
			if(mXmlWriter != null)
			{
				try
				{
					mXmlWriter.close();
				}
				catch(Exception ex)
				{}
				mXmlWriter = null;
			}
		}
	}
}
