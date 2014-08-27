package org.droidslicer.config;

import com.google.common.base.Predicate;

public class FlowPredicate implements Predicate<Object>
{
	private final static int TRUE = 1;
	private final static int FALSE = 0;
	private int mResult = -1;
	@Override
	public boolean apply(Object arg)
	{
		return mResult == TRUE;
	}
	public boolean isResultSet()
	{
		return mResult == TRUE || mResult == FALSE;
	}
	public void setResult(boolean val)
	{
		mResult = val ? TRUE : FALSE;
	}
}
