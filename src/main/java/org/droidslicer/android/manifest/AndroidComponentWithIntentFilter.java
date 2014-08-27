package org.droidslicer.android.manifest;

import java.util.Iterator;

public interface AndroidComponentWithIntentFilter
{
	public void addIntentFilter(AndroidIntentFilter filter);
	public Iterator<AndroidIntentFilter> intentFilterIterator();
}
