package org.droidslicer.ifds;

import java.util.Arrays;
import java.util.HashSet;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;

public class CallRecords
{
	public static class CallSite
	{
		private final int mNodeId;
		private final int mInstIdx;
		public CallSite(int nodeId, int instIdx)
		{
			mNodeId = nodeId;
			mInstIdx = instIdx;
		}
		@Override
		public int hashCode()
		{
			return (mInstIdx << 16) + mNodeId;
		}
		@Override
		public boolean equals(Object other)
		{
			if(this == other)
				return true;
			if(!(other instanceof CallSite))
				return false;
			CallSite that = (CallSite)other;
			return mNodeId == that.mNodeId && mInstIdx == that.mInstIdx;
		}
	}
	public class CallSites extends HashSet<CallSite>
	{
		private static final long serialVersionUID = 4932442470033280559L;
		public CallSites()
		{
			super(8);
		}
	}
	private CallSites[] mRecords;
	private final CallGraph mCg;
	public CallRecords(CallGraph cg)
	{
		if(cg == null)
			throw new IllegalArgumentException();
		mRecords = new CallSites[0];
		mCg = cg;
	}
	public CallRecords(CallRecords other)
	{
		if(other == null)
			throw new IllegalArgumentException();
		mRecords = new CallSites[other.mRecords.length];
		for(int i = 0; i < other.mRecords.length; ++i)
		{
			if(other.mRecords[i] == null)
				mRecords[i] = null;
			else
			{
				mRecords[i] = new CallSites();
				mRecords[i].addAll(other.mRecords[i]);
			}
		}
		mCg = other.mCg;
	}
	public void clear()
	{
		mRecords = new CallSites[0];
	}
	public boolean addAllCalls(CallRecords that)
	{
		if(this == that)
			return false;
		if(that.mRecords.length > mRecords.length)
		{
			mRecords = Arrays.copyOf(mRecords, that.mRecords.length);
		}
		boolean changed = false;
		for(int i = 0; i < that.mRecords.length; ++i)
		{
			CallSites oCallSites = that.mRecords[i];
			if(oCallSites == null)
				continue;
			if(mRecords[i] == null)
			{
				mRecords[i] = new CallSites();
				mRecords[i].addAll(oCallSites);
				changed = true;
			}
			else if(mRecords[i].addAll(oCallSites))
				changed = true;
		}
		return changed;
	}
	public boolean addCall(CGNode callerNode, int instIdx, CGNode callee)
	{
		int calleeId = callee.getGraphNodeId();
		if(calleeId < 0)
			throw new IllegalArgumentException();
		if(mRecords.length <= calleeId)
			mRecords = Arrays.copyOf(mRecords, Math.max(calleeId + 1, mRecords.length * 2));
		CallSites sites = mRecords[calleeId];
		if(sites == null)
		{
			sites = new CallSites();
			mRecords[calleeId] = sites;
		}
		assert callerNode.getGraphNodeId() >= 0;
		return sites.add(new CallSite(callerNode.getGraphNodeId(), instIdx));
	}
	public CallSites getCallSitesTo(CGNode node)
	{
		int nodeId = node.getGraphNodeId();
		if(nodeId < 0)
			throw new IllegalArgumentException();
		if(nodeId >= mRecords.length)
			return null;
		return mRecords[nodeId];
	}
}
