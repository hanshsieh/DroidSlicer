package org.droidslicer.graph.entity;

import com.ibm.wala.ipa.callgraph.CGNode;

public interface IInstructionUnit
{
	public CGNode getNode();
	public int getInstructionIndex();
}
