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

public class CIntentCommUnit extends IntentCommUnit implements IInstructionUnit, IStatementFlowUnit
{
	private final Set<Statement> mInflows = new HashSet<Statement>();
	private final CGNode mNode;
	private final int mInstIdx;
	private ConcreteValue mIntentValue = NullValue.getInstance();
	/**
	 * 
	 * @param targetEntityType type of the target component, e.g. android.app.Activity
	 * @param node
	 * @param instIdx
	 * @param intentParamIdx index of the Intent parameter (not including the implicit 'this')
	 */
	public CIntentCommUnit(Class<? extends AppComponentUnit> targetEntityType, CGNode node, int instIdx)
	{
		super(targetEntityType);
		mNode = node;
		mInstIdx = instIdx;
	}

	public void setIntentValue(ConcreteValue intent)
	{
		if(intent == null)
			throw new IllegalArgumentException();
		mIntentValue = intent;
	}
	@Override
	public ConcreteValue getIntentValue()
	{
		return mIntentValue;
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
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(IntentCommUnit.class.getSimpleName());
		builder.append(" target=");
		builder.append(getTargetEntityType().getSimpleName());
		builder.append(", intent=");
		builder.append(getIntentValue());
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
