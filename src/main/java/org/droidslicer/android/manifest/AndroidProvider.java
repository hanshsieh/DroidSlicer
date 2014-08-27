package org.droidslicer.android.manifest;

import java.util.Collection;
import java.util.HashSet;

import com.ibm.wala.types.TypeReference;

public class AndroidProvider extends AndroidAppComponent
{
	private final Collection<String> mAuths = new HashSet<String>();
	public AndroidProvider(TypeReference typeRef)
	{
		super(typeRef);
	}
	public Collection<String> getAuthorities()
	{
		return mAuths;
	}
	public void addAuthority(String auth)
	{
		mAuths.add(auth);
	}
}
