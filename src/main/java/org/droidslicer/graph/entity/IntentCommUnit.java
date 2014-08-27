package org.droidslicer.graph.entity;

import org.droidslicer.value.ConcreteValue;

public abstract class IntentCommUnit extends ICCParamCallerUnit
{
	private final Class<? extends AppComponentUnit> mTargetEntityType;
	/**
	 * 
	 * @param targetEntityType type of the target component, e.g. android.app.Activity
	 * @param node
	 * @param instIdx
	 * @param intentParamIdx index of the Intent parameter (not including the implicit 'this')
	 */
	public IntentCommUnit(Class<? extends AppComponentUnit> targetEntityType)
	{
		if(targetEntityType == null)
			throw new IllegalArgumentException();
		mTargetEntityType = targetEntityType;
	}
	public abstract ConcreteValue getIntentValue();
	/**
	 * Get the type of the target component, e.g. android.app.Activity.
	 * @return
	 */
	public Class<? extends AppComponentUnit> getTargetEntityType()
	{
		return mTargetEntityType;
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
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(IntentCommUnit.class.getSimpleName());
		builder.append(" target=");
		builder.append(mTargetEntityType.getSimpleName());
		builder.append(", intent=");
		builder.append(getIntentValue());
		builder.append(']');
		return builder.toString();
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitIntentCommUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
