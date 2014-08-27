package org.droidslicer.graph.entity;

import com.ibm.wala.types.MethodReference;

public abstract class ICCReturnCalleeUnit extends ICCUnit
{
	private final MethodReference mMethodRef;
	public ICCReturnCalleeUnit(MethodReference methodRef)
	{
		if(methodRef == null)
			throw new IllegalArgumentException();
		mMethodRef = methodRef;
	}
	public MethodReference getMethod()
	{
		return mMethodRef;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(ICCReturnCalleeUnit.class.getSimpleName());
		builder.append(" method=[");
		builder.append(getMethod());
		builder.append("]]");
		return builder.toString();
	}
	@Override
	public boolean equals(Object other)
	{
		return this == other;
	}
	@Override
	public int hashCode()
	{
		return super.hashCode();
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitICCReturnCalleeUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
