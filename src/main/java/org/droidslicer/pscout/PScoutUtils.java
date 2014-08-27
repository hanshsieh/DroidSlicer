package org.droidslicer.pscout;

public class PScoutUtils
{
	public static boolean isWhitespaceString(String line)
	{
		for(int i = 0; i < line.length(); ++i)
		{
			if(!Character.isWhitespace(line.charAt(i)))
				return false;
		}
		return true;
	}
}
