package org.droidslicer.signature;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.config.BehaviorSignatureParser;
import org.droidslicer.graph.BehaviorGraph;
import org.droidslicer.graph.BehaviorMethod;
import org.droidslicer.graph.BehaviorNode;
import org.droidslicer.graph.BehaviorSupergraph;
import org.droidslicer.graph.NumberedBehaviorSupergraph;
import org.droidslicer.graph.entity.DataDependencyRelation;
import org.droidslicer.graph.entity.FileInputUnit;
import org.droidslicer.graph.entity.FileOutputUnit;
import org.droidslicer.graph.entity.FileSystemDataRelation;
import org.droidslicer.graph.entity.RelationEntity;
import org.droidslicer.graph.entity.SQLiteDbInputUnit;
import org.droidslicer.graph.entity.SQLiteDbOutputUnit;
import org.droidslicer.graph.entity.UnitEntity;
import org.droidslicer.ifds.IFDSSolver;
import org.droidslicer.util.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationSolver;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

public class BehaviorSignaturesTester
{
	private static class FlowSignatureEntry
	{
		private boolean mSolved = false;
		private FlowSignature mFlowSig;
		private final Set<BehaviorNode> mFromNodes, mToNodes;
		public FlowSignatureEntry(FlowSignature flowSig, Set<BehaviorNode> fromNodes, Set<BehaviorNode> toNodes)
		{
			if(flowSig == null || fromNodes == null || toNodes == null)
				throw new IllegalArgumentException();
			mFlowSig = flowSig;
			mFromNodes = fromNodes;
			mToNodes = toNodes;
		}
		public FlowSignature getFlowSignature()
		{
			return mFlowSig;
		}
		public Set<BehaviorNode> getSourceNodes()
		{
			return mFromNodes;
		}
		public Set<BehaviorNode> getDstNodes()
		{
			return mToNodes;
		}
		public boolean isSolved()
		{
			return mSolved;
		}
		public void setSolved(boolean val)
		{
			mSolved = val;
		}
	}
	private static class EncodedFlowSignatures
	{
		private final List<BitSet> mEncode = new ArrayList<BitSet>();
		
