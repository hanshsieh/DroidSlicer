package org.droidslicer.ifds;

import com.ibm.wala.ipa.slicer.Statement;

public class CallEntry 
{
	private Statement mCallerStm, mEntryStm;
	public CallEntry(Statement caller, Statement entry)
	{
		if(caller == null)
			throw new IllegalArgumentException();
		mCallerStm = caller;
		mEntryStm = entry;
	}
	public Statement getCallerStatement()
	{
		return mCallerStm;
	}
	public Statement getEntryStatement()
	{
		return mEntryStm;
	}
	@Override
	public int hashCode()
	{
		int result = 0;
		result = result * 31 + mCallerStm.hashCode();
		result = result * 31 + (mEntryStm == null ? 0 : mEntryStm.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof CallEntry))
			return false;
		CallEntry that = (CallEntry)other;
		if(!mCallerStm.equals(that.mCallerStm))
			return false;
		if(mEntryStm == null)
		{
			if(that.mEntryStm != null)
				return false;
			else
				return true;
		}
		else
			return mEntryStm.equals(that.mEntryStm);
	}
}
