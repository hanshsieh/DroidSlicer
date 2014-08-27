package org.droidslicer.value;

import java.util.Iterator;

import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.strings.StringStuff;

public class ComponentNameValue extends ConcreteValue
{
	private final ConcreteValue mClassVal;
	public ComponentNameValue(ConcreteValue classVal)
	{
		mClassVal = classVal;
	}
	public ConcreteValue getClassValue()
	{
		return mClassVal;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Maybe we should do better
		return this;
	}
	protected static boolean isClassNamePossibleMatch(ConcreteValue classVal, TypeName typeName, ConstantStringValue typeNameStrVal)
	{
		Iterator<ConcreteValue> classItr = OrValue.getSingleValueIterator(classVal);
		while(classItr.hasNext())
		{
			ConcreteValue classValSingle = classItr.next();
			if(classValSingle instanceof ClassObjValue)
			{
				ClassObjValue classObjVal = (ClassObjValue)classValSingle;
				if(typeName.equals(classObjVal.getClassName()))
					return true;
			}
			else if(classValSingle instanceof UnknownValue)
			{
				// TODO To reduce false-positive, we use conservative approach
				continue;
			}
			else
			{
				if(ConstantStringValue.isPossibleMatched(typeNameStrVal, classValSingle))
					return true;
			}
		}
		return false;
	}
	public static boolean isPossibleTypeNameMatch(ConcreteValue compNameVal, TypeName typeName)
	{
		ConstantStringValue typeNameStrVal = new ConstantStringValue(StringStuff.jvmToBinaryName(typeName.toString()));
		Iterator<ConcreteValue> compNameItr = OrValue.getSingleValueIterator(compNameVal);
		while(compNameItr.hasNext())
		{
			ConcreteValue compNameValSingle = compNameItr.next();
			if(compNameValSingle instanceof UnknownValue)
			{
				// TODO To reduce false-positive, we use conservative approach
				continue;
			}
			else if(compNameValSingle instanceof ComponentNameValue)
			{
				ComponentNameValue compName = (ComponentNameValue)compNameValSingle;
				if(isClassNamePossibleMatch(compName.getClassValue(), typeName, typeNameStrVal))
					return true;
			}
		}
		return false;
	}
	@Override
	public int hashCode()
	{
		return mClassVal.hashCode() * 1069;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof ComponentNameValue))
			return false;
		ComponentNameValue that = (ComponentNameValue)other;
		return mClassVal.equals(that.mClassVal);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[COMP_NAME class=");
		builder.append(mClassVal);
		builder.append(']');
		return builder.toString();
	}
}
