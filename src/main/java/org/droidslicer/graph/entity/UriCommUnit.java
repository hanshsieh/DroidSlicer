package org.droidslicer.graph.entity;

import org.droidslicer.value.ConcreteValue;

import com.ibm.wala.types.MethodReference;

public abstract class UriCommUnit extends ICCParamCallerUnit
{
	private final MethodReference mTargetMethodRef;
	/**
	 * 
	 * @param targetEntityType type of the target component, e.g. android.app.Activity
	 * @param node
	 * @param instIdx
	 * @param intentParamIdx index of the Intent parameter (not including the implicit 'this')
	 */
	public UriCommUnit(MethodReference targetMethodRef)
	{
		if(targetMethodRef == null)
			throw new IllegalArgumentException();
		mTargetMethodRef = targetMethodRef;
	}

	public MethodReference getTargetMethod()
	{
		return mTargetMethodRef;
	}
	public abstract ConcreteValue getUriValue();
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(UriCommUnit.class.getSimpleName());
		builder.append(" target=");
		builder.append(mTargetMethodRef.getName());
		builder.append(", uri=");
		builder.append(getUriValue());
		builder.append(']');
		return builder.toString();
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitUriCommUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
