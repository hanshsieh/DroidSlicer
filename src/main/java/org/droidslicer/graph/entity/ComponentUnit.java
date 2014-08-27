package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;

public abstract class ComponentUnit extends UnitEntity
{
	private final TypeReference mType;
	private final Map<Selector, Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit>> mEntryMethods = 
			new HashMap<Selector, Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit>>();
	private boolean mIsSystem = false;
	public ComponentUnit(TypeReference type)
	{
		if(type == null)
			throw new IllegalArgumentException();
		mType = type;
	}
	public void setSystemComponent(boolean val)
	{
		mIsSystem = val;
	}
	public boolean isSystemComponent()
	{
		return mIsSystem;
	}
	public void addEntryMethod(ICCParamCalleeUnit param, ICCReturnCalleeUnit ret)
	{
		MethodReference paramMethodRef = param.getMethod();
		MethodReference retMethodRef = ret.getMethod();
		if(!paramMethodRef.equals(retMethodRef))
		{
			throw new IllegalArgumentException(
					"The given method parameter and method return doesn't correspond to " +
					"a same method");
		}
		if(!paramMethodRef.getDeclaringClass().equals(mType))
		{
			throw new IllegalArgumentException(
					"The given method parameter and method return doesn't have a same declaring class " +
					"as this component");
		}
		mEntryMethods.put(paramMethodRef.getSelector(), Pair.of(param, ret));
	}
	public Collection<Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit>> getEntryMethods()
	{
		return mEntryMethods.values();
	}
	public Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit> getEntryMethod(Selector selec)
	{
		return mEntryMethods.get(selec);
	}
	public TypeReference getType()
	{
		return mType;
	}
	/**
	 * Even if two {@link ComponentUnit}'s have same content, they may represent different
	 * concrete instances.
	 */
	@Override
	public final boolean equals(Object other)
	{
		return this == other;
	}
	@Override
	public final int hashCode()
	{
		return super.hashCode();
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(getClass().getSimpleName());
		builder.append(" type=[");
		builder.append(mType);
		builder.append("], isSystem=");
		builder.append(mIsSystem);
		builder.append(']');
		return builder.toString();
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitComponentUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
