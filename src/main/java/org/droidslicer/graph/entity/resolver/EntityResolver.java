package org.droidslicer.graph.entity.resolver;

public class EntityResolver
{
	public static final int DEFAULT_RESOLVE_DEPTH = 10;
	private int mResolveDepth = DEFAULT_RESOLVE_DEPTH;
	public int getResolveDepth()
	{
		return mResolveDepth;
	}
	public void setResolveDepth(int depth)
	{
		mResolveDepth = depth;
	}
}
