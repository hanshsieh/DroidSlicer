package org.droidslicer.graph;

import heros.InterproceduralCFG;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.graph.entity.ActivityUnit;
import org.droidslicer.graph.entity.ApplicationUnit;
import org.droidslicer.graph.entity.Call2ReturnRelation;
import org.droidslicer.graph.entity.ComponentReachRelation;
import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.DataDependencyRelation;
import org.droidslicer.graph.entity.ICCParamCalleeUnit;
import org.droidslicer.graph.entity.ICCParamCallerUnit;
import org.droidslicer.graph.entity.ICCRelation;
import org.droidslicer.graph.entity.ICCReturnCalleeUnit;
import org.droidslicer.graph.entity.ICCReturnCallerUnit;
import org.droidslicer.graph.entity.ICCUnit;
import org.droidslicer.graph.entity.IntentCommUnit;
import org.droidslicer.graph.entity.ReceiverUnit;
import org.droidslicer.graph.entity.RelationEntity;
import org.droidslicer.graph.entity.SUseUnit;
import org.droidslicer.graph.entity.ServiceUnit;
import org.droidslicer.graph.entity.UnitEntity;
import org.droidslicer.graph.entity.UriCommUnit;
import org.droidslicer.util.MethodId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.ibm.wala.types.Selector;

public class BehaviorSupergraph implements InterproceduralCFG<BehaviorNode, BehaviorMethod>
{
	private final static Logger mLogger = LoggerFactory.getLogger(BehaviorSupergraph.class);
	private final static int MAX_CALLERS_CACHE_SIZE = 1000;
	private final static int MAX_CALLEE_CACHE_SIZE = 10000;
	private final static int MAX_CALLS_WITHIN_METHOD_CACHE_SIZE = 3000;
	private final static int MAX_SUCC_CACHE_SIZE = 40000;
	private final static int MAX_RET_SITE_CACHE_SIZE = 20000;
	private final static int MAX_UNITS_TO_NODES_CACHE_SIZE = 1000;
	private final static int MAX_RET_TO_CALL_CACHE_SIZE = 1000;
	private final static int CONCURRENCY = Runtime.getRuntime().availableProcessors();
	private final static Selector SEL_ACT_CANDIDATE = MethodId.ANDROID_ACTIVITY_ON_CREATE.getMethodReference().getSelector();
	private final static Selector SEL_SERVICE_CANDIDATE = MethodId.ANDROID_SERVICE_ON_CREATE.getMethodReference().getSelector();
	private final static Selector SEL_APP_CANDIDATE = MethodId.ANDROID_APPLICATION_ON_CREATE.getMethodReference().getSelector();
	private final static Selector SEL_RECEIVER_CANDIDATE = MethodId.ANDROID_RECEIVER_ON_RECEIVE.getMethodReference().getSelector();

