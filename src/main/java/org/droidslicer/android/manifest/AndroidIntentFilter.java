package org.droidslicer.android.manifest;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

public class AndroidIntentFilter
{
	private final Set<String> mActNames = new HashSet<String>();
	private final Set<String> mCategoryNames = new HashSet<String>();
	private final Set<String> mDataSchemes = new HashSet<String>();
	private final Set<String> mDataHosts = new HashSet<String>();
	private final MutableIntSet mDataPorts = MutableSparseIntSet.makeEmpty();
	private final Set<String> mDataPaths = new HashSet<String>();
	private final Set<String> mDataPathPatterns = new HashSet<String>();
	private final Set<String> mDataPathPrefixs = new HashSet<String>();
	private final Set<String> mDataMimeTypes = new HashSet<String>();
	public AndroidIntentFilter()
	{}
	public void addActionName(String name)
	{
		mActNames.add(name);
	}
	public Iterator<String> actionNamesIterator()
	{
		return mActNames.iterator();
	}
	public void addCategoryName(String name)
	{
		mCategoryNames.add(name);
	}
	public Iterator<String> categoryNamesIterator()
	{
		return mCategoryNames.iterator();
	}
	public void addDataScheme(String scheme)
	{
		mDataSchemes.add(scheme);
	}
	public Iterator<String> dataSchemesIterator()
	{
		return mDataSchemes.iterator();
	}
	public void addDataHost(String host)
	{
		mDataHosts.add(host);
	}
	public Iterator<String> dataHostsIterator()
	{
		return mDataHosts.iterator();
	}
	public void addDataPort(int port)
	{
		mDataPorts.add(port);
	}
	public IntIterator dataPortsIterator()
	{
		return mDataPorts.intIterator();
	}
	public void addDataPath(String path)
	{
		mDataPaths.add(path);
	}
	public Iterator<String> dataPathsIterator()
	{
		return mDataPaths.iterator();
	}
	public void addDataPathPattern(String pattern)
	{
		mDataPathPatterns.add(pattern);
	}
	public Iterator<String> dataPathPatternsIterator()
	{
		return mDataPathPatterns.iterator();
	}
	public void addDataPathPrefix(String prefix)
	{
		mDataPathPrefixs.add(prefix);
	}
	public Iterator<String> dataPathPrefixsIterator()
	{
		return mDataPathPrefixs.iterator();
	}
	public void addDataMimeType(String mime)
	{
		mDataMimeTypes.add(mime);
	}
	public Iterator<String> dataMimeTypesIterator()
	{
		return mDataMimeTypes.iterator();
	}
}
