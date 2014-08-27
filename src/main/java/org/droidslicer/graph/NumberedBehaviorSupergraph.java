package org.droidslicer.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterators;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

public class NumberedBehaviorSupergraph	implements ISupergraph<BehaviorNode, BehaviorMethod>
{
	private static class MethodEntry
	{
		private final int mFirstNodeId;
		private final int mNumNodes;
		public MethodEntry(int nodeStartId, int numNodes)
		{
			if(nodeStartId < 0 || numNodes < 0)
				throw new IllegalArgumentException();
			mFirstNodeId = nodeStartId;
			mNumNodes = numNodes;
		}
		/**
		 * The first node ID of the nodes in the method.
		 * @return
		 */
		public int getFirstNodeId()
		{
			return mFirstNodeId;
		}
		/**
		 * Returns Number of nodes in the method.
		 * @return
		 */
		public int getNumberNodes()
		{
			return mNumNodes;
		}
	}
	private static class NodeEntry
	{
		private final int mNodeId;
		public NodeEntry(int nodeId)
		{
			mNodeId = nodeId;
		}
		public int getNodeId()
		{
			return mNodeId;
		}
	}
	private final List<BehaviorNode> mNodes;
	private final Map<BehaviorMethod, MethodEntry> mMethodsMap = new HashMap<BehaviorMethod, MethodEntry>();
	private final Map<BehaviorNode, NodeEntry> mNodesMap = new HashMap<BehaviorNode, NodeEntry>();
	private final BehaviorSupergraph mSupergraph;
	public NumberedBehaviorSupergraph(BehaviorSupergraph supergraph)
	{
		mSupergraph = supergraph;
		ArrayList<BehaviorNode> nodes = new ArrayList<BehaviorNode>();
		for(BehaviorMethod method : mSupergraph.getMethods())
		{
			Collection<BehaviorNode> nodesOfMethod = mSupergraph.getNodesOfMethod(method);
			mMethodsMap.put(method, new MethodEntry(nodes.size(), nodesOfMethod.size()));
			nodes.ensureCapacity(nodes.size() + nodesOfMethod.size());
			for(BehaviorNode node : nodesOfMethod)
			{
				mNodesMap.put(node, new NodeEntry(nodes.size()));
				nodes.add(node);
			}
		}
		nodes.trimToSize();
		mNodes = nodes;
	}
	public BehaviorSupergraph getDelegate()
	{
		return mSupergraph;
	}
	@Override
	public void removeNodeAndEdges(BehaviorNode n)
			throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<BehaviorNode> iterator()
	{
		return mSupergraph.allNodes().iterator();
	}

	@Override
	public int getNumberOfNodes()
	{
		return mNodes.size();
	}

	@Override
	public void addNode(BehaviorNode n)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeNode(BehaviorNode n) throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsNode(BehaviorNode node)
	{
		return mNodesMap.containsKey(node);
	}

	@Override
	public Iterator<BehaviorNode> getPredNodes(BehaviorNode n)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getPredNodeCount(BehaviorNode n)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<BehaviorNode> getSuccNodes(BehaviorNode n)
	{
		return mSupergraph.getSuccsOf(n).iterator();
	}

	@Override
	public int getSuccNodeCount(BehaviorNode node)
	{
		return mSupergraph.getSuccsOf(node).size();
	}

