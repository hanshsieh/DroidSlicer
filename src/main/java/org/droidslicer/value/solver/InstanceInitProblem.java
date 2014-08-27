package org.droidslicer.value.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.droidslicer.ifds.SDGSupergraph;

import com.ibm.wala.dataflow.IFDS.IMergeFunction;
import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationProblem;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.UnorderedDomain;
import com.ibm.wala.ipa.slicer.MethodEntryStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;

public class InstanceInitProblem implements PartiallyBalancedTabulationProblem<Statement, PDG, Object>
{
	private final ISupergraph<Statement, PDG> mSupergraph;
	private final UnorderedDomain<Object, Statement> mDomain = new UnorderedDomain<Object, Statement>();
	private final Set<Statement> mSeeds;
	private final InstanceInitFlowFuction mFunct = new InstanceInitFlowFuction();
	public InstanceInitProblem(SDG sdg, Set<Statement> seeds)
	{
		// We want to do forward slicing
		mSupergraph = new SDGSupergraph(sdg);
		mSeeds = seeds;
	}
	@Override
	public ISupergraph<Statement, PDG> getSupergraph()
	{
		return mSupergraph;
	}

	@Override
	public TabulationDomain<Object, Statement> getDomain()
	{
		return mDomain;
	}

	@Override
	public Collection<PathEdge<Statement>> initialSeeds()
	{
		ArrayList<PathEdge<Statement>> result = new ArrayList<PathEdge<Statement>>();
		for(Statement seed : mSeeds)
		{
			Statement entryStm = getFakeEntry(seed);
			PathEdge<Statement> seedPath = PathEdge.createPathEdge(
					entryStm, 0, seed, 0);
			result.add(seedPath);
		}
		return result;
	}

	@Override
	public IMergeFunction getMergeFunction()
	{
		return null;
	}

	@Override
	public IPartiallyBalancedFlowFunctions<Statement> getFunctionMap()
	{
		return mFunct;
	}

	@Override
	public Statement getFakeEntry(Statement n)
	{
		return new MethodEntryStatement(n.getNode());
	}

}
