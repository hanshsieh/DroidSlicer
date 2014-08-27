package org.droidslicer.android.manifest;

import com.ibm.wala.types.TypeReference;

public abstract class AndroidEntryComponent
{
	private final TypeReference mType;
	public AndroidEntryComponent(TypeReference type)
	{
		mType = type;
	}
	public TypeReference getType()
	{
		return mType;
	}
}
