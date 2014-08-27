package org.droidslicer.graph.entity;

import org.droidslicer.value.ConcreteValue;

public class SysIntentCommUnit extends IntentCommUnit
{
	private ConcreteValue mIntentVal;
	public SysIntentCommUnit(Class<? extends AppComponentUnit> targetEntityType)
	{
		super(targetEntityType);
	}

	public void setIntentValue(ConcreteValue intentVal)
	{
		mIntentVal = intentVal;
	}
	@Override
	public ConcreteValue getIntentValue()
	{
		return mIntentVal;
	}

}
