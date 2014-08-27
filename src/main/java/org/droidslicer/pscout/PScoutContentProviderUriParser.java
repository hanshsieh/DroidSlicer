package org.droidslicer.pscout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PScoutContentProviderUriParser
{
	private BufferedReader mPScoutInput;
	private int mLineCnt = 0;
	private final Pattern mUriPat = Pattern.compile(
			"\\A\\s*" + 
			"(\\S+)" + // URI
			"\\s+" + 
			"(?:([RW])\\s+(\\S+)|grant-uri-permission)" + // read/write + permission OR "grant-uri-permission" 
			"(?:\\s+(pathPrefix|pathPattern|path))?" + // pattern type (optional)
			"\\s*\\z"
			);
	public PScoutContentProviderUriParser(Reader pscoutReader)
	{
		if(pscoutReader instanceof BufferedReader)
			mPScoutInput = (BufferedReader)pscoutReader;
		else
			mPScoutInput = new BufferedReader(pscoutReader);
	}
	public PScoutContentProviderUriParser(String pscoutStr)
	{
		mPScoutInput = new BufferedReader(new StringReader(pscoutStr));
	}
	/**
	 * It won't close the underlying stream.
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		mPScoutInput = null;
	}
	@Override
	protected void finalize() throws Throwable
	{
		close();
	}
	private PScoutContentProviderUri parseContentProviderUri(String line)
		throws PScoutFormatException
	{
		PScoutContentProviderUri result = new PScoutContentProviderUri();
		Matcher m = mUriPat.matcher(line);
		if(!m.find())
			throw new PScoutFormatException(getMessagePrefix() + "Illegal line");
		String uriStr = m.group(1);
		String accTypeStr = m.group(2);
		if(accTypeStr != null)
		{
			if(accTypeStr.equals("R"))
				result.setAccessType(PScoutContentProviderUri.PScoutAccessType.READ);
			else
			{
				assert accTypeStr.equals("W");
				result.setAccessType(PScoutContentProviderUri.PScoutAccessType.WRITE);
			}
			result.setPermission(m.group(3));
		}
		else
		{
			result.setAccessType(PScoutContentProviderUri.PScoutAccessType.GRANT_URI_PERMISSION);
			result.setPermission(null);
		}
		String typeStr = m.group(4);
		try
		{
			if(typeStr != null)
			{
				if(typeStr.equals("path"))
				{
					result.setPathPatternType(PScoutContentProviderUri.PScoutPathPatternType.LITERAL);
					URI uri = new URI(uriStr);
					result.setAuthority(uri.getAuthority());
					result.setPathPattern(uri.getPath());
				}
				else if(typeStr.equals("pathPrefix"))
				{
					result.setPathPatternType(PScoutContentProviderUri.PScoutPathPatternType.PREFIX);
					URI uri = new URI(uriStr);
					result.setAuthority(uri.getAuthority());
					result.setPathPattern(uri.getPath());
				}
				else
				{
					assert typeStr.equals("pathPattern");
					result.setPathPatternType(PScoutContentProviderUri.PScoutPathPatternType.SIMPLE_GLOB);
					String prefix = "content://";
					if(!uriStr.startsWith(prefix))
						throw new PScoutFormatException(getMessagePrefix() + "Illegal scheme of URI " + uriStr);
					int slashIdx = uriStr.indexOf('/', prefix.length());
					if(slashIdx < 0)
					{
						if(uriStr.endsWith(".*"))
						{
							result.setAuthority(uriStr.substring(prefix.length(), uriStr.length() - 2));
							result.setPathPattern(".*");
						}
						else
						{
							slashIdx = uriStr.length();
							result.setAuthority(uriStr.substring(prefix.length()));
							result.setPathPattern("");
						}
					}
					else
					{
						result.setAuthority(uriStr.substring(prefix.length(), slashIdx));
						result.setPathPattern(uriStr.substring(slashIdx));
					}
				}
			}
			else
			{
				result.setPathPatternType(PScoutContentProviderUri.PScoutPathPatternType.NONE);
				URI uri = new URI(uriStr);
				String path = uri.getPath();
				if(path != null && !path.isEmpty())
					throw new PScoutFormatException(getMessagePrefix() + "Path is given in the URI, but no path pattern type is specified");
				result.setAuthority(uri.getAuthority());
				result.setPathPattern(null);
			}
		}
		catch(URISyntaxException ex)
		{
			throw new PScoutFormatException(getMessagePrefix() + "Invalid URI " + uriStr);
		}
		return result;
	}
	protected String getMessagePrefix()
	{
		return "Line " + mLineCnt + ": ";
	}
	public PScoutContentProviderUri read() throws IOException, PScoutFormatException
	{
		do
		{
			String line = mPScoutInput.readLine();
			if(line == null)
				return null;
			mLineCnt++;
			line = line.trim();
			if(line.isEmpty())
				continue;
			return parseContentProviderUri(line);
		}while(true);	
	}
}
