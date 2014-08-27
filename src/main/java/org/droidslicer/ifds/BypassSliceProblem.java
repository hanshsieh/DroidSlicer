package org.droidslicer.ifds;

import java.util.ArrayList;
import java.util.Collection;

import com.ibm.wala.dataflow.IFDS.IMergeFunction;
import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationProblem;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.UnorderedDomain;
import com.ibm.wala.ipa.slicer.MethodEntryStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.Statement;

public class BypassSliceProblem implements PartiallyBalancedTabulationProblem<Statement, PDG, Object>
{
	protected final ISupergraph<Statement, PDG> mSupergraph;
	protected IDependencyFlowFunctions mFuncts;
	public BypassSliceProblem(
			ISupergraph<Statement, PDG> sdgSupergraph, 
			IDependencyFlowFunctions flowFuncts)
	{
		if(sdgSupergraph == null || flowFuncts == null)
			throw new IllegalArgumentException();
		mSupergraph = sdgSupergraph;
		mFuncts = flowFuncts;
	}
	@Override
	public ISupergraph<Statement, PDG> getSupergraph()
	{
		return mSupergraph;
	}
	@Override
	public TabulationDomain<Object, Statement> getDomain()
	{
		// We doesn't need this, just return a domain without priority of path edges
		return new UnorderedDomain<Object, Statement>();
	}
	@Override
	public IPartiallyBalancedFlowFunctions<Statement> getFunctionMap() 
	{
		return mFuncts;
	}
	@Override
	public Collection<PathEdge<Statement>> initialSeeds()
	{
		Collection<PathEdge<Statement>> result = new ArrayList<PathEdge<Statement>>();
		int zeroFact = mFuncts.getZeroFact();
		for (Statement stm : mFuncts.getSeeds().keySet())
		{
			Statement entryStm = getFakeEntry(stm);
			PathEdge<Statement> seed = PathEdge.createPathEdge(
					entryStm, zeroFact, stm, zeroFact);
			result.add(seed);
		}
		return result;
	}
	@Override
	public IMergeFunction getMergeFunction()
	{
		// No merge function
		return null;
	}
	@Override
	public Statement getFakeEntry(Statement stm) 
	{
		return new MethodEntryStatement(stm.getNode());
	}
}
