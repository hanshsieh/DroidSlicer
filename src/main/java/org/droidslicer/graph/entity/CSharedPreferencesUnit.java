package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.droidslicer.value.ConcreteValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;

public class CSharedPreferencesUnit extends SharedPreferencesUnit 
	implements IInstructionUnit, IStatementFlowUnit, IMutableStatementInflowUnit, IMutableStatementOutflowUnit
{
	private final CGNode mNode;
	private final int mInstIdx;
	private final Set<Statement> mInflows = new LinkedHashSet<Statement>();
	private final Set<Statement> mOutflows = new LinkedHashSet<Statement>();
	public CSharedPreferencesUnit(ConcreteValue sharedPrefVal, CGNode node, int instIdx)
	{
		super(sharedPrefVal);
		if(node == null || instIdx < 0)
			throw new IllegalArgumentException();
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
		return mInflows;
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
	public void addInflowStatement(Statement stm)
	{
		mInflows.add(stm);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(SharedPreferencesUnit.class.getSimpleName());
		builder.append(" value=");
		builder.append(getSharedPreferencesValue());
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
