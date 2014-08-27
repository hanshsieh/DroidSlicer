package org.droidslicer.value;

import com.ibm.wala.types.TypeReference;

public abstract class ConcreteValue 
{
	public abstract ConcreteValue getStringValue();
	public ConcreteValue simplify()
	{
		return this;
	}
	public static ConcreteValue fromConstantValue(Object constVal)
	{
		if(constVal == null)
			return NullValue.getInstance();
		else if(constVal instanceof String)
			return new ConstantStringValue((String)constVal);
		else if(constVal instanceof Integer)
			return new IntValue((Integer)constVal);
		else if(constVal instanceof Float)
		{
			return new FloatValue((Float)constVal);
		}
		else if(constVal instanceof Double)
		{
			return new DoubleValue((Float)constVal);
		}
		else if(constVal instanceof Long)
		{
			return new LongValue((Long)constVal);
		}
		else if(constVal instanceof Character)
		{
			return new CharValue((Character)constVal);
		}
		else if(constVal instanceof TypeReference)
		{
			return ClassObjValue.make(((TypeReference)constVal).getName());
		}
		else
			return UnknownValue.getInstance();
	}
}
