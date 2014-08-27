package org.droidslicer.analysis;

import com.ibm.wala.types.FieldReference;

public class FieldSpec
{
	private final FieldReference mFieldRef;
	private final int mAccessFlags;
	public FieldSpec(FieldReference ref, int accessFlags)
	{
		if(ref == null)
			throw new IllegalArgumentException("null");
		mFieldRef = ref;
		mAccessFlags = accessFlags;
	}
	public FieldReference getFieldReference()
	{
		return mFieldRef;
	}
	public int getAccessFlags()
	{
		return mAccessFlags;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof FieldSpec))
			return false;
		FieldSpec that = (FieldSpec)other;
		return mFieldRef.equals(that.mFieldRef) && mAccessFlags == that.mAccessFlags;
	}
	@Override
	public int hashCode()
	{
		return mFieldRef.hashCode() * 8179 + mAccessFlags;
	}
}