	// Set of nodes corresponding to the unit
	private final LoadingCache<UnitEntity, Set<BehaviorNode>> mUnit2NodesCache = getBaseCacheBuilder()
			.maximumSize(MAX_UNITS_TO_NODES_CACHE_SIZE)
			.build(new CacheLoader<UnitEntity, Set<BehaviorNode>>()
			{
				@Override
				public Set<BehaviorNode> load(UnitEntity unit) throws Exception 
				{
					Set<BehaviorNode> result = new LinkedHashSet<BehaviorNode>();
					for(RelationEntity oRelation : mGraph.incomingEdgesOf(unit))
					{
						if(!(oRelation instanceof ComponentReachRelation))
							continue;
						UnitEntity reachUnit = mGraph.getEdgeSource(oRelation);
						if(!(reachUnit instanceof ComponentUnit))
							continue;
						ComponentUnit reachComp = (ComponentUnit)reachUnit;
						ComponentReachRelation reachRelation = (ComponentReachRelation)oRelation;
						Selector candSel = getCandidateSelector(reachComp);
						if(candSel != null)
						{
							result.add(new BehaviorNode(new BehaviorMethod(reachComp, candSel), unit));
						}
						else
						{
							for(Selector reachSelector : reachRelation.getSelectors())
								result.add(new BehaviorNode(new BehaviorMethod(reachComp, reachSelector), unit));
						}
					}
					return result;
				}
		
			});
	private final LoadingCache<BehaviorMethod, Set<BehaviorNode>> mReachableUnitsCache = getBaseCacheBuilder()
			.maximumSize(CONCURRENCY * 100)
			.build(new CacheLoader<BehaviorMethod, Set<BehaviorNode>>()
			{
				@Override
				public Set<BehaviorNode> load(BehaviorMethod method)
						throws Exception
				{
					Set<BehaviorNode> result = new LinkedHashSet<BehaviorNode>();
					ComponentUnit comp = method.getComponent();
					Selector candSel = getCandidateSelector(comp);
					Selector selector = method.getSelector();
					if(candSel != null && !candSel.equals(selector))
						return Collections.emptySet();
					for(RelationEntity relation : mGraph.outgoingEdgesOf(comp))
					{
						if(!(relation instanceof ComponentReachRelation))
							continue;
						ComponentReachRelation compReachRel = (ComponentReachRelation)relation;

						// We need to check the selectors in the relation only if we don't use a 
						// candidate selector for the component
						if(candSel == null && !compReachRel.getSelectors().contains(selector))
							continue;
						result.add(new BehaviorNode(method, mGraph.getEdgeTarget(compReachRel)));
					}
					return result;
				}
				
			});
	private final LoadingCache<BehaviorNode, List<BehaviorNode>> mSuccsCache = getBaseCacheBuilder()
			.maximumSize(MAX_SUCC_CACHE_SIZE)
			.build(new CacheLoader<BehaviorNode, List<BehaviorNode>>()
			{
				@Override
				public List<BehaviorNode> load(BehaviorNode node)
						throws Exception
				{
					ArrayList<BehaviorNode> result = new ArrayList<BehaviorNode>();
					if(isCallStmt(node))
					{
						Collection<BehaviorMethod> methods = getCalleesOfCallAt(node);
						for(BehaviorMethod method : methods)
						{
							result.addAll(getStartPointsOf(method));
						}
						result.addAll(getReturnSitesOfCallAt(node));
					}
					else if(isExitStmt(node))
					{
						BehaviorMethod method = getMethodOf(node);
						for(BehaviorNode caller : getCallersOf(method))
						{
							for(BehaviorNode returnSite : getReturnSitesOfCallAt(caller))
							{
								result.add(returnSite);
							}
						}
					}
					else
					{
						BehaviorMethod method = node.getMethod();
						Selector selector = method.getSelector();
						ComponentUnit comp = method.getComponent();
						Set<BehaviorNode> reachableNodes = null;
						for(RelationEntity outRel : mGraph.outgoingEdgesOf(node.getUnit()))
						{
							if(outRel instanceof DataDependencyRelation)
							{
								UnitEntity target = mGraph.getEdgeTarget(outRel);
								
								// If we haven't built the set of reachable units of the entry point
								if(reachableNodes == null)
								{
									// See if we have the luck to have the corresponding nodes of the target unit
									Set<BehaviorNode> targetNodes = mUnit2NodesCache.getIfPresent(target);
									if(targetNodes != null)
									{
										BehaviorNode targetNode = new BehaviorNode(method, target);
										if(targetNodes.contains(targetNode))
											result.add(targetNode);
										continue;
									}
								}
								
								// We don't always directly check if the unit is reachable from the entry point 
								// because in the analysis of IFDS, it is more likely that we will continuously find the 
								// successor nodes of nodes reachable from a same entry point.
								if(reachableNodes == null)
									reachableNodes = mReachableUnitsCache.get(new BehaviorMethod(comp, selector));
								BehaviorNode targetNode = new BehaviorNode(method, target);
								if(reachableNodes.contains(targetNode))
									result.add(targetNode);
							}
						}
					}
					result.trimToSize();
					if(mLogger.isDebugEnabled())
					{
						Set<BehaviorNode> set = new HashSet<BehaviorNode>();
						for(BehaviorNode succ : result)
						{
							if(!set.add(succ))
							{
								throw new RuntimeException("Expecting unique nodes in the list");
							}
						}
					}
					return result;
				}
			});
	private final LoadingCache<BehaviorNode, Collection<BehaviorMethod>> mCalleesCache = getBaseCacheBuilder()
			.maximumSize(MAX_CALLEE_CACHE_SIZE)
			.build(new CacheLoader<BehaviorNode, Collection<BehaviorMethod>>()
			{
				@Override
				public Collection<BehaviorMethod> load(BehaviorNode node)
						throws Exception
				{
					UnitEntity unit = node.getUnit();
					Selector targetSelector = null;
					if(unit instanceof IntentCommUnit)
						targetSelector = null;
					else if(unit instanceof UriCommUnit)
						targetSelector = ((UriCommUnit)unit).getTargetMethod().getSelector();
					else
						throw new IllegalArgumentException();
					ArrayList<BehaviorMethod> result = new ArrayList<BehaviorMethod>();
					for(RelationEntity relation : mGraph.outgoingEdgesOf(unit))
					{
						if(!(relation instanceof ICCRelation))
							continue;
						UnitEntity target = mGraph.getEdgeTarget(relation);
						if(!(target instanceof ComponentUnit))
							continue;
						ComponentUnit comp = (ComponentUnit)target;
						
						// If we use a candidate selector for the component
						Selector candSel = getCandidateSelector(comp);
						if(candSel != null)
						{
							result.add(new BehaviorMethod(comp, candSel));
						}
						else
						{
							for(Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit> pair : comp.getEntryMethods())
							{
								Selector selector = pair.getLeft().getMethod().getSelector();
								if(targetSelector == null || targetSelector.equals(selector))
								{
									result.add(new BehaviorMethod(comp, selector));
								}
							}
						}
					}
					result.trimToSize();
					if(mLogger.isDebugEnabled())
					{
						Set<BehaviorMethod> set = new HashSet<BehaviorMethod>();
						for(BehaviorMethod succ : result)
						{
							if(!set.add(succ))
							{
								throw new RuntimeException("Expecting unique methods in the list");
							}
						}
					}
					return result;
				}
			});
	private final LoadingCache<BehaviorMethod, Collection<BehaviorNode>> mCallersCache = getBaseCacheBuilder()
			.maximumSize(MAX_CALLERS_CACHE_SIZE)
			.build(new CacheLoader<BehaviorMethod, Collection<BehaviorNode>>()
			{
				@Override
				public Collection<BehaviorNode> load(BehaviorMethod method)
						throws Exception
				{
					ArrayList<BehaviorNode> result = new ArrayList<BehaviorNode>();
					ComponentUnit comp = method.getComponent();
					Selector selector = method.getSelector();
					for(RelationEntity relation : mGraph.incomingEdgesOf(comp))
					{
						if(!(relation instanceof ICCRelation))
							continue;
						UnitEntity caller = mGraph.getEdgeSource(relation);
						
						// Check whether the caller node can reach this method
						if(caller instanceof IntentCommUnit)
						{}
						else if(caller instanceof UriCommUnit)
						{
							UriCommUnit uriUnit = (UriCommUnit)caller;
							Selector srcSelector = uriUnit.getTargetMethod().getSelector();
							if(!selector.equals(srcSelector))
								continue;
						}
						else
							continue;
						assert caller instanceof ICCParamCallerUnit;
						result.addAll(mUnit2NodesCache.get(caller));
					}
					result.trimToSize();
					if(mLogger.isDebugEnabled())
					{
						Set<BehaviorNode> set = new HashSet<BehaviorNode>();
						for(BehaviorNode succ : result)
						{
							if(!set.add(succ))
							{
								throw new RuntimeException("Expecting unique methods in the list");
							}
						}
					}
					return result;
				}
			});
	private final LoadingCache<BehaviorMethod, Set<BehaviorNode>> mCallsWithinMethodCache = getBaseCacheBuilder()
			.maximumSize(MAX_CALLS_WITHIN_METHOD_CACHE_SIZE)
			.build(new CacheLoader<BehaviorMethod, Set<BehaviorNode>>()
			{
				@Override
				public Set<BehaviorNode> load(BehaviorMethod method)
						throws Exception
				{
					return Sets.filter(mReachableUnitsCache.getUnchecked(method), new Predicate<BehaviorNode>()
					{
						@Override
						public boolean apply(BehaviorNode node)
						{
							return node.getUnit() instanceof ICCParamCallerUnit;
						}
					});
				}
			});
	private final LoadingCache<BehaviorNode, Collection<BehaviorNode>> mRetSitesCache = getBaseCacheBuilder()
			.maximumSize(MAX_RET_SITE_CACHE_SIZE)
			.build(new CacheLoader<BehaviorNode, Collection<BehaviorNode>>()
			{
				@Override
				public Collection<BehaviorNode> load(BehaviorNode node)
						throws Exception
				{
					ArrayList<BehaviorNode> result = new ArrayList<BehaviorNode>(1);
					UnitEntity caller = node.getUnit();
					for(RelationEntity relation : mGraph.outgoingEdgesOf(caller))
					{
						if(relation instanceof Call2ReturnRelation)
						{
							UnitEntity target = mGraph.getEdgeTarget(relation);
							if(target instanceof ICCReturnCallerUnit)
								result.add(new BehaviorNode(node.getMethod(), target));
						}
					}
					result.trimToSize();
					if(mLogger.isDebugEnabled())
					{
						Set<BehaviorNode> set = new HashSet<BehaviorNode>();
						for(BehaviorNode succ : result)
						{
							if(!set.add(succ))
							{
								throw new RuntimeException("Expecting unique methods in the list");
							}
						}
					}
					return result;
				}
			});
	private final LoadingCache<BehaviorNode, Collection<BehaviorNode>> mRet2CallCache = getBaseCacheBuilder()
			.maximumSize(MAX_RET_TO_CALL_CACHE_SIZE)
			.build(new CacheLoader<BehaviorNode, Collection<BehaviorNode>>()
			{
				@Override
				public Collection<BehaviorNode> load(BehaviorNode retSite)
						throws Exception
				{
					ArrayList<BehaviorNode> result = new ArrayList<BehaviorNode>();
					for(RelationEntity relation : mGraph.incomingEdgesOf(retSite.getUnit()))
					{
						if(!(relation instanceof Call2ReturnRelation))
							continue;
						UnitEntity callUnit = mGraph.getEdgeSource(relation);
						if(!(callUnit instanceof ICCParamCallerUnit))
							continue;
						result.add(new BehaviorNode(retSite.getMethod(), callUnit));
					}
					result.trimToSize();
					if(mLogger.isDebugEnabled())
					{
						Set<BehaviorNode> set = new HashSet<BehaviorNode>();
						for(BehaviorNode succ : result)
						{
							if(!set.add(succ))
							{
								throw new RuntimeException("Expecting unique methods in the list");
							}
						}
					}
					return result;
				}				
			});
	private final BehaviorGraph mGraph;
	public BehaviorSupergraph(BehaviorGraph graph)
	{
		mGraph = graph;
	}
	private CacheBuilder<Object, Object> getBaseCacheBuilder()
	{
		CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
				.concurrencyLevel(CONCURRENCY)
				.softValues();
		if(mLogger.isDebugEnabled())
		{
			cacheBuilder.recordStats();
		}
		return cacheBuilder;
	}
	public BehaviorGraph getBehaviorGraph()
	{
		return mGraph;
	}
	private static Selector getCandidateSelector(ComponentUnit reachComp)
	{
		if(reachComp instanceof ActivityUnit)
		{
			return SEL_ACT_CANDIDATE;
		}
		else if(reachComp instanceof ReceiverUnit)
		{
			return SEL_RECEIVER_CANDIDATE;
		}
		else if(reachComp instanceof ServiceUnit)
		{
			return SEL_SERVICE_CANDIDATE;
		}
		else if(reachComp instanceof ApplicationUnit)
		{
			return SEL_APP_CANDIDATE;
		}
		else
			return null;
	}
	private void logCacheStatistic(String name, Cache<?, ?> cache)
	{
		CacheStats stats = cache.stats();
		mLogger.debug("Cache statistic of {}", name);
		mLogger.debug("\tHit rate: {}, Persistent hit rate: {}, Size: {}, Avg load penalty: {} ms, Total load time: {} ms, Request count: {}, Load count: {}, Eviction count: {}", 
				stats.hitRate(), 
				stats.loadCount() + stats.hitCount() == 0 ? 0.0 : (double)stats.hitCount() / (stats.loadCount() + stats.hitCount()), 
				cache.size(), 
				stats.averageLoadPenalty() / (1000000.0), 
				stats.totalLoadTime() / (1000000.0), 
				stats.requestCount(), 
				stats.loadCount(), 
				stats.evictionCount());
	}
	public void logCacheStatistic()
	{
		if(!mLogger.isDebugEnabled())
			return;
		logCacheStatistic("unit2nodes", mUnit2NodesCache);
		logCacheStatistic("reachableUnits", mReachableUnitsCache);
		logCacheStatistic("succs", mSuccsCache);
		logCacheStatistic("callees", mCalleesCache);
		logCacheStatistic("callers", mCallersCache);
		logCacheStatistic("callsWithinMethod", mCallsWithinMethodCache);
		logCacheStatistic("retSites", mRetSitesCache);
		{
			int nNodes = 0;
			for(Set<BehaviorNode> nodes : mUnit2NodesCache.asMap().values())
				nNodes += nodes.size();
			mLogger.debug("Total # nodes in unit2NodesCache: {}", nNodes);
		}
		{
			int nNodes = 0;
			for(Set<BehaviorNode> nodes : mReachableUnitsCache.asMap().values())
			{
				nNodes += nodes.size();
			}
			mLogger.debug("Total # nodes in reachableNodesCache: {}", nNodes);
		}
		{
			int nNodes = 0;
			for(List<BehaviorNode> nodes : mSuccsCache.asMap().values())
			{
				nNodes += nodes.size();
			}
			mLogger.debug("Total # nodes in succsCache: {}", nNodes);
		}
		{
			int nMethods = 0;
			for(Collection<BehaviorMethod> methods : mCalleesCache.asMap().values())
			{
				nMethods += methods.size();
			}
			mLogger.debug("Total # methods in calleesCache: {}", nMethods);
		}
		{
			int nNodes = 0;
			for(Collection<BehaviorNode> nodes : mCallersCache.asMap().values())
			{
				nNodes += nodes.size();
			}
			mLogger.debug("Total # nodes in callersCache: {}", nNodes);
		}
		{
			int nNodes = 0;
			for(Collection<BehaviorNode> nodes : mCallsWithinMethodCache.asMap().values())
			{
				nNodes += nodes.size();
			}
			mLogger.debug("Total # nodes in callsWithinMethodCache: {}", nNodes);
		}
		{
			int nNodes = 0;
			for(Collection<BehaviorNode> nodes : mRetSitesCache.asMap().values())
			{
				nNodes += nodes.size();
			}
			mLogger.debug("Total # nodes in retSitesCache: {}", nNodes);
		}
		
	}
	public Set<BehaviorNode> getNodesForUnit(UnitEntity unit)
	{
		return mUnit2NodesCache.getUnchecked(unit);
	}
	@Override
	public BehaviorMethod getMethodOf(BehaviorNode node)
	{
		return node.getMethod();
	}

