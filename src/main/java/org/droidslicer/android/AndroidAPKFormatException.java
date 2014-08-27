package org.droidslicer.android;

public class AndroidAPKFormatException extends Exception
{
	private static final long serialVersionUID = 3208991213918797868L;
	public AndroidAPKFormatException()
	{}
	public AndroidAPKFormatException(String msg)
	{
		super(msg);
	}
	public AndroidAPKFormatException(Throwable throwable)
	{
		super(throwable);
	}
}
