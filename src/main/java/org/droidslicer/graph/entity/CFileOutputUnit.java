package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.util.EntityUtils;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.value.ConcreteValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;

/**
 * TODO Handle java.nio
 * @author chuan
 *
 */
public class CFileOutputUnit extends FileOutputUnit implements IInstructionUnit, IStatementFlowUnit
{
	private final CGNode mNode;
	private final int mInstIdx;
	private final Set<Statement> mInflowStms = new HashSet<Statement>();
	private CFileOutputUnit(CGNode node, int instIdx, ConcreteValue pathVal)
	{
		super(pathVal);
		mNode = node;
		mInstIdx = instIdx;
	}
	public static CFileOutputUnit make(
			AndroidAnalysisContext analysisCtx, StatementWithInstructionIndex startStm, ConcreteValue pathVal, ProgressMonitor monitor)
		throws CancelException
	{
		Collection<Statement> flowStms;
		flowStms = EntityUtils.computeInOutStreamFlowStatements(analysisCtx, startStm, false, monitor);
		CFileOutputUnit result = new CFileOutputUnit(startStm.getNode(), startStm.getInstructionIndex(), pathVal);
		for(Statement stm : flowStms)
		{
			result.mInflowStms.add(stm);
		}
		return result;
	}
	@Override
	public Collection<Statement> getInflowStatements()
	{
		return mInflowStms;
	}

	@Override
	public Collection<Statement> getOutflowStatements() 
	{
		return Collections.emptySet();
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
	public boolean isAllowFlowThrough()
	{
		return false;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(FileOutputUnit.class.getSimpleName());
		builder.append(" val=");
		builder.append(getPathValue());
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
		builder.append("]");
		return builder.toString();
	}
}

