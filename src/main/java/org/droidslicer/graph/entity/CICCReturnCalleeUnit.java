package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.types.MethodReference;

public class CICCReturnCalleeUnit extends ICCReturnCalleeUnit implements IStatementFlowUnit, IMutableStatementInflowUnit
{
	private final HashSet<Statement> mInflowStms = new HashSet<Statement>();
	public CICCReturnCalleeUnit(MethodReference methodRef)
	{
		super(methodRef);
	}

	@Override
	public Collection<Statement> getOutflowStatements()
	{
		return Collections.emptySet();
	}

	@Override
	public Collection<Statement> getInflowStatements()
	{
		return mInflowStms;
	}
	@Override
	public void addInflowStatement(Statement stm)
	{
		mInflowStms.add(stm);
	}

	@Override
	public boolean isAllowFlowThrough()
	{
		return true;
	}
}
