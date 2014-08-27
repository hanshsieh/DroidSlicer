package org.droidslicer.value;


public class InetAddressLoopbackValue extends InetAddressValue
{
	public InetAddressLoopbackValue()
	{
		super(new ConstantStringValue("127.0.0.1"));
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		return other instanceof InetAddressLoopbackValue;
	}
	@Override
	public int hashCode()
	{
		return 404017;
	}
	@Override
	public String toString()
	{
		return "[LOOP_BACK_ADDR]";
	}
}
