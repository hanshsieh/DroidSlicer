package org.droidslicer.graph.entity;

import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.OrValue;

public interface IntentFilterUnit
{
	public void addIntentFilterValue(ConcreteValue val);
	public OrValue getIntentFilterValues();
}