	@Override
	public List<BehaviorNode> getSuccsOf(BehaviorNode node)
	{
		return mSuccsCache.getUnchecked(node);
	}

	@Override
	public Collection<BehaviorMethod> getCalleesOfCallAt(BehaviorNode node)
	{
		return mCalleesCache.getUnchecked(node);
	}

	@Override
	public Collection<BehaviorNode> getCallersOf(BehaviorMethod method) 
	{
		return mCallersCache.getUnchecked(method);
	}

	@Override
	public Set<BehaviorNode> getCallsFromWithin(BehaviorMethod method)
	{
		return mCallsWithinMethodCache.getUnchecked(method);
	}

	@Override
	public Collection<BehaviorNode> getStartPointsOf(final BehaviorMethod method)
	{
		ComponentUnit comp = method.getComponent();
		Selector candSel = getCandidateSelector(comp);
		if(candSel != null)
		{
			if(!method.getSelector().equals(candSel))
				return Collections.emptySet();
			return Collections2.transform(comp.getEntryMethods(), new Function<Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit>, BehaviorNode>()
			{
				@Override
				public BehaviorNode apply(
						Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit> pair)
				{
					return new BehaviorNode(method, pair.getLeft());
				}
			});
		}
		else
		{
			Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit> pair = comp.getEntryMethod(method.getSelector());
			if(pair == null)
				return Collections.emptySet();
			return Collections.singleton(new BehaviorNode(method, pair.getLeft()));
		}
	}

