package org.droidslicer.value;


public class InetAddressLocalHostValue extends InetAddressValue
{
	public InetAddressLocalHostValue()
	{
		super(new ConstantStringValue("127.0.0.1"));
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		return other instanceof InetAddressLocalHostValue;
	}
	@Override
	public int hashCode()
	{
		return 565937;
	}
	@Override
	public String toString()
	{
		return "[LOCAL_HOST_ADDR]";
	}
}
