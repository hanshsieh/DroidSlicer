package org.droidslicer.pscout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PScoutAPIPermParser
{
	private final static int STATE_PERM = 1;
	private final static int STATE_NUM_CALLERS = 2;
	private final static int STATE_API_PERM = 3;
	private BufferedReader mPScoutInput;
	private final Pattern mPermStartPat = Pattern.compile("\\s*Permission\\s*:\\s*(\\S+)\\s*"); // permission name
	private final Pattern mCallersPat = Pattern.compile("\\s*(\\d+)\\s*Callers\\s*:\\s*"); // number of callers of the permission
	private final Pattern mApiPermPat = Pattern.compile(
			"\\s*<\\s*" +
			"([^:]+)"+ // class name
			"\\s*:\\s*" +
			"(\\S+)" + // return type
			"\\s+" + 
			"([^(]+)" + // function name
			"\\(" +
			"([^)]*)" + // argument types (entire argument types descriptor)
			"\\)>\\s*\\(" + 
			"(\\d+)?" + // a tailing count (what's does it mean?)
			"\\)\\s*");
	private String mPermName = null, mPackageName = null, mClassName = null, mRetType = null, mMethodName = null;
	private String[] mArgsTypes = null;
	private int mState = STATE_PERM;
	public PScoutAPIPermParser(Reader pscoutReader)
	{
		if(pscoutReader instanceof BufferedReader)
			this.mPScoutInput = (BufferedReader)pscoutReader;
		else
			this.mPScoutInput = new BufferedReader(pscoutReader);
	}
	public PScoutAPIPermParser(String pscoutStr)
	{
		this.mPScoutInput = new BufferedReader(new StringReader(pscoutStr));
	}
	/**
	 * It won't close the underlying stream.
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		
	}
	@Override
	protected void finalize() throws Throwable
	{
		close();
	}
	private boolean parsePermStart(String line)
	{
		Matcher m = mPermStartPat.matcher(line);
		if(!m.find() || m.start() != 0 || m.end() != line.length())
			return false;
		mPermName = m.group(1);
		return true;
	}
	private boolean parseNumCallers(String line)
	{
		Matcher m = mCallersPat.matcher(line);
		if(!m.find() || m.start() != 0 || m.end() != line.length())
			return false;
		return true;
	}
	private boolean parseAPIPerm(String line)
	{
		Matcher m = mApiPermPat.matcher(line);
		if(!m.find() || m.start() != 0 || m.end() != line.length())
			return false;
		String pkgClassName = m.group(1);
		{
			int idx = pkgClassName.lastIndexOf('.');
			if(idx >= 0)
			{
				mPackageName = pkgClassName.substring(0, idx);
				mClassName = pkgClassName.substring(idx + 1);
			}
			else
			{
				mPackageName = "";
				mClassName = pkgClassName;
			}
		}
		mRetType = m.group(2);
		mMethodName = m.group(3);
		String argTypesStr = m.group(4);
		String[] tmpArgTypes = argTypesStr.split(",");
		if(tmpArgTypes.length == 1 && PScoutUtils.isWhitespaceString(tmpArgTypes[0]))
		{
			mArgsTypes = new String[0];
			return true;
		}
		else
			mArgsTypes = new String[tmpArgTypes.length];
		for(int i = 0; i < tmpArgTypes.length; ++i)
		{
			mArgsTypes[i] = tmpArgTypes[i].trim();
			if(mArgsTypes[i].length() == 0)
				return false;
		}
		return true;
	}
	private PScoutAPIPerm emit()
	{
		assert mPermName != null && mPackageName != null && mClassName != null && mRetType != null && mMethodName != null && mArgsTypes != null;
		PScoutAPIPerm apiPerm = new PScoutAPIPerm();
		apiPerm.setPermission(mPermName);
		apiPerm.setPackageName(mPackageName);
		apiPerm.setClassName(mClassName);
		apiPerm.setReturnType(mRetType);
		apiPerm.setMethodName(mMethodName);
		apiPerm.setArgumentTypes(mArgsTypes);
		return apiPerm;
	}
	public PScoutAPIPerm read() throws IOException, PScoutFormatException
	{
		do
		{
			String line = mPScoutInput.readLine();
			if(line == null)
				return null;
			if(PScoutUtils.isWhitespaceString(line))
				continue;
			switch(mState)
			{
			case STATE_API_PERM:
				if(parseAPIPerm(line))
					return emit();
				// Fall through
			case STATE_PERM:
				if(parsePermStart(line))
				{
					mState = STATE_NUM_CALLERS;
					continue;
				}
				break;
			case STATE_NUM_CALLERS:
				if(parseNumCallers(line))
				{
					mState = STATE_API_PERM;
					continue;
				}
				break;
			}
			throw new PScoutFormatException();				
		}while(true);
	}
}
