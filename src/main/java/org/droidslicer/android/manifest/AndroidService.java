package org.droidslicer.android.manifest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.types.TypeReference;

public class AndroidService extends AndroidAppComponent implements AndroidComponentWithIntentFilter
{
	private final Collection<AndroidIntentFilter> mIntentFilters = new ArrayList<AndroidIntentFilter>();
	public AndroidService(TypeReference typeRef)
	{
		super(typeRef);
	}
	public void addIntentFilter(AndroidIntentFilter filter)
	{
		mIntentFilters.add(filter);
	}
	public Iterator<AndroidIntentFilter> intentFilterIterator()
	{
		return mIntentFilters.iterator();
	}
}
