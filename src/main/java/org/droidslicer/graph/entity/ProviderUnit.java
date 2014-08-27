package org.droidslicer.graph.entity;

import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.NullValue;

import com.ibm.wala.types.TypeReference;

public class ProviderUnit extends AppComponentUnit
{
	private ConcreteValue mAuthorityVal = NullValue.getInstance();
	
	// The path requirement of the querying URI
	// It there's no requirement of the path, use {@link NullValue}.
	// The path include the leading '/'
	private ConcreteValue mPathVal = NullValue.getInstance();
	public ProviderUnit(TypeReference type)
	{
		super(type);
	}
	public ConcreteValue getAuthorityValue()
	{
		return mAuthorityVal;
	}
	public void setAuthorityValue(ConcreteValue val)
	{
		if(val == null)
			throw new IllegalArgumentException();
		mAuthorityVal = val;
	}
	public ConcreteValue getPathValue()
	{
		return mPathVal;
	}
	public void setPathValue(ConcreteValue val)
	{
		if(val == null)
			throw new IllegalArgumentException();
		mPathVal = val;
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitProviderUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(ProviderUnit.class.getSimpleName());
		builder.append(" type=[");
		builder.append(getType());
		builder.append("], isSystem=");
		builder.append(isSystemComponent());
		builder.append(", authorities=");
		builder.append(mAuthorityVal);
		builder.append(", path=");
		builder.append(mPathVal);
		builder.append(']');
		return builder.toString();
	}
}
