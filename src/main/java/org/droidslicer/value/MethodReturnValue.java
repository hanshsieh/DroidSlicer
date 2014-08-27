package org.droidslicer.value;

import java.util.Arrays;

import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.StringStuff;

public class MethodReturnValue extends ConcreteValue 
{
	private final MethodReference mMethodRef;
	private final ConcreteValue[] mParamVals;
	private final boolean mIsStatic;
	public MethodReturnValue(MethodReference methodRef, boolean isStatic)
	{
		if(methodRef == null)
			throw new IllegalArgumentException();
		mMethodRef = methodRef;
		mIsStatic = isStatic;
		int nParam = isStatic ? methodRef.getNumberOfParameters() : methodRef.getNumberOfParameters() + 1;
		mParamVals = new ConcreteValue[nParam];
	}
	public boolean isStatic()
	{
		return mIsStatic;
	}
	/**
	 * For non-static method, index 0 corresponds to the 'this' parameter.
	 * @param idx
	 * @param value
	 */
	public void setParamValue(int idx, ConcreteValue value)
	{
		if(idx < 0 || idx >= mParamVals.length)
			throw new IllegalArgumentException("Illegal index");
		if(value == null)
			throw new IllegalArgumentException("null");
		mParamVals[idx] = value;
	}
	public void addPossibleParamValue(int idx, ConcreteValue value)
	{
		if(idx < 0 || idx >= mParamVals.length)
			throw new IllegalArgumentException("Illegal index");
		if(value == null)
			throw new IllegalArgumentException("null");
		if(mParamVals[idx] == null)
			mParamVals[idx] = value;
		else
			mParamVals[idx] = new OrValue(mParamVals[idx], value);
	}	
	public ConcreteValue getParamValue(int idx)
	{
		if(idx < 0 || idx >= mParamVals.length)
			throw new IllegalArgumentException("Illegal index");
		return mParamVals[idx];
	}
	public MethodReference getMethod()
	{
		return mMethodRef;
	}
	@Override
	public int hashCode()
	{
		int hash = mMethodRef.hashCode();
		for(int i = 0; i < mParamVals.length; ++i)
		{
			hash = hash * 31 + (mParamVals[i] != null ? mParamVals[i].hashCode() : 0);
		}
		return hash;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof MethodReturnValue))
			return false;
		MethodReturnValue that = (MethodReturnValue)other;
		if(!mMethodRef.equals(that.mMethodRef))
			return false;
		return Arrays.equals(mParamVals, that.mParamVals);
	}
	@Override
	public String toString()
	{
		TypeName clazzName = mMethodRef.getDeclaringClass().getName();
		StringBuilder builder = new StringBuilder();
		builder.append("[METHOD ");
		builder.append(StringStuff.jvmToReadableType(clazzName.toString()));
		builder.append(':');
		if(mIsStatic)
			builder.append("static ");
		builder.append(StringStuff.jvmToReadableType(mMethodRef.getReturnType().getName().toString()));
		builder.append(' ');
		builder.append(mMethodRef.getName());
		builder.append('(');
		int nParam = mMethodRef.getNumberOfParameters();
		if(!mIsStatic)
			nParam++;
		for(int i = 0; i < nParam; ++i)
		{
			if(i != 0)
				builder.append(", ");
			TypeReference paramTypeRef;
			if(!mIsStatic)
			{
				if(i == 0)
					paramTypeRef = mMethodRef.getDeclaringClass();
				else
					paramTypeRef = mMethodRef.getParameterType(i - 1);
			}
			else
			{
				paramTypeRef = mMethodRef.getParameterType(i);
			}
			builder.append(StringStuff.jvmToReadableType(paramTypeRef.getName().toString()));
			if(i == 0 && !mIsStatic)
				builder.append("(this)");
			builder.append("(value=");
			ConcreteValue paramVal = mParamVals[i];
			builder.append(paramVal == null ? UnknownValue.getInstance() : paramVal);
			builder.append(")");
		}
		builder.append(')');
		builder.append(']');
		return builder.toString();
	}
	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Maybe we should do better
		return this;
	}
}
