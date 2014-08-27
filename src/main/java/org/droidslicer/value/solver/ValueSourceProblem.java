package org.droidslicer.value.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.ifds.SDGSupergraph;

import com.ibm.wala.dataflow.IFDS.BackwardsSupergraph;
import com.ibm.wala.dataflow.IFDS.IMergeFunction;
import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationProblem;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.ipa.slicer.MethodExitStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

public class ValueSourceProblem implements PartiallyBalancedTabulationProblem<Statement, PDG, Statement>
{
	private final ISupergraph<Statement, PDG> mSupergraph;
	private final ValueSourceFunctions mFunct;
	private final Collection<Statement> mSeeds;
	private final IntSet mSeedFacts;
	public ValueSourceProblem(SDG sdg, ValueSourceFunctions funct, Collection<Statement> seeds, IntSet facts)
	{
		SDGSupergraph supergraph = new SDGSupergraph(sdg);
		
		// We want to do backward-slicing
		mSupergraph = BackwardsSupergraph.make(supergraph);
		mFunct = funct;
		mSeeds = seeds;
		mSeedFacts = facts;
	}
	public Iterator<Pair<Statement, Statement>> getKilledEdges()
	{
		return mFunct.getKilledEdges();
	}
	@Override
	public ISupergraph<Statement, PDG> getSupergraph()
	{
		return mSupergraph;
	}

	@Override
	public TabulationDomain<Statement, Statement> getDomain()
	{
		return mFunct.getDomain();
	}

	@Override
	public Collection<PathEdge<Statement>> initialSeeds()
	{
		ArrayList<PathEdge<Statement>> result = new ArrayList<PathEdge<Statement>>();
		result.ensureCapacity(mSeeds.size());
		for(Statement seed : mSeeds)
		{
			IntIterator itr = mSeedFacts.intIterator();
			while(itr.hasNext())
			{
				int fact = itr.next();
				Statement entryStm = getFakeEntry(seed);
				PathEdge<Statement> seedPath = PathEdge.createPathEdge(
						entryStm, fact, seed, fact);
				result.add(seedPath);
			}
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
	public Statement getFakeEntry(Statement stm)
	{
		return new MethodExitStatement(stm.getNode());
	}

}
