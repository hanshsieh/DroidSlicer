package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.NullValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;

public class CUriCommUnit extends UriCommUnit implements IInstructionUnit, IStatementFlowUnit
{
	private final CGNode mNode;
	private final int mInstIdx;
	private final Set<Statement> mInflows = new HashSet<Statement>();
	private ConcreteValue mUriValue = NullValue.getInstance();
	public CUriCommUnit(MethodReference targetMethod, CGNode node, int instIdx)
	{
		super(targetMethod);
		mNode = node;
		mInstIdx = instIdx;
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
	public void addInflowStatement(Statement stm)
	{
		mInflows.add(stm);
	}
	@Override
	public Collection<Statement> getInflowStatements()
	{
		return mInflows;
	}

	@Override
	public Collection<Statement> getOutflowStatements()
	{
		return Collections.emptySet();
	}
	@Override
	public boolean isAllowFlowThrough()
	{
		return false;
	}
	public void setUriValue(ConcreteValue intent)
	{
		if(intent == null)
			throw new IllegalArgumentException();
		mUriValue = intent;
	}
	@Override
	public ConcreteValue getUriValue()
	{
		return mUriValue;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(UriCommUnit.class.getSimpleName());
		builder.append(" target=");
		builder.append(getTargetMethod().getName());
		builder.append(", uri=");
		builder.append(getUriValue());
		builder.append(", node=[");
		builder.append(mNode);
		IR ir = mNode.getIR();
		if(ir != null)
		{
			SSAInstruction inst = ir.getInstructions()[mInstIdx];
			builder.append(", inst=[");
			builder.append(inst);
			builder.append(']');
		}
		builder.append(']');
		return builder.toString();
	}
}
