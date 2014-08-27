package org.droidslicer.graph.entity;

import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.OrValue;

import com.ibm.wala.types.TypeReference;

public class ServiceUnit extends AppComponentUnit implements IntentFilterUnit
{
	private final OrValue mIntentFilterVals = new OrValue();
	public ServiceUnit(TypeReference type)
	{
		super(type);
	}
	@Override
	public void addIntentFilterValue(ConcreteValue val)
	{
		mIntentFilterVals.addValue(val);
	}
	@Override
	public OrValue getIntentFilterValues()
	{
		return mIntentFilterVals;
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitServiceUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(ServiceUnit.class.getSimpleName());
		builder.append(" type=[");
		builder.append(getType());
		builder.append("], isSystem=");
		builder.append(isSystemComponent());
		builder.append(", intentFilter=");
		builder.append(mIntentFilterVals);
		builder.append(']');
		return builder.toString();
	}
}
