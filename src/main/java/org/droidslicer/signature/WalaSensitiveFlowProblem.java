package org.droidslicer.signature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.droidslicer.graph.BehaviorMethod;
import org.droidslicer.graph.BehaviorNode;
import org.droidslicer.graph.NumberedBehaviorSupergraph;

import com.ibm.wala.dataflow.IFDS.IMergeFunction;
import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationProblem;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.UnorderedDomain;

public class WalaSensitiveFlowProblem implements PartiallyBalancedTabulationProblem<BehaviorNode, BehaviorMethod, Object>
{
	private final NumberedBehaviorSupergraph mSupergraph;
	private final Map<BehaviorNode, Set<Object>> mSeeds;
	private final TabulationDomain<Object, BehaviorNode> mDomain = new UnorderedDomain<Object, BehaviorNode>();
	private final Object mZeroFactoid = new Object();
	private final WalaSensitiveFlowFunctions mFuncts = new WalaSensitiveFlowFunctions();
	private final Map<BehaviorNode, BehaviorNode> mFakeEntries = new HashMap<BehaviorNode, BehaviorNode>();
	public WalaSensitiveFlowProblem(NumberedBehaviorSupergraph supergraph, Map<BehaviorNode, Set<Object>> seeds)
	{
		mSupergraph = supergraph;
		mSeeds = seeds;
		
		// Preserve the zero fact
		mDomain.add(mZeroFactoid);
		for(Set<Object> facts : seeds.values())
		{
			for(Object fact : facts)
			{
				mDomain.add(fact);
			}
		}
	}
	@Override
	public ISupergraph<BehaviorNode, BehaviorMethod> getSupergraph()
	{
		return mSupergraph;
	}

	@Override
	public TabulationDomain<Object, BehaviorNode> getDomain()
	{
		return mDomain;
	}

	@Override
	public Collection<PathEdge<BehaviorNode>> initialSeeds()
	{
		ArrayList<PathEdge<BehaviorNode>> result = new ArrayList<PathEdge<BehaviorNode>>();
		for(Map.Entry<BehaviorNode, Set<Object>> entry : mSeeds.entrySet())
		{
			for(Object factoid : entry.getValue())
			{
				int fact = mDomain.getMappedIndex(factoid);
				BehaviorNode node = entry.getKey();
				BehaviorNode entryNode = getFakeEntry(node);
				PathEdge<BehaviorNode> seedPath = PathEdge.createPathEdge(
						entryNode, fact, node, fact);
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
	public IPartiallyBalancedFlowFunctions<BehaviorNode> getFunctionMap()
	{
		return mFuncts;
	}

	@Override
	public BehaviorNode getFakeEntry(BehaviorNode node)
	{
		BehaviorNode entry = mFakeEntries.get(node);
		if(entry != null)
			return entry;
		BehaviorMethod method = mSupergraph.getProcOf(node);
		
		// TODO Is really OK to select an arbitrary one?
		entry = mSupergraph.getEntriesForProcedure(method)[0];
		mFakeEntries.put(node, entry);
		return entry;
	}
	
}
