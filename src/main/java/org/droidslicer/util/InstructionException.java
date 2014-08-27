package org.droidslicer.util;

public class InstructionException extends RuntimeException
{
	private static final long serialVersionUID = 1062125528554156961L;
	public InstructionException(String msg)
	{
		super(msg);
	}
	public InstructionException()
	{
		super();
	}
	public InstructionException(Throwable throwable)
	{
		super(throwable);
	}
}
