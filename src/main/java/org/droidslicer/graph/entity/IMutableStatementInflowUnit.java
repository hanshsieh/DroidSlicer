package org.droidslicer.graph.entity;

import com.ibm.wala.ipa.slicer.Statement;

public interface IMutableStatementInflowUnit
{
	public void addInflowStatement(Statement stm);
}
