package org.droidslicer.value;

import com.ibm.wala.types.TypeName;

/**
 * It represents a java.lang.Class object.
 * @author someone
 *
 */
public class ClassObjValue extends ConcreteValue
{
	private final TypeName mClassName;
	private ClassObjValue(TypeName className)
	{
		if(className == null)
			throw new IllegalArgumentException("Class name cannot be null");
		mClassName = className;
	}
	public static ClassObjValue make(TypeName name)
	{
		return new ClassObjValue(name);
	}
	public TypeName getClassName()
	{
		return mClassName;
	}
	@Override
	public int hashCode()
	{
		return mClassName.hashCode();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof ClassObjValue))
			return false;
		ClassObjValue that = (ClassObjValue)other;
		return mClassName.equals(that.mClassName);
	}
	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Maybe we should do better
		return this;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(mClassName.toString());
		builder.append(']');
		return builder.toString();
	}
}
