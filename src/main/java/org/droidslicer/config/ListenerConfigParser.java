package org.droidslicer.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ListenerConfigParser
{
	private BufferedReader mReader;
	public ListenerConfigParser(InputStream input)
		throws IOException
	{
		mReader = new BufferedReader(new InputStreamReader(input));
	}
	/**
	 * This method won't close the underlying source.
	 * @throws IOException
	 */
	public void close()
		throws IOException
	{
		// Just release the resource, don't close the underlying source
		mReader = null;
	}
	public String read()
		throws IOException
	{
		String line;
		while((line = mReader.readLine()) != null)
		{
			line = line.trim();
			// Skip comment and empty line
			if(!line.startsWith("#") &&
				!line.isEmpty())
			{
				return line;
			}
		}
		return null;
	}
}
