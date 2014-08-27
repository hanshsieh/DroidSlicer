package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.types.MethodReference;

public class CICCParamCalleeUnit extends ICCParamCalleeUnit implements IStatementFlowUnit, IMutableStatementOutflowUnit
{
	private final HashSet<Statement> mOutflowStms = new HashSet<Statement>();
	public CICCParamCalleeUnit(MethodReference methodRef)
	{
		super(methodRef);
	}	
	@Override
	public Collection<Statement> getInflowStatements()
	{
		return Collections.emptySet();
	}

	@Override
	public Collection<Statement> getOutflowStatements()
	{
		return mOutflowStms;
	}
	@Override
	public void addOutflowStatement(Statement stm)
	{
		mOutflowStms.add(stm);
	}
	@Override
	public boolean isAllowFlowThrough() 
	{
		return false;
	}
}
