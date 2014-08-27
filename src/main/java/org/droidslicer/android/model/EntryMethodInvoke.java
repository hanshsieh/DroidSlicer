package org.droidslicer.android.model;

import java.util.HashSet;
import java.util.Set;

import org.droidslicer.android.appSpec.EntryCompSpec;
import org.droidslicer.android.appSpec.EntryMethodSpec;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.HeapStatement.HeapParamCallee;

public class EntryMethodInvoke
{
	private final EntryCompSpec mCompSpec;
	private final EntryMethodSpec mMethodSpec;
	private final CGNode mCalleeNode;
	private final Set<HeapParamCallee> mHeapFlows = new HashSet<HeapParamCallee>();
	public EntryMethodInvoke(EntryCompSpec compSpec, EntryMethodSpec methodSpec, CGNode calleeNode)
	{
		if(compSpec == null || methodSpec == null || calleeNode == null)
			throw new IllegalArgumentException();
		mCompSpec = compSpec;
		mMethodSpec = methodSpec;
		mCalleeNode = calleeNode;
	}
	public CGNode getCalleeNode()
	{
		return mCalleeNode;
	}
	public EntryCompSpec getEntryCompSpec()
	{
		return mCompSpec;
	}
	public EntryMethodSpec getEntryMethodSpec()
	{
		return mMethodSpec;
	}
	public void addHeapFlow(HeapParamCallee stm)
	{
		mHeapFlows.add(stm);
	}
	public Set<HeapParamCallee> getHeapFlows()
	{
		return mHeapFlows;
	}
	@Override
	public int hashCode()
	{
		return mMethodSpec.hashCode() * 31 + mCalleeNode.hashCode();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof EntryMethodInvoke))
			return false;
		EntryMethodInvoke that = (EntryMethodInvoke)other;
		return mMethodSpec.equals(that.mMethodSpec) &&
				mCalleeNode.equals(that.mCalleeNode);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("compSpec=[");
		builder.append(mCompSpec);
		builder.append("], methodSpec=[");
		builder.append(mMethodSpec);
		builder.append("], calleeNode=[");
		builder.append(mCalleeNode);
		builder.append(']');
		return builder.toString();
	}
}