	@Override
	public Collection<BehaviorNode> getReturnSitesOfCallAt(BehaviorNode node)
	{
		return mRetSitesCache.getUnchecked(node);
	}

	public boolean isReturnSiteStmt(BehaviorNode node)
	{
		return node.getUnit() instanceof ICCReturnCallerUnit;
	}
	@Override
	public boolean isCallStmt(BehaviorNode node)
	{
		return node.getUnit() instanceof ICCParamCallerUnit;
	}

	@Override
	public boolean isExitStmt(BehaviorNode node)
	{
		return node.getUnit() instanceof ICCReturnCalleeUnit;
	}

	@Override
	public boolean isStartPoint(BehaviorNode node)
	{
		return node.getUnit() instanceof ICCParamCalleeUnit;
	}

	@Override
	public Set<BehaviorNode> allNonCallStartNodes()
	{
		return new BehaviorNodesSet(mGraph, mUnit2NodesCache, new Predicate<UnitEntity>()
		{
			@Override
			public boolean apply(UnitEntity unit)
			{
				if(unit instanceof ICCUnit)
				{
					if(unit instanceof ICCParamCallerUnit || unit instanceof ICCParamCalleeUnit)
						return false;
					else
						return true;
				}
				else if(unit instanceof SUseUnit)
					return true;
				else
					return false;
			}
		});
	}
	public boolean containsEdge(BehaviorNode node1, BehaviorNode node2)
	{
		if(isCallStmt(node1))
		{
			if(isStartPoint(node2))
			{
				UnitEntity unit1 = node1.getUnit();
				BehaviorMethod method2 = node2.getMethod();
				if(unit1 instanceof IntentCommUnit)
				{}
				else if(unit1 instanceof UriCommUnit)
				{
					Selector targetSelector = ((UriCommUnit)unit1).getTargetMethod().getSelector();
					if(!method2.getSelector().equals(targetSelector))
						return false;
				}
				else
					throw new RuntimeException();
				ComponentUnit comp2 = method2.getComponent();
				RelationEntity relation = mGraph.getEdge(unit1, comp2);
				if(!(relation instanceof ICCRelation))
					return false;
				return true;
			}
			else if(isReturnSiteStmt(node2))
			{
				if(!node1.getMethod().equals(node2.getMethod()))
					return false;
				RelationEntity relation = mGraph.getEdge(node1.getUnit(), node2.getUnit());
				if(!(relation instanceof Call2ReturnRelation))
					return false;
				return true;
			}
			else
				return false;
		}
		else if(isExitStmt(node1))
		{
			if(isReturnSiteStmt(node2))
			{
				BehaviorMethod method1 = node1.getMethod();
				ComponentUnit comp1 = method1.getComponent();
				for(RelationEntity relation : mGraph.incomingEdgesOf(node2.getUnit()))
				{
					if(!(relation instanceof Call2ReturnRelation))
						continue;
					UnitEntity callerUnit = mGraph.getEdgeSource(relation);
					if(callerUnit instanceof IntentCommUnit)
					{}
					else if(callerUnit instanceof UriCommUnit)
					{
						Selector sel = ((UriCommUnit)callerUnit).getTargetMethod().getSelector();
						if(!method1.getSelector().equals(sel))
							continue;
					}
					else
						continue;
					RelationEntity reachRel = mGraph.getEdge(callerUnit, comp1);
					if(!(reachRel instanceof ICCRelation))
						continue;
					return true;
				}
				return false;
			}
			else
				return false;
		}
		else
		{
			if(!node1.getMethod().equals(node2.getMethod()))
				return false;
			RelationEntity relation = mGraph.getEdge(node1.getUnit(), node2.getUnit());
			if(!(relation instanceof DataDependencyRelation))
				return false;
			return true;
		}
	}
	public Collection<BehaviorNode> getCallSites(BehaviorNode retSite)
	{
		return mRet2CallCache.getUnchecked(retSite);
	}
	public Set<BehaviorNode> allNodes()
	{
		return new BehaviorNodesSet(mGraph, mUnit2NodesCache, Predicates.<UnitEntity>alwaysTrue());
	}

