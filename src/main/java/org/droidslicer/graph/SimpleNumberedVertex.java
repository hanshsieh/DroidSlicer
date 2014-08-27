package org.droidslicer.graph;

import com.ibm.wala.util.graph.impl.NodeWithNumber;

public class SimpleNumberedVertex extends NodeWithNumber
{
	private Object mInfo;
	public void setInfo(Object info)
	{
		mInfo = info;
	}
	public Object getInfo()
	{
		return mInfo;
	}
}
