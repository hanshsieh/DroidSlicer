package org.droidslicer.pscout;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.droidslicer.config.PathPermission;
import org.droidslicer.config.PathPermission.PathPatternType;
import org.droidslicer.config.ProviderPermission;
import org.droidslicer.config.ProviderPermissionXMLElement;

import com.ibm.wala.util.io.CommandLine;

public class PScoutContentProviderUriToXML
{
	private static String USAGE = "-p <pscout_content_provider_uri_file> -o <output_file>";
	private static String CMD_PSCOUT = "p";
	private static String CMD_OUTPUT = "o";
	protected XMLStreamWriter mXmlWriter = null;
	protected int mIndent = 0;
	public static void main(String[] args)
	{
		Properties props = CommandLine.parse(args);
		String pscoutFileName = props.getProperty(CMD_PSCOUT);
		String outputFileName = props.getProperty(CMD_OUTPUT);
		try
		{
			new PScoutContentProviderUriToXML(pscoutFileName, outputFileName);
			System.out.println("Finished");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
			return;
		}		
	}
	protected void writeComment(String comment)
			throws XMLStreamException
	{
		mXmlWriter.writeCharacters("\n");
		for(int i = 0; i < mIndent; ++i)
		{
			mXmlWriter.writeCharacters("\t");
		}
		mXmlWriter.writeComment("\n\t" + comment + "\n");
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
	public Map<String, ProviderPermission> parse(String pscoutFileName)
		throws IOException, PScoutFormatException
	{
		Reader reader = null;
		PScoutContentProviderUriParser parser = null;
		Map<String, ProviderPermission> providerPerms = new HashMap<String, ProviderPermission>();
		try
		{
			reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(pscoutFileName)));
			parser = new PScoutContentProviderUriParser(reader);
			PScoutContentProviderUri entry;
			while((entry = parser.read()) != null)
			{
				boolean isRead;
				switch(entry.getAccessType())
				{
				case READ:
					isRead = true;
					break;
				case WRITE:
					isRead = false;
					break;
				default: // Ignore grant-uri-permission
					continue;
				}
				String auth = entry.getAuthority();
				
				// Get or create a ProviderPermission with the authority
				ProviderPermission providerPerm = providerPerms.get(auth);
				if(providerPerm == null)
				{
					providerPerm = new ProviderPermission();
					providerPerm.addAuthority(auth);
					providerPerms.put(auth, providerPerm);
				}
				
				// Handle the path pattern type
				PathPatternType pathPatType;
				switch(entry.getPathPatternType())
				{
				case LITERAL:
					pathPatType = PathPatternType.LITERAL;
					break;
				case PREFIX:
					pathPatType = PathPatternType.PREFIX;
					break;
				case SIMPLE_GLOB:
					pathPatType = PathPatternType.SIMPLE_GLOB;
					break;
				default: // NONE
					// The permission describe the permission of the whole provider
					if(isRead)
						providerPerm.setReadPermission(entry.getPermission());
					else
						providerPerm.setWritePermission(entry.getPermission());
					continue;
				}
				
				// The permission describe the permission of a path pattern
				
				String path = entry.getPathPattern();
				
				// Find if there's already a PathPermission with the same path pattern and
				// path pattern type. If we can find it, then try to merge them.
				PathPermission oriPathPerm = null;
				assert entry.getPermission() != null;
				for(PathPermission pathPerm : providerPerm.getPathPermissions())
				{
					if(pathPerm.getPathPattern().equals(path) &&
						pathPerm.getPathPatternType().equals(pathPatType))
					{
						if(isRead)
						{
							// If there's permission conflict, ignore it
							String readPerm = pathPerm.getReadPermission();
							if(readPerm != null && !readPerm.equals(entry.getPermission()))
								continue;
						}
						else
						{
							// If there's permission conflict, ignore it
							String writePerm = pathPerm.getWritePermission();
							if(writePerm != null && !writePerm.equals(entry.getPermission()))
								continue;
						}
						oriPathPerm = pathPerm;
						break;
					}
				}
				
				// If there's no existing path permission with same path pattern and path pattern type,
				// create a new one.
				if(oriPathPerm == null)
				{
					String readPerm = null, writePerm = null;
					if(isRead)
						readPerm = entry.getPermission();
					else
						writePerm = entry.getPermission();
					oriPathPerm = new PathPermission(entry.getPathPattern(), pathPatType);
					oriPathPerm.setReadPermission(readPerm);
					oriPathPerm.setWritePermission(writePerm);
					providerPerm.addPathPermission(oriPathPerm);
				}
				else
				{
					// Otherwise, remove the original one, and insert a new one with merged information.
					String readPerm = oriPathPerm.getReadPermission(), writePerm = oriPathPerm.getWritePermission();
					if(isRead)
						readPerm = entry.getPermission();
					else
						writePerm = entry.getPermission();
					PathPermission newPathPerm = new PathPermission(entry.getPathPattern(), pathPatType);
					newPathPerm.setReadPermission(readPerm);
					newPathPerm.setWritePermission(writePerm);
				
					providerPerm.removePathPermission(oriPathPerm);
					providerPerm.addPathPermission(newPathPerm);
				}
			}
			return providerPerms;
		}
		finally
		{

			if(parser != null)
			{
				try
				{
					parser.close();
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
		}
	}
	private static boolean equalAllowNull(String val1, String val2)
	{
		if(val1 == null)
			return val2 == null;
		else
			return val1.equals(val2);
	}
	protected Collection<ProviderPermission> mergeProviderPerms(Map<String, ProviderPermission> providerPerms)
	{
		List<ProviderPermission> merged = new ArrayList<ProviderPermission>();
		for(Map.Entry<String, ProviderPermission> entry : providerPerms.entrySet())
		{
			ProviderPermission providerPerm = entry.getValue();
			boolean found = false;
			for(ProviderPermission oldProviderPerm : merged)
			{
				String oldReadPerm = oldProviderPerm.getReadPermission();
				String oldWritePerm = oldProviderPerm.getWritePermission();
				String newReadPerm = providerPerm.getReadPermission();
				String newWritePerm = providerPerm.getWritePermission();
				if(!equalAllowNull(oldReadPerm, newReadPerm) || 
					!equalAllowNull(oldWritePerm, newWritePerm))
					continue;
				Collection<PathPermission> oldPathPerms = oldProviderPerm.getPathPermissions();
				Collection<PathPermission> pathPerms = providerPerm.getPathPermissions();
				if(oldPathPerms.size() != pathPerms.size() || !pathPerms.containsAll(oldPathPerms))
					continue;
				oldProviderPerm.addAuthority(entry.getKey());
				found = true;
				break;
			}
			if(found)
				continue;
			merged.add(providerPerm);
		}
		return merged;
	}
	public PScoutContentProviderUriToXML(String pscoutFileName, String outputFileName)
		throws Exception
	{
		if(pscoutFileName == null || outputFileName == null)
		{
			System.err.println(USAGE);
			System.exit(1);
			return;
		}
		OutputStream output = null;
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		try
		{
			output = new FileOutputStream(outputFileName);
			mXmlWriter = xmlOutputFactory.createXMLStreamWriter(output, "UTF-8");
			mXmlWriter.writeStartDocument();
			String comment = "Auto generated file by " + PScoutContentProviderUriToXML.class.getName();
			writeComment(comment);
			Collection<ProviderPermission> providerPerms;
			{
				Map<String, ProviderPermission> providerPermsMap = parse(pscoutFileName);
				providerPerms = mergeProviderPerms(providerPermsMap);
			}
			writeStartElement(ProviderPermissionXMLElement.PROVIDER_SPEC.getTagName());
			for(ProviderPermission providerPerm : providerPerms)
			{
				writeStartElement(ProviderPermissionXMLElement.PROVIDER.getTagName());
				{
					StringBuilder auths = new StringBuilder();
					boolean first = true;
					for(String auth : providerPerm.getAuthories())
					{
						if(first)
							first = false;
						else
							auths.append(';');
						auths.append(auth);
					}
					mXmlWriter.writeAttribute(ProviderPermissionXMLElement.A_AUTHORITIES, auths.toString());
				}
				{
					String readPerm = providerPerm.getReadPermission();
					String writePerm = providerPerm.getWritePermission();
					if(equalAllowNull(readPerm, writePerm))
					{
						if(readPerm != null)
						{
							mXmlWriter.writeAttribute(ProviderPermissionXMLElement.A_PERMISSION, readPerm);
						}
					}
					else
					{
						if(readPerm != null)
							mXmlWriter.writeAttribute(ProviderPermissionXMLElement.A_READ_PERMISSION, readPerm);
						if(writePerm != null)
							mXmlWriter.writeAttribute(ProviderPermissionXMLElement.A_WRITE_PERMISSION, writePerm);
					}
				}
				for(PathPermission pathPerm : providerPerm.getPathPermissions())
				{
					writeEmptyElement(ProviderPermissionXMLElement.PATH_PERMISSION.getTagName());
					switch(pathPerm.getPathPatternType())
					{
					case LITERAL:
						mXmlWriter.writeAttribute(ProviderPermissionXMLElement.A_PATH, pathPerm.getPathPattern());
						break;
					case SIMPLE_GLOB:
						mXmlWriter.writeAttribute(ProviderPermissionXMLElement.A_PATH_PATTERN, pathPerm.getPathPattern());
						break;
					case PREFIX:
						mXmlWriter.writeAttribute(ProviderPermissionXMLElement.A_PATH_PREFIX, pathPerm.getPathPattern());
						break;
					default:
						throw new RuntimeException("Unreachable");
					}
					{
						String readPerm = pathPerm.getReadPermission();
						String writePerm = pathPerm.getWritePermission();
						if(equalAllowNull(readPerm, writePerm))
						{
							if(readPerm != null)
							{
								mXmlWriter.writeAttribute(ProviderPermissionXMLElement.A_PERMISSION, readPerm);
							}
						}
						else
						{
							if(readPerm != null)
								mXmlWriter.writeAttribute(ProviderPermissionXMLElement.A_READ_PERMISSION, readPerm);
							if(writePerm != null)
								mXmlWriter.writeAttribute(ProviderPermissionXMLElement.A_WRITE_PERMISSION, writePerm);
						}
					}
				}
				writeEndElement();
			}
			writeEndElement();
			mXmlWriter.writeEndDocument();
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
		}
	}
}