	public Collection<BehaviorNode> getExitsOf(BehaviorMethod method)
	{
		ComponentUnit comp = method.getComponent();
		Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit> pair = comp.getEntryMethod(method.getSelector());
		if(pair == null)
			return Collections.emptySet();
		else
			return Collections.singleton(new BehaviorNode(method, pair.getRight()));
	}
	public Collection<BehaviorMethod> getMethods()
	{
		ArrayList<BehaviorMethod> result = new ArrayList<BehaviorMethod>();
		for(UnitEntity unit : mGraph.vertexSet())
		{
			if(!(unit instanceof ComponentUnit))
				continue;
			ComponentUnit comp = (ComponentUnit)unit;
			Selector candSel = getCandidateSelector(comp);
			if(candSel != null)
			{
				result.add(new BehaviorMethod(comp, candSel));
			}
			else
			{
				for(Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit> pair : comp.getEntryMethods())
				{
					Selector sel = pair.getLeft().getMethod().getSelector();
					result.add(new BehaviorMethod(comp, sel));
				}
			}
		}
		return result;
	}
	public Collection<BehaviorNode> getNodesOfMethod(final BehaviorMethod method)
	{
		return mReachableUnitsCache.getUnchecked(method);
	}
	/**
	 * This method isn't actually used in IDE/IFDS solver.
	 */
	@Override
	public boolean isFallThroughSuccessor(BehaviorNode stmt, BehaviorNode succ)
	{
		return false;
	}

