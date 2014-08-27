package org.droidslicer.analysis;

import java.util.Iterator;

import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.graph.NumberedNodeManager;

public interface LazyNumberedNodeManager<T> extends NumberedNodeManager<T>
{
	public Iterator<? extends Statement> iterateLazyNodes();
	public int getNumberOfLazyNodes();
}
