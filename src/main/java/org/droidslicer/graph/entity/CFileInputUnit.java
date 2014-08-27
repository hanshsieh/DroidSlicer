package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.util.EntityUtils;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.value.ConcreteValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;

public class CFileInputUnit extends FileInputUnit implements IInstructionUnit, IStatementFlowUnit
{
	private final CGNode mNode;
	private final int mInstIdx;
	private final Set<Statement> mOutflowStms = new HashSet<Statement>();
	public CFileInputUnit(CGNode node, int instIdx, ConcreteValue pathVal)
	{
		super(pathVal);
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

	@Override
	public Collection<Statement> getInflowStatements() 
	{
		return Collections.emptyList();
	}

	@Override
	public Collection<Statement> getOutflowStatements() 
	{
		return mOutflowStms;
	}
	public static CFileInputUnit make(
			AndroidAnalysisContext analysisCtx, StatementWithInstructionIndex startStm, ConcreteValue pathVal, ProgressMonitor monitor)
		throws CancelException
	{
		monitor.beginTask("Building FileInputEntity", 100);
		try
		{
			Collection<Statement> flowStms;
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 100);
				flowStms = EntityUtils.computeInOutStreamFlowStatements(analysisCtx, startStm, true, subMonitor);
			}
			CFileInputUnit result = new CFileInputUnit(startStm.getNode(), startStm.getInstructionIndex(), pathVal);
			for(Statement stm : flowStms)
			{
				result.mOutflowStms.add(stm);
			}
			return result;
		}
		finally
		{
			monitor.done();
		}
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
		builder.append(FileInputUnit.class.getSimpleName());
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