	/**
	 * This method isn't actually used in IDE/IFDS solver
	 */
	@Override
	public boolean isBranchTarget(BehaviorNode stmt, BehaviorNode succ)
	{
		return true;
	}
	private class BehaviorNodesSet extends AbstractSet<BehaviorNode>
	{
		private int mSize = -1;
		private final Cache<UnitEntity, Set<BehaviorNode>> mUnit2NodesCache;
		private final BehaviorGraph mGraph;
		private final Predicate<UnitEntity> mUnitFilter;
		public BehaviorNodesSet(BehaviorGraph graph, Cache<UnitEntity, Set<BehaviorNode>> unit2NodesCache, Predicate<UnitEntity> unitFilter)
		{
			if(graph == null || unit2NodesCache == null || unitFilter == null)
				throw new IllegalArgumentException();
			mGraph = graph;
			mUnit2NodesCache = unit2NodesCache;
			mUnitFilter = unitFilter;
		}
		@Override
		public boolean contains(Object obj)
		{
			if(!(obj instanceof BehaviorNode))
				return false;
			BehaviorNode node = (BehaviorNode)obj;
			UnitEntity unit = node.getUnit();
			if(!mGraph.vertexSet().contains(unit))
				return false;
			BehaviorMethod method = node.getMethod();
			ComponentUnit comp = method.getComponent();
			Selector selector = method.getSelector();
			Selector candSel = getCandidateSelector(comp);
			if(candSel != null && !candSel.equals(selector))
				return false;
			for(RelationEntity relation : mGraph.incomingEdgesOf(unit))
			{
				if(!(relation instanceof ComponentReachRelation))
					continue;
				ComponentReachRelation reachRelation = (ComponentReachRelation)relation;
				UnitEntity src = mGraph.getEdgeSource(reachRelation);
				if(!(src instanceof ComponentUnit))
					continue;
				ComponentUnit oComp = (ComponentUnit)src;
				if(!comp.equals(oComp))
					continue;
				if(candSel == null)
				{
					if(!reachRelation.getSelectors().contains(selector))
						continue;
				}
				return true;
			}
			return false;
		}
		@Override
		public int size()
		{
			if(mSize >= 0)
				return mSize;
			mSize = 0;
			for(UnitEntity unit : mGraph.vertexSet())
			{
				if(!(unit instanceof ICCUnit) && !(unit instanceof SUseUnit))
					continue;
				if(!mUnitFilter.apply(unit))
					continue;
				Set<BehaviorNode> nodes = mUnit2NodesCache.getIfPresent(unit);
				if(nodes != null)
				{
					mSize += nodes.size();
					continue;
				}
				for(RelationEntity relation : mGraph.incomingEdgesOf(unit))
				{
					if(!(relation instanceof ComponentReachRelation))
						continue;
					ComponentReachRelation reachRelation = (ComponentReachRelation)relation;
					UnitEntity src = mGraph.getEdgeSource(reachRelation);
					if(!(src instanceof ComponentUnit))
						continue;
					ComponentUnit comp = (ComponentUnit)src;
					Selector candSel = getCandidateSelector(comp);
					if(candSel != null)
						mSize += 1;
					else
						mSize += reachRelation.getSelectors().size();
					if(mSize < 0)
						throw new RuntimeException("Integer overflow");
				}
			}
			return mSize;
		}
		@Override
		public Iterator<BehaviorNode> iterator()
		{
			return new BehaviorNodesIterator(mGraph, mUnit2NodesCache, mUnitFilter);
		}
	}
	private static class BehaviorNodesIterator implements Iterator<BehaviorNode>
	{
		private final BehaviorGraph mGraph;
		private final Iterator<UnitEntity> mUnitsItr;
		private Iterator<BehaviorNode> mNodesItr = null;
		private final Cache<UnitEntity, Set<BehaviorNode>> mUnit2NodesCache;
		private final Predicate<UnitEntity> mUnitFilter;
		public BehaviorNodesIterator(BehaviorGraph graph,  
				Cache<UnitEntity, Set<BehaviorNode>> unit2NodesCache, 
				Predicate<UnitEntity> unitFilter)
		{
			if(graph == null || unit2NodesCache == null || unitFilter == null)
				throw new IllegalArgumentException();
			mGraph = graph;
			mUnitsItr = graph.vertexSet().iterator();
			mUnit2NodesCache = unit2NodesCache;
			mUnitFilter = unitFilter;
		}
		protected void findNextNodesChunk()
		{
			mNodesItr = null;
			while(mUnitsItr.hasNext())
			{
				UnitEntity unit = mUnitsItr.next();
				if(!(unit instanceof ICCUnit) && !(unit instanceof SUseUnit))
					continue;
				if(!mUnitFilter.apply(unit))
					continue;
				Set<BehaviorNode> nodes = mUnit2NodesCache.getIfPresent(unit);
				if(nodes != null)
				{
					mNodesItr = nodes.iterator();
					break;
				}
				mNodesItr = new BehaviorNodesForUnitIterator(mGraph, unit);
				break;
			}
		}
		@Override
		public boolean hasNext()
		{
			while(mNodesItr == null || !mNodesItr.hasNext())
			{
				findNextNodesChunk();
				if(mNodesItr == null)
					return false;
			}
			return true;
		}