		// It is important to use {@link java.util.LinkedHashMap} to preserve the insertion order
		// so that nodes with a same dataflow fact can be grouped together.
		private final LinkedHashMap<BehaviorNode, Integer> mNodeFacts = new LinkedHashMap<BehaviorNode, Integer>();
		private final List<FlowSignatureEntry> mFlowSigsEntries;
		private final Map<BehaviorNode, Set<FlowSignatureEntry>> mDstNode2Sigs = new HashMap<BehaviorNode, Set<FlowSignatureEntry>>();
		private final Map<BehaviorNode, Set<FlowSignatureEntry>> mSrcNode2Sigs = new HashMap<BehaviorNode, Set<FlowSignatureEntry>>();
		public EncodedFlowSignatures(List<FlowSignatureEntry> flowSigEntries)
		{
			if(flowSigEntries == null)
				throw new IllegalArgumentException();
			mFlowSigsEntries = flowSigEntries;
			Map<BitSet, Set<BehaviorNode>> sig2Nodes = new HashMap<BitSet, Set<BehaviorNode>>();
			{
				Map<BehaviorNode, BitSet> node2Sig = new HashMap<BehaviorNode, BitSet>();
				int nSigs = flowSigEntries.size();
				int id = 0;
				for(FlowSignatureEntry entry : flowSigEntries)
				{
					Set<BehaviorNode> fromNodes = entry.getSourceNodes();
					for(BehaviorNode fromNode : fromNodes)
					{
						BitSet encode = node2Sig.get(fromNode);
						if(encode == null)
						{
							encode = new BitSet(nSigs);
							node2Sig.put(fromNode, encode);
						}
						encode.set(id);
					}
					++id;
				}
				
				mLogger.debug("Before encoding, # nodes: {}", node2Sig.size());
				
				for(Map.Entry<BehaviorNode, BitSet> entry : node2Sig.entrySet())
				{
					BehaviorNode node = entry.getKey();
					BitSet encode = entry.getValue();
					Set<BehaviorNode> nodes = sig2Nodes.get(encode);
					if(nodes == null)
					{
						nodes = new LinkedHashSet<BehaviorNode>();
						sig2Nodes.put(encode, nodes);
					}
					nodes.add(node);
				}
			}
			{
				int fact = 0;
				List<Map.Entry<BitSet, Set<BehaviorNode>>> sig2NodesList = new ArrayList<Map.Entry<BitSet, Set<BehaviorNode>>>(sig2Nodes.size());
				sig2NodesList.addAll(sig2Nodes.entrySet());
				Collections.sort(sig2NodesList, new Comparator<Map.Entry<BitSet, Set<BehaviorNode>>>()
				{
					@Override
					public int compare(Entry<BitSet, Set<BehaviorNode>> entry1,
							Entry<BitSet, Set<BehaviorNode>> entry2)
					{
						return entry2.getKey().cardinality() - entry1.getKey().cardinality();
					}
				});
				for(Map.Entry<BitSet, Set<BehaviorNode>> entry : sig2NodesList)
				{
					appendEncode(entry.getKey());
					for(BehaviorNode node : entry.getValue())
						mNodeFacts.put(node, fact);
					++fact;
				}
				mLogger.debug("After encoding, # facts: {}", fact);
			}
			
			for(FlowSignatureEntry entry : flowSigEntries)
			{
				for(BehaviorNode node : entry.getSourceNodes())
				{
					Set<FlowSignatureEntry> sigs = mSrcNode2Sigs.get(node);
					if(sigs == null)
					{
						sigs = new LinkedHashSet<FlowSignatureEntry>();
						mSrcNode2Sigs.put(node, sigs);
					}
					sigs.add(entry);
				}	
				for(BehaviorNode node : entry.getDstNodes())
				{
					Set<FlowSignatureEntry> sigs = mDstNode2Sigs.get(node);
					if(sigs == null)
					{
						sigs = new LinkedHashSet<FlowSignatureEntry>();
						mDstNode2Sigs.put(node, sigs);
					}
					sigs.add(entry);
				}			
			}
		}
		public Set<FlowSignatureEntry> getFlowSignaturesForDstNode(BehaviorNode node)
		{
			Set<FlowSignatureEntry> result = mDstNode2Sigs.get(node);
			if(result == null)
				return Collections.emptySet();
			else
				return result;
		}
		public void setFlowSignatureSolved(FlowSignatureEntry sigEntry)
		{
			sigEntry.setSolved(true);
			for(BehaviorNode node : sigEntry.getDstNodes())
			{
				Set<FlowSignatureEntry> sigEntries = mDstNode2Sigs.get(node);
				if(sigEntries == null)
					continue;
				sigEntries.remove(sigEntry);
				if(sigEntries.isEmpty())
				{
					mDstNode2Sigs.remove(node);
				}
			}
			for(BehaviorNode node : sigEntry.getSourceNodes())
			{
				Set<FlowSignatureEntry> sigEntries = mSrcNode2Sigs.get(node);
				if(sigEntries == null)
					continue;
				sigEntries.remove(sigEntry);
				if(sigEntries.isEmpty())
				{
					mSrcNode2Sigs.remove(node);
					mNodeFacts.remove(node);
				}
			}
		}
		public Collection<FlowSignatureEntry> getFlowSignatureEntries()
		{
			return mFlowSigsEntries;
		}
		public void appendEncode(BitSet encode)
		{
			mEncode.add(encode);
		}
		public Pair<BehaviorNode, Set<Object>> popPendingNode()
		{
			Iterator<Map.Entry<BehaviorNode, Integer>> itr = mNodeFacts.entrySet().iterator();
			Map.Entry<BehaviorNode, Integer> entry = itr.next();
			itr.remove();
			return Pair.of(entry.getKey(), Collections.singleton((Object)entry.getValue()));
		}
		public Pair<BehaviorNode, Set<Object>> peekPendingNode()
		{
			Iterator<Map.Entry<BehaviorNode, Integer>> itr = mNodeFacts.entrySet().iterator();
			Map.Entry<BehaviorNode, Integer> entry = itr.next();
			return Pair.of(entry.getKey(), Collections.singleton((Object)entry.getValue()));
		}
		public boolean hasPendingNodes()
		{
			return !mNodeFacts.isEmpty();
		}
		public int getNumPendingNodes()
		{
			return mNodeFacts.size();
		}
		public Iterator<FlowSignatureEntry> getUnsolvedSignatures()
		{
			return Iterators.filter(mFlowSigsEntries.iterator(), new Predicate<FlowSignatureEntry>()
			{
				@Override
				public boolean apply(FlowSignatureEntry sig)
				{
					return !sig.isSolved();
				}				
			});
		}
		public Iterator<FlowSignatureEntry> getFlowSignatures(Set<Object> facts)
		{
			final BitSet encode = new BitSet(mFlowSigsEntries.size());
			for(Object fact : facts)
			{
				if(!(fact instanceof Integer))
					continue;
				encode.or(mEncode.get((Integer)fact));
			}
			return Iterators.filter(mFlowSigsEntries.iterator(), new Predicate<FlowSignatureEntry>()
			{
				private int idx = 0;
				@Override
				public boolean apply(FlowSignatureEntry entry)
				{
					boolean result = encode.get(idx);
					++idx;
					return result;
				}				
			});
		}
	}
	private final static int MAX_UNIT_SIGS_CACHE_SIZE = 1000;
	private final static long MAX_IFDS_SIZE_HEROS = 125000000L;
	//private final static long MAX_IFDS_SIZE_WALA = 125000000L;
	private final static long MAX_IFDS_SIZE_WALA = 54000000L;
	private final static int MAX_PROCESS_NODES_FACTOR_PER_ROUND = 4;
	private final static int MAX_IFDS_PROPAGATION_FACTOR = 2;
	private final static boolean USE_HEROS = false;
	private final static Logger mLogger = LoggerFactory.getLogger(BehaviorSignaturesTester.class);
	private final Collection<BehaviorSignature> mSigs;
	private final Collection<BehaviorSignature> mMatchedSigs = new ArrayList<BehaviorSignature>();
	private final BehaviorSupergraph mSupergraph; 
	private final NumberedBehaviorSupergraph mNumberedSupergraph;
	private final LoadingCache<UnitSignaturesUnion, Set<BehaviorNode>> mMatchedUnitSigNodesCache = CacheBuilder.newBuilder()
		.maximumSize(MAX_UNIT_SIGS_CACHE_SIZE)
		.softValues()
		.build(new CacheLoader<UnitSignaturesUnion, Set<BehaviorNode>>()
		{
			@Override
			public Set<BehaviorNode> load(UnitSignaturesUnion sig)
					throws Exception
			{
				Set<BehaviorNode> result = new LinkedHashSet<BehaviorNode>();
				BehaviorGraph graph = mSupergraph.getBehaviorGraph();
				for(UnitEntity unit : graph.vertexSet())
				{
					Collection<BehaviorNode> nodes = sig.evaluate(mSupergraph, unit);
					result.addAll(nodes);
				}
				return result;
			}			
		});
	public BehaviorSignaturesTester(BehaviorGraph graph, File sigFile)
		throws IOException
	{
		BehaviorSignatureParser parser = new BehaviorSignatureParser(new FileInputStream(sigFile));
		mSigs = parser.getSemanticSignatures();
		mSupergraph = new BehaviorSupergraph(graph);
		if(USE_HEROS)
			mNumberedSupergraph = null;
		else
			mNumberedSupergraph = new NumberedBehaviorSupergraph(mSupergraph);
	}
	public BehaviorSupergraph getSupergraph()
	{
		return mSupergraph;
	}
	public Collection<BehaviorSignature> getMatchedSignatures()
	{
		return mMatchedSigs;
	}
	private List<FlowSignatureEntry> solveUnitSignatures(SemanticSignatureContext ctx)
	{
		List<FlowSignatureEntry> pendingFlowSigs = 
				new ArrayList<FlowSignatureEntry>();
		// We store the pending signatures to another collection to avoid concurrent modification
		Collection<Signature> sigs = new ArrayList<Signature>(ctx.getPendingSignature());
		for(Signature sig : sigs)
		{
			if(sig instanceof FlowSignature)
			{
				FlowSignature flowSig = (FlowSignature)sig;
				UnitSignaturesUnion toSig = flowSig.getTo();
				Set<BehaviorNode> toNodes = Sets.filter(mMatchedUnitSigNodesCache.getUnchecked(toSig), new Predicate<BehaviorNode>()
				{
					@Override
					public boolean apply(BehaviorNode node)
					{
						UnitEntity unit = node.getUnit();
						if(unit instanceof FileInputUnit || unit instanceof SQLiteDbInputUnit)
							return false;
						else
							return true;
					}
				});
				if(toNodes.isEmpty())
				{
					ctx.setSignatureValue(flowSig, false);
					continue;
				}
				UnitSignaturesUnion fromSig = flowSig.getFrom();
				Set<BehaviorNode> fromNodes = Sets.filter(mMatchedUnitSigNodesCache.getUnchecked(fromSig), new Predicate<BehaviorNode>()
				{
					@Override
					public boolean apply(BehaviorNode node)
					{
						UnitEntity unit = node.getUnit();
						if(unit instanceof FileOutputUnit || unit instanceof SQLiteDbOutputUnit)
							return false;
						else
							return true;
					}
				});
				if(fromNodes.isEmpty())
				{
					ctx.setSignatureValue(flowSig, false);
					continue;
				}
				if(hasIntersection(fromNodes, toNodes))
				{
					ctx.setSignatureValue(flowSig, true);
					continue;
				}
				pendingFlowSigs.add(new FlowSignatureEntry(flowSig, fromNodes, toNodes));
			}
			else if(sig instanceof UnitSignaturesUnion)
			{
				UnitSignaturesUnion unionSig = (UnitSignaturesUnion)sig;
				Collection<BehaviorNode> nodes = mMatchedUnitSigNodesCache.getUnchecked(unionSig);
				ctx.setSignatureValue(sig, !nodes.isEmpty());
			}
			else
				throw new IllegalArgumentException("Unknown type of signature: " + sig.getClass().getName());
		}
		return pendingFlowSigs;
	}
	private static boolean updateNodeFacts(Map<BehaviorNode, Set<Object>> map, BehaviorNode node, Set<Object> facts)
	{
		if(facts.isEmpty())
			return false;
		Set<Object> oldFacts = map.get(node);
		if(oldFacts == null)
		{
			oldFacts = new LinkedHashSet<Object>();
			map.put(node, oldFacts);
		}
		return oldFacts.addAll(facts);
	}
	private static boolean isIFDSMemSizeAcceptable(int nNode, int nFact)
	{
		if(USE_HEROS)
			return (long)nNode * nNode * nNode * nFact * nFact <= MAX_IFDS_SIZE_HEROS;
		else
			return (long)nNode * nNode * nNode * nFact <= MAX_IFDS_SIZE_WALA;
	}
	private static int calMaxProcessNodesPerRound(int nNode)
	{
		return nNode * MAX_PROCESS_NODES_FACTOR_PER_ROUND;
	}
	private static int calMaxIFDSPropagationCount(int nNode)
	{
		return nNode * nNode * MAX_IFDS_PROPAGATION_FACTOR;
	}
	private int dumpChildren(Stack<BehaviorNode> stack, BehaviorNode node, int depth)
	{
		int count = 1;
		if(stack.contains(node))
		{
			mLogger.debug("Reocurring node found: {}", node);
			for(BehaviorNode n : stack)
			{
				mLogger.debug("node: {}", n);
			}
		}
		stack.push(node);
		try
		{
			if(depth < 0)
				return count;
			for(BehaviorNode succ : mSupergraph.getSuccsOf(node))
			{
				//mLogger.debug("depth: {}, node: {}", depth, succ);
				count += dumpChildren(stack, succ, depth - 1);
			}
			return count;
		}
		finally
		{
			stack.pop();
		}
	}
	private Iterator<Pair<BehaviorNode, Set<Object>>> solveIFDSWithHeros(Map<BehaviorNode, Set<Object>> seedsWithFacts)
	{
		SensitiveFlowProblem problem = new SensitiveFlowProblem(mSupergraph, seedsWithFacts);
		final IFDSSolver<BehaviorNode, Object, BehaviorMethod, BehaviorSupergraph> solver = 
				new IFDSSolver<BehaviorNode, Object, BehaviorMethod, BehaviorSupergraph>(problem, null);
		{
			//solver.setMaxPropagationCount(calMaxIFDSPropagationCount(seedsWithFacts.size()));
			solver.solve();
			if(solver.getMaxPropagationCount() >= 0 && solver.propagationCount >= solver.getMaxPropagationCount())
				mLogger.debug("IFDS propagation limit ({}) is reached", solver.getMaxPropagationCount());
			mLogger.debug("Propagation count: {}, flow function app count: {}, duration flow function app: {}, flow function construction count: {}, duration flow function construction: {}", 
					solver.propagationCount, 
					solver.flowFunctionApplicationCount, solver.durationFlowFunctionApplication, 
					solver.flowFunctionConstructionCount, solver.durationFlowFunctionConstruction);
		}
		Set<BehaviorNode> reachedNodes = solver.getReachedNodes();
		return Iterators.transform(reachedNodes.iterator(), new Function<BehaviorNode, Pair<BehaviorNode, Set<Object>>>()
		{
			@Override
			public Pair<BehaviorNode, Set<Object>> apply(BehaviorNode node) 
			{
				return Pair.of(node, solver.ifdsResultsAt(node));
			}			
		});
	}
	private Iterator<Pair<BehaviorNode, Set<Object>>> solveIFDSWithWala(Map<BehaviorNode, Set<Object>> seedsWithFacts)
	{
		final WalaSensitiveFlowProblem problem = new WalaSensitiveFlowProblem(mNumberedSupergraph, seedsWithFacts);
		PartiallyBalancedTabulationSolver<BehaviorNode, BehaviorMethod, Object> solver = 
				PartiallyBalancedTabulationSolver.createPartiallyBalancedTabulationSolver(problem, null);
		
		try
		{
			final TabulationResult<BehaviorNode, BehaviorMethod, Object> tResult = solver.solve();
			Collection<BehaviorNode> reachedNodes = tResult.getSupergraphNodesReached();
			return Iterators.transform(reachedNodes.iterator(), new Function<BehaviorNode, Pair<BehaviorNode, Set<Object>>>()
			{
				@Override
				public Pair<BehaviorNode, Set<Object>> apply(BehaviorNode node) 
				{
					IntSet facts = tResult.getResult(node);
					Set<Object> factoids = new HashSet<Object>();
					TabulationDomain<Object, BehaviorNode> domain = problem.getDomain();
					for(IntIterator itr = facts.intIterator(); itr.hasNext(); )
					{
						factoids.add(domain.getMappedObject(itr.next()));
					}
					return Pair.of(node, factoids);
				}			
			});
		}
		catch(CancelException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	private Map<BehaviorNode, Set<Object>> consumeAndFindReachableNodes(EncodedFlowSignatures flowSigs)
	{
		Map<BehaviorNode, Set<Object>> result = new LinkedHashMap<BehaviorNode, Set<Object>>();
		Map<BehaviorNode, Set<Object>> seedFacts = new LinkedHashMap<BehaviorNode, Set<Object>>();
		Set<BehaviorNode> pendingNodes = new LinkedHashSet<BehaviorNode>();
		
		// Extract the nodes to be processed
		{
			Set<Object> allFacts = new HashSet<Object>();
			while(flowSigs.hasPendingNodes())
			{
				Pair<BehaviorNode, Set<Object>> pair = flowSigs.peekPendingNode();
				BehaviorNode node = pair.getLeft();
				Set<Object> facts = pair.getRight();
				if(facts.isEmpty())
				{
					flowSigs.popPendingNode();
					continue;
				}
				int newNumFacts = allFacts.size();
				for(Object fact : facts)
				{
					if(!allFacts.contains(fact))
						++newNumFacts;
				}
				if(!isIFDSMemSizeAcceptable(pendingNodes.size() + 1, newNumFacts))
					break;
				flowSigs.popPendingNode();
				allFacts.add(facts);
				seedFacts.put(node, new LinkedHashSet<Object>(facts));
				pendingNodes.add(node);
			}
			mLogger.debug("Solving reachability from {} nodes, # facts: {}", pendingNodes.size(), allFacts.size());
		}
		
		final int iniNumNodes = pendingNodes.size();
		int nProcessed = 0;
		while(!pendingNodes.isEmpty())
		{
			// TODO It is a bypass solution to prevent too many extended nodes
			// in complex app. Think of better solution
			if(nProcessed >= calMaxProcessNodesPerRound(iniNumNodes))
			{
				mLogger.debug("Too many processed nodes, abandon the remain nodes. # processed nodes: {}. # pending nodes in queue: {}", nProcessed, pendingNodes.size());
				break;
			}
			Map<BehaviorNode, Set<Object>> seedsWithFacts = new HashMap<BehaviorNode, Set<Object>>();
			{
				Set<Object> allFacts = new HashSet<Object>();
				for(Iterator<BehaviorNode> itr = pendingNodes.iterator(); itr.hasNext(); )
				{
					BehaviorNode seed = itr.next();
					Set<Object> facts = seedFacts.get(seed);
					int newNumFacts = allFacts.size();
					for(Object fact : facts)
					{
						if(!allFacts.contains(fact))
							++newNumFacts;
					}
					if(!isIFDSMemSizeAcceptable(seedsWithFacts.size() + 1, newNumFacts))
						break;
					itr.remove();
					allFacts.add(facts);
					seedsWithFacts.put(seed, facts);
				}
				nProcessed += seedsWithFacts.size();
				mLogger.debug("Solving IFDS problem with {} seeds, # processed nodes: {}, # pending nodes in queue: {}", seedsWithFacts.size(), nProcessed, pendingNodes.size());
			}
			int nReachedNode = 0;			
			int nFileExtendNodes = 0, nDataExtendNodes = 0;
			Iterator<Pair<BehaviorNode, Set<Object>>> ifdsResult;
			{
				Stopwatch solverWatch = Stopwatch.createStarted();
				if(USE_HEROS)
					ifdsResult = solveIFDSWithHeros(seedsWithFacts);
				else
					ifdsResult = solveIFDSWithWala(seedsWithFacts);
				mLogger.debug("Finished solving IFDS problem. Elapsed time: {}", solverWatch);
				mSupergraph.logCacheStatistic();
			}
			Stopwatch extendWatch = Stopwatch.createStarted();
			while(ifdsResult.hasNext())
			{
				++nReachedNode;
				Pair<BehaviorNode, Set<Object>> pair = ifdsResult.next();
				BehaviorNode reachedNode = pair.getLeft();
				Set<Object> facts = pair.getRight();
				if(facts.isEmpty())
					continue;
				
				// Update the facts
				updateNodeFacts(result, reachedNode, facts);
				
				// Find other nodes that are potentially data dependent on the node
				BehaviorGraph graph = mSupergraph.getBehaviorGraph();
				
				if(nProcessed + pendingNodes.size() > calMaxProcessNodesPerRound(iniNumNodes))
					continue;

				LABEL_FIND_EXTENDED_NODES:
				for(RelationEntity outRel : graph.outgoingEdgesOf(reachedNode.getUnit()))
				{
					DataDependencyRelation dataRel = null;
					if(outRel instanceof FileSystemDataRelation)
					{}
					else if(outRel instanceof DataDependencyRelation)
						dataRel = (DataDependencyRelation)outRel;
					else
						continue;					
					
					UnitEntity extendUnit = graph.getEdgeTarget(outRel);
					
					// Get the nodes corresponding to the unit
					for(BehaviorNode extendNode : mSupergraph.getNodesForUnit(extendUnit))
					{
						if(dataRel != null && !dataRel.getCrossComponentConditions().contains(extendNode.getMethod()))
							continue;
						if(!updateNodeFacts(seedFacts, extendNode, facts))
							continue;
						if(pendingNodes.add(extendNode))
						{
							if(outRel instanceof FileSystemDataRelation)
								++nFileExtendNodes;
							else if(outRel instanceof DataDependencyRelation)
								++nDataExtendNodes;
							if(nProcessed + pendingNodes.size() > calMaxProcessNodesPerRound(iniNumNodes))
							{
								break LABEL_FIND_EXTENDED_NODES;
							}
						}
					}
				}
			}
			mLogger.debug("Elapsed time for finding extended nodes: {}. # reached nodes: {}. # extended nodes via file: {}, # extended nodes via data: {}", extendWatch, nReachedNode, nFileExtendNodes, nDataExtendNodes);
		}
		return result;
	}
	private boolean hasIntersection(Set<? extends Object> set1, Set<? extends Object> set2)
	{
		if(set1.size() > set2.size())
		{
			Set<? extends Object> tmp = set1;
			set1 = set2;
			set2 = tmp;
		}
		for(Object val : set1)
		{
			if(set2.contains(val))
				return true;
		}
		return false;
	}

	private void evalutePendingSignatures(SemanticSignatureContext ctx)
	{
		if(ctx.getPendingSignature().isEmpty())
			return;
		mLogger.debug("Evaluating pending signatures. # pending signatures: {}", ctx.getPendingSignature().size());

		// Flow signatures waiting to be solved
		EncodedFlowSignatures encodedFlowSigs = new EncodedFlowSignatures(solveUnitSignatures(ctx));		
		 
		mLogger.debug("Evaluating flow signatures. # flow signatures: {}", encodedFlowSigs.getFlowSignatureEntries().size());		
		mLogger.debug("Total number of nodes to solve for reachability: {}", encodedFlowSigs.getNumPendingNodes());
		int oriPendingNodes = encodedFlowSigs.getNumPendingNodes();
		Stopwatch totalWatch = Stopwatch.createStarted();
		
		while(encodedFlowSigs.hasPendingNodes())
		{
			Stopwatch roundWatch = Stopwatch.createStarted();
			Map<BehaviorNode, Set<Object>> reachableNodes = consumeAndFindReachableNodes(encodedFlowSigs);
			for(Map.Entry<BehaviorNode, Set<Object>> entry : reachableNodes.entrySet())
			{
				BehaviorNode node = entry.getKey();
				Set<Object> facts = entry.getValue();
				
				// Get the signatures that have this node as destination node
				Set<FlowSignatureEntry> sigEntries = encodedFlowSigs.getFlowSignaturesForDstNode(node);
			
				// If no signatures are interested to the node, then skip the node
				if(sigEntries.isEmpty())
					continue;

				for(Iterator<FlowSignatureEntry> reachingSigsItr = encodedFlowSigs.getFlowSignatures(facts); reachingSigsItr.hasNext(); )
				{
					FlowSignatureEntry reachingSigEntry = reachingSigsItr.next();
					if(sigEntries.contains(reachingSigEntry))
					{
						ctx.setSignatureValue(reachingSigEntry.getFlowSignature(), true);
						encodedFlowSigs.setFlowSignatureSolved(reachingSigEntry);
					}
				}
			}
			if(mLogger.isDebugEnabled())
			{
				int pendingNodes = encodedFlowSigs.getNumPendingNodes();
				int consumedNodes = oriPendingNodes - pendingNodes;
				long elapsedTime = totalWatch.elapsed(TimeUnit.MILLISECONDS);
				long remainTime = (long)((double)elapsedTime / consumedNodes * pendingNodes);
				String remainTimeStr = String.format("%02d:%02d:%02d",
						TimeUnit.MILLISECONDS.toHours(remainTime),
					    TimeUnit.MILLISECONDS.toMinutes(remainTime) % 60,
					    TimeUnit.MILLISECONDS.toSeconds(remainTime) % 60);
				mLogger.debug("Round finished. Total elapsed time: {}. Round elapsed time: {}. Estimated remaining time: {}. Total nodes: {}. Remaining nodes: {}", totalWatch, roundWatch, remainTimeStr, oriPendingNodes, pendingNodes);
			}
		}
		
		// The remaining flow signatures are not matched
		for(Iterator<FlowSignatureEntry> sigEntriesItr = encodedFlowSigs.getUnsolvedSignatures(); sigEntriesItr.hasNext(); )
		{
			ctx.setSignatureValue(sigEntriesItr.next().getFlowSignature(), false);
		}
		mLogger.debug("Evaluation of pending signatures finished. Elapsed time: {}", totalWatch);
	}
	public void test(ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			mMatchedSigs.clear();
			mLogger.debug("Matching semantic signatures");
			monitor.beginTask("Matching semantic signatures", mSigs.size() * 10);
			SemanticSignatureContext ctx = new SemanticSignatureContext();
			if(mLogger.isDebugEnabled())
			{
				mLogger.debug("Calculating number of non-call-start nodes in supergraph...");
				Set<BehaviorNode> allNodes = mSupergraph.allNodes();
				int size = allNodes.size();
				BehaviorGraph graph = mSupergraph.getBehaviorGraph();
				mLogger.debug("# nodes in SBG: {}, # edges in SBG: {}", graph.vertexSet().size(), graph.edgeSet().size());
				mLogger.debug("# nodes in supergraph: {}", size);
				int checkSize = 0;
				for(Iterator<BehaviorNode> itr = allNodes.iterator(); itr.hasNext(); )
				{
					itr.next();
					checkSize++;
				}
				if(checkSize != size)
					throw new RuntimeException("Number of non-call-start nodes mismatched");
			}
			ctx.setSupergraph(mSupergraph);
			mLogger.debug("Number of semantic signatures: {}", mSigs.size());
			Stopwatch totalTimeWatch = Stopwatch.createStarted();
			int nMatched = 0;
			int nProcessed = 0;
			{
				Collection<BehaviorSignature> pendingSigs = new LinkedList<BehaviorSignature>();
				pendingSigs.addAll(mSigs);
				Stopwatch progressReportWatch = Stopwatch.createStarted();
				Stopwatch statsReportWatch = Stopwatch.createStarted();
				while(!pendingSigs.isEmpty())
				{
					for(Iterator<BehaviorSignature> sigsItr = pendingSigs.iterator(); sigsItr.hasNext(); )
					{
						BehaviorSignature sig = sigsItr.next();
						boolean matched;
						try
						{
							matched = sig.getSignature().apply(ctx);
						}
						catch(UnevaluatedSignatureException ex)
						{
							continue;
						}
						if(matched)
						{
							mMatchedSigs.add(sig);
							++nMatched;
						}
						sigsItr.remove();
						monitor.worked(10);
						++nProcessed;
					}
					evalutePendingSignatures(ctx);
					if(progressReportWatch.elapsed(TimeUnit.MINUTES) > 10)
					{
						mLogger.debug("# processed: {}, # matched: {}, elapsed time: {}", nProcessed, nMatched, totalTimeWatch);
						progressReportWatch.reset();
						progressReportWatch.start();
					}
					if(statsReportWatch.elapsed(TimeUnit.MINUTES) > 20)
					{
						mSupergraph.logCacheStatistic();						
						statsReportWatch.reset();
						statsReportWatch.start();
					}
				}
			}
			mSupergraph.logCacheStatistic();
			totalTimeWatch.stop();
			if(mLogger.isDebugEnabled())
			{
				StringBuilder builder = new StringBuilder("Matched signatures: \n");
				for(BehaviorSignature sig : mMatchedSigs)
				{
					builder.append('\t');
					builder.append(sig.getName());
					builder.append('\n');
				}
				mLogger.debug("{}", builder.toString());
			}
			mLogger.debug("Number of matched signatures: {}", nMatched);
			mLogger.debug("Elapsed time for matching sensitive behavior signatures: {}", totalTimeWatch);
		}
		catch(CancelRuntimeException ex)
		{
			throw new CancelException(ex);
		}
		finally
		{
			monitor.done();
		}
	}
}
