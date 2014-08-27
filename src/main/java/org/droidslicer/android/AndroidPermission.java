package org.droidslicer.android;

public enum AndroidPermission
{
	INTERNET("android.permission.INTERNET");
	private final String mVal;
	private AndroidPermission(String val)
	{
		mVal = val;
	}
	public String getValue()
	{
		return mVal;
	}
}
