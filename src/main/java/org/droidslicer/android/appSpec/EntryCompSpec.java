package org.droidslicer.android.appSpec;

import java.util.HashMap;
import java.util.Iterator;

import com.google.common.collect.Iterators;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;

public abstract class EntryCompSpec
{
	private final TypeReference mClassType;
	private final HashMap<Selector, EntryMethodSpec> mEntryMethods = new HashMap<Selector, EntryMethodSpec>();
	public EntryCompSpec(TypeReference clazz)
	{
		if(clazz == null)
			throw new IllegalArgumentException();
		mClassType = clazz;
	}
	public void addEntryMethod(EntryMethodSpec entryMethod)
	{
		if(entryMethod == null)
			throw new IllegalArgumentException();
		MethodReference methodRef = entryMethod.getMethod();
		if(!methodRef.getDeclaringClass().equals(mClassType))
			throw new IllegalArgumentException("The declaring class type of the method isn't equal to the class type of this class");
		mEntryMethods.put(methodRef.getSelector(), entryMethod);
	}
	public Iterator<EntryMethodSpec> entryMethodsIterator()
	{
		return Iterators.unmodifiableIterator(mEntryMethods.values().iterator());
	}
	public EntryMethodSpec getMethodSpec(Selector selector)
	{
		return mEntryMethods.get(selector);
	}
	public TypeReference getClassType()
	{
		return mClassType;
	}
	@Override
	public int hashCode()
	{
		return mClassType.hashCode();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof EntryCompSpec))
			return false;
		EntryCompSpec that = (EntryCompSpec)other;
		return mClassType.equals(that.mClassType);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Type=[");
		builder.append(mClassType);
		builder.append(']');
		return builder.toString();
	}
}
