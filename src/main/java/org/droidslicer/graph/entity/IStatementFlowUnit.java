package org.droidslicer.graph.entity;

import java.util.Collection;

import com.ibm.wala.ipa.slicer.Statement;

public interface IStatementFlowUnit
{
	public boolean isAllowFlowThrough();
	public Collection<Statement> getInflowStatements();
	public Collection<Statement> getOutflowStatements();
}
