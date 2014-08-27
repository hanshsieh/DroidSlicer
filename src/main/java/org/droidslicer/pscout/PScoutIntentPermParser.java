package org.droidslicer.pscout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class PScoutIntentPermParser
{
	private final BufferedReader mInput;
	private int mLineCnt = 0;
	public PScoutIntentPermParser(Reader pscoutReader)
	{
		if(pscoutReader instanceof BufferedReader)
			mInput = (BufferedReader)pscoutReader;
		else
			mInput = new BufferedReader(pscoutReader);
	}
	protected String getMessagePrefix()
	{
		return "Line " + mLineCnt + ": ";
	}
	public PScoutIntentPerm read()
		throws IOException
	{
		do
		{
			String line = mInput.readLine();
			if(line == null)
				return null;
			++mLineCnt;
			if(PScoutUtils.isWhitespaceString(line))
				continue;
			String[] toks = line.split(" ");
			int tokenCnt = 0;
			String action = null, perm = null;
			boolean isSender = false;
			
			for(String tok : toks)			
			{
				if(PScoutUtils.isWhitespaceString(tok))
					continue;
				switch(tokenCnt)
				{
				case 0:
					action = tok;
					break;
				case 1:
					perm = tok;
					break;
				case 2:
					{
						if(tok.equals("S"))
							isSender = true;
						else if(tok.equals("R"))
							isSender = false;
						else
							throw new IOException(getMessagePrefix() + "Invalid token, must be either \"S\" or \"R\"");
						break;
					}
				default:
					throw new IOException(getMessagePrefix() + "Too many tokens");
				}
				++tokenCnt;
			}
			if(tokenCnt != 3)
				throw new IOException(getMessagePrefix() + "Too few tokens");
			return new PScoutIntentPerm(action, perm, isSender);
		}while(true);
	}
}
