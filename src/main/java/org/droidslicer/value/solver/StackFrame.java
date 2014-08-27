package org.droidslicer.value.solver;
import com.ibm.wala.ipa.callgraph.CGNode;

public class StackFrame
{
	private final int mInstIdx;
	private final CGNode mNode;
	public StackFrame(CGNode node, int instIdx)
	{
		if(node == null)
			throw new IllegalArgumentException();
		mNode = node;
		mInstIdx = instIdx;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof StackFrame))
			return false;
		StackFrame that = (StackFrame)other;
		return mInstIdx == that.mInstIdx && mNode.equals(that.mNode);
	}
	@Override
	public int hashCode()
	{
		return mNode.hashCode() * 31 + mInstIdx;
	}
	public int getInstructionIndex()
	{
		return mInstIdx;
	}
	public CGNode getNode()
	{
		return mNode;
	}
}