package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;

public class CICCReturnCallerUnit extends ICCReturnCallerUnit implements IInstructionUnit, IStatementFlowUnit
{
	private final CGNode mNode;
	private final int mInstIdx;
	private final Set<Statement> mOutflows = new HashSet<Statement>();
	public CICCReturnCallerUnit(CGNode node, int instIdx)
	{
		mNode = node;
		mInstIdx = instIdx;
	}
	@Override
	public boolean isAllowFlowThrough()
	{
		return false;
	}

	@Override
	public Collection<Statement> getInflowStatements()
	{
		return Collections.emptySet();
	}

	public void addOutflowStatement(Statement stm)
	{
		mOutflows.add(stm);
	}
	@Override
	public Collection<Statement> getOutflowStatements()
	{
		return mOutflows;
	}

	@Override
	public CGNode getNode()
	{
		return mNode;
	}

	@Override
	public int getInstructionIndex()
	{
		return mInstIdx;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(ICCReturnCallerUnit.class.getSimpleName());
		builder.append(" node=[");
		builder.append(mNode);
		builder.append(']');
		IR ir = mNode.getIR();
		if(ir != null)
		{
			builder.append(", inst=[");
			builder.append(ir.getInstructions()[mInstIdx]);
			builder.append(']');
		}
		builder.append(']');
		return builder.toString();
	}
}