	@Override
	public void addEdge(BehaviorNode src, BehaviorNode dst)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeEdge(BehaviorNode src, BehaviorNode dst)
			throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();		
	}

	@Override
	public void removeAllIncidentEdges(BehaviorNode node)
			throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();	
	}

	@Override
	public void removeIncomingEdges(BehaviorNode node)
			throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeOutgoingEdges(BehaviorNode node)
			throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasEdge(BehaviorNode src, BehaviorNode dst)
	{
		return mSupergraph.containsEdge(src, dst);
	}

	@Override
	public int getNumber(BehaviorNode node)
	{
		NodeEntry entry = mNodesMap.get(node);
		return entry.getNodeId();
	}

	@Override
	public BehaviorNode getNode(int number)
	{
		return mNodes.get(number);
	}

	@Override
	public int getMaxNumber()
	{
		return mNodes.size() - 1;
	}

	@Override
	public Iterator<BehaviorNode> iterateNodes(IntSet set)
	{
		final IntIterator itr = set.intIterator();
		return new Iterator<BehaviorNode>()
		{
			@Override
			public boolean hasNext()
			{
				return itr.hasNext();
			}

			@Override
			public BehaviorNode next()
			{
				int id = itr.next();
				return mNodes.get(id);
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public IntSet getSuccNodeNumbers(BehaviorNode node)
	{
		Collection<BehaviorNode> succs = mSupergraph.getSuccsOf(node);
		MutableIntSet set = MutableSparseIntSet.createMutableSparseIntSet(succs.size());
		for(BehaviorNode succ : succs)
			set.add(mNodesMap.get(succ).getNodeId());
		return set;
	}

	@Override
	public IntSet getPredNodeNumbers(BehaviorNode node)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Graph<? extends BehaviorMethod> getProcedureGraph()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCall(BehaviorNode node)
	{
		return mSupergraph.isCallStmt(node);
	}

	@Override
	public Iterator<? extends BehaviorNode> getCalledNodes(BehaviorNode call)
	{
		final Iterator<BehaviorMethod> methodsItr = mSupergraph.getCalleesOfCallAt(call).iterator();
		return new Iterator<BehaviorNode>()
		{
			private Iterator<BehaviorNode> mStartNodesItr = null;
			@Override
			public boolean hasNext()
			{
				while(mStartNodesItr == null || !mStartNodesItr.hasNext())
				{
					if(methodsItr.hasNext())
						mStartNodesItr = mSupergraph.getStartPointsOf(methodsItr.next()).iterator();
					else
						return false;
				}
				return true;
			}

			@Override
			public BehaviorNode next()
			{
				hasNext();
				return mStartNodesItr.next();
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
	
		};
	}

	@Override
	public Iterator<BehaviorNode> getNormalSuccessors(BehaviorNode call)
	{
		return Iterators.emptyIterator();
	}

	@Override
	public Iterator<? extends BehaviorNode> getReturnSites(BehaviorNode call,
			BehaviorMethod callee)
	{
		return mSupergraph.getReturnSitesOfCallAt(call).iterator();
	}

	@Override
	public Iterator<? extends BehaviorNode> getCallSites(BehaviorNode ret,
			BehaviorMethod callee)
	{
		return mSupergraph.getCallSites(ret).iterator();
	}

	@Override
	public boolean isExit(BehaviorNode node)
	{
		return mSupergraph.isExitStmt(node);
	}

	@Override
	public BehaviorMethod getProcOf(BehaviorNode node)
	{
		return mSupergraph.getMethodOf(node);
	}

	@Override
	public BehaviorNode[] getEntriesForProcedure(BehaviorMethod method)
	{
		Collection<BehaviorNode> entries = mSupergraph.getStartPointsOf(method);
		BehaviorNode[] result = new BehaviorNode[entries.size()];
		entries.toArray(result);
		return result;
	}

	@Override
	public BehaviorNode[] getExitsForProcedure(BehaviorMethod procedure)
	{
		Collection<BehaviorNode> exits = mSupergraph.getExitsOf(procedure);
		BehaviorNode[] result = new BehaviorNode[exits.size()];
		exits.toArray(result);
		return result;
	}

	@Override
	public int getNumberOfBlocks(BehaviorMethod method)
	{
		return mMethodsMap.get(method).getNumberNodes();
	}

	@Override
	public int getLocalBlockNumber(BehaviorNode node)
	{
		NodeEntry nodeEntry = mNodesMap.get(node);
		BehaviorMethod method = node.getMethod();
		MethodEntry methodEntry = mMethodsMap.get(method);
		int idx = nodeEntry.getNodeId() - methodEntry.getFirstNodeId();
		assert idx >= 0;
		return idx;
	}

	@Override
	public BehaviorNode getLocalBlock(BehaviorMethod method, int idx)
	{
		MethodEntry entry = mMethodsMap.get(method);
		int nodeId = entry.getFirstNodeId() + idx;
		return mNodes.get(nodeId);
	}

	@Override
	public boolean isReturn(BehaviorNode node)
	{
		return mSupergraph.isReturnSiteStmt(node);
	}

	@Override
	public boolean isEntry(BehaviorNode node)
	{
		return mSupergraph.isStartPoint(node);
	}

	@Override
	public byte classifyEdge(BehaviorNode src, BehaviorNode dest)
	{
		throw new UnsupportedOperationException();
	}

}
