package org.droidslicer.config;

public class ConfigurationSyntaxException extends Exception
{
	private static final long serialVersionUID = -6916496118132372378L;
	public ConfigurationSyntaxException()
	{}
	public ConfigurationSyntaxException(String msg)
	{
		super(msg);
	}
	public ConfigurationSyntaxException(Throwable throwable)
	{
		super(throwable);
	}
}