		@Override
		public BehaviorNode next()
		{
			if(mNodesItr == null)
				findNextNodesChunk();
			return mNodesItr.next();
		}

		@Override
		public void remove()
		{
			throw new RuntimeException("Unimplemented");
		}	
	}
	private static class BehaviorNodesForUnitIterator implements Iterator<BehaviorNode>
	{
		private final UnitEntity mUnit;
		private final BehaviorGraph mGraph;
		private final Iterator<RelationEntity> mRelationsItr;
		private Iterator<BehaviorNode> mNodesItr = null;
		public BehaviorNodesForUnitIterator(BehaviorGraph graph, UnitEntity unit)
		{
			if(graph == null || unit == null)
				throw new IllegalArgumentException();
			mGraph = graph;
			mUnit = unit;
			mRelationsItr = mGraph.incomingEdgesOf(mUnit).iterator();
		}
		private void findNextNodesChunk()
		{
			mNodesItr = null;
			while(mRelationsItr.hasNext())
			{
				RelationEntity relation = mRelationsItr.next();
				if(!(relation instanceof ComponentReachRelation))
					continue;
				ComponentReachRelation reachRelation = (ComponentReachRelation)relation;
				UnitEntity src = mGraph.getEdgeSource(reachRelation);
				if(!(src instanceof ComponentUnit))
					continue;
				final ComponentUnit comp = (ComponentUnit)src;
				Selector candSel = getCandidateSelector(comp);
				if(candSel != null)
				{
					mNodesItr = Iterators.singletonIterator(new BehaviorNode(new BehaviorMethod(comp, candSel), mUnit));
				}
				else
				{
					mNodesItr = Iterators.transform(reachRelation.getSelectors().iterator(), new Function<Selector, BehaviorNode>()
					{
						@Override
						public BehaviorNode apply(Selector selector)
						{
							return new BehaviorNode(new BehaviorMethod(comp, selector), mUnit);
						}						
					});
				}
				break;
			}
		}
		@Override
		public boolean hasNext()
		{
			while(mNodesItr == null || !mNodesItr.hasNext())
			{
				findNextNodesChunk();
				if(mNodesItr == null)
					return false;
			}
			return true;
		}

		@Override
		public BehaviorNode next()
		{
			if(mNodesItr == null)
				findNextNodesChunk();
			return mNodesItr.next();
		}

		@Override
		public void remove()
		{
			throw new RuntimeException("Unimplemented");
		}
	}
}
