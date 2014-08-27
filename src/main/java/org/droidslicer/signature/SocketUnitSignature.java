package org.droidslicer.signature;

import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.SocketUnit;
import org.droidslicer.graph.entity.UnitEntity;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.InetSocketAddressValue;
import org.droidslicer.value.SocketValue;

public class SocketUnitSignature extends UnitSignature 
{
	private final ConcreteValue mAddr;
	private final ConcreteValue mPort;
	private int mHash = -1;
	public SocketUnitSignature(Class<? extends ComponentUnit> comp, Boolean isSystem, ConcreteValue addr, ConcreteValue port)
	{
		super(comp, isSystem);
		if(addr == null || port == null)
			throw new IllegalArgumentException();
		mAddr = addr;
		mPort = port;
	}
	private boolean checkedProperties(ConcreteValue oSockVal)
	{
		return oSockVal == null || SocketValue.isPossibleMatch(new SocketValue(new InetSocketAddressValue(mAddr, mPort), false), oSockVal);
	}
	@Override
	public MatchType isBasicMatched(UnitEntity unit)
	{
		if(!(unit instanceof SocketUnit))
			return MatchType.NOT_MATCHED;
		SocketUnit sockUnit = (SocketUnit)unit;
		ConcreteValue oSockVal = sockUnit.getSocketValue();
		MatchType matched = super.isBasicMatched(unit);
		switch(matched)
		{
		case NOT_MATCHED:
			return MatchType.NOT_MATCHED;
		case MATCHED:
			if(checkedProperties(oSockVal))
				return MatchType.MATCHED;
			else
				return MatchType.NOT_MATCHED;
		default:
			if(checkedProperties(oSockVal))
				return MatchType.POSSIBLE_MATCHED;
			else
				return MatchType.NOT_MATCHED;
		}
	}
	@Override
	public boolean equals(Object other)
	{
		if(!super.equals(other))
			return false;
		if(!(other instanceof SocketUnitSignature))
			return false;
		SocketUnitSignature that = (SocketUnitSignature)other;
		return mAddr.equals(that.mAddr) && mPort.equals(that.mPort);
	}
	@Override
	public int hashCode()
	{
		if(mHash == -1)
		{
			mHash = mAddr.hashCode() + mPort.hashCode();
			if(mHash == -1)
				mHash = 66889;
		}
		return super.hashCode() + mHash;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("comp=[");
		builder.append(getComponentType());
		builder.append("], isSystem=");
		builder.append(isSystemComponent());
		builder.append(", isSocket=\"true\"");
		return builder.toString();
	}
}
