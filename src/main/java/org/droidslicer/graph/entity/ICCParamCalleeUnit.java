package org.droidslicer.graph.entity;

import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

public abstract class ICCParamCalleeUnit extends ICCUnit
{
	private final MethodReference mMethodRef;
	
	/**
	 * Notice that the declaring class of the method reference should be the 
	 * component class of the method, not the declaring class of the method.
	 * For example, if class B and C extends A, and B and C are registered as 
	 * entry component, and there's a entry method only declared in A but not in
	 * B or C, then there should be two {@link ICCParamCalleeUnit} for the method
	 * , one for the entry method of B, and one for the entry method of C, and the declaring
	 * class of the two {@link ICCParamCalleeUnit} should be B and C, respectively.
	 * @param methodRef
	 */
	public ICCParamCalleeUnit(MethodReference methodRef)
	{
		if(methodRef == null)
			throw new IllegalArgumentException();
		mMethodRef = methodRef;
	}
	public TypeReference getType()
	{
		return mMethodRef.getDeclaringClass();
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
		builder.append(ICCParamCalleeUnit.class.getSimpleName());
		builder.append(" method=[");
		builder.append(getMethod());
		builder.append("]]");
		return builder.toString();
	}
	
	/**
	 * Even if the method references of two {@link ICCParamCalleeUnit}'s are the same, they may represent
	 * different concrete instances.
	 */
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
		if(visitor.visitICCParamCalleeUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
