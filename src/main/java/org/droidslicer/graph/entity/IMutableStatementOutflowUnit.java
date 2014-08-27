package org.droidslicer.graph.entity;

import com.ibm.wala.ipa.slicer.Statement;

public interface IMutableStatementOutflowUnit
{
	public void addOutflowStatement(Statement stm);
}
