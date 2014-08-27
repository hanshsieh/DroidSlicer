package org.droidslicer.android.manifest;

public class AndroidManifestSyntaxException extends Exception
{
	private static final long serialVersionUID = -8537927252588869819L;
	public AndroidManifestSyntaxException()
	{}
	public AndroidManifestSyntaxException(String msg)
	{
		super(msg);
	}
	public AndroidManifestSyntaxException(Throwable throwable)
	{
		super(throwable);
	}
}
