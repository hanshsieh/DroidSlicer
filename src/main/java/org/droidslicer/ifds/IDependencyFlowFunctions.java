package org.droidslicer.ifds;

import java.util.Map;

import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.intset.IntSet;

public interface IDependencyFlowFunctions extends IPartiallyBalancedFlowFunctions<Statement> 
{
	public void addSeed(Statement stm, IntSet facts);
	public void clearSeeds();
	public Map<Statement, ? extends IntSet> getSeeds();
	public int getZeroFact();
}
