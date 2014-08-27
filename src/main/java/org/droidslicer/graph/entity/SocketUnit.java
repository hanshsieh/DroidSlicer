package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.Collections;

import org.droidslicer.android.AndroidPermission;
import org.droidslicer.value.ConcreteValue;

public class SocketUnit extends SUseUnit
{
	private ConcreteValue mSockVal = null;
	public void setSocketValue(ConcreteValue val)
	{
		mSockVal = val;
	}
	public ConcreteValue getSocketValue()
	{
		return mSockVal;
	}
	
	@Override
	public Collection<String> getPermissions()
	{
		return Collections.singleton(AndroidPermission.INTERNET.getValue());
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(SocketUnit.class.getSimpleName());
		builder.append(" value=");
		builder.append(mSockVal);
		builder.append(']');
		return builder.toString();
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitSocketUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
