package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;

public class UrlConnectionInputUnit extends UrlConnectionUnit 
	implements IInstructionUnit, IStatementFlowUnit, IMutableStatementOutflowUnit 
{
	private final Set<Statement> mOutflows = new HashSet<Statement>();
	private final CGNode mNode;
	private final int mInstIdx;
	public UrlConnectionInputUnit(CGNode node, int instIdx)
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
	public void addOutflowStatement(Statement stm)
	{
		mOutflows.add(stm);
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitUrlConnectionInputUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(UrlConnectionInputUnit.class.getSimpleName());
		builder.append(" url=");
		builder.append(getUrlValue());
		builder.append(", node=[");
		builder.append(mNode);
		builder.append(']');
		{
			IR ir = mNode.getIR();
			if(ir != null)
			{
				SSAInstruction inst = ir.getInstructions()[mInstIdx];
				builder.append(", inst=[");
				builder.append(inst);
				builder.append(']');
			}
		}
		builder.append(']');
		return builder.toString();
	}
}
