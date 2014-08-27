package org.droidslicer.graph.entity.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.graph.BehaviorMethod;
import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.ICCParamCallerUnit;
import org.droidslicer.graph.entity.ICCReturnCallerUnit;
import org.droidslicer.graph.entity.UnitEntity;
import org.droidslicer.value.solver.ConcreteValueSolver;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.Statement;

class UnitsResolverContext
{
	private final Map<UnitEntity, CGNode> mUnits = new HashMap<UnitEntity, CGNode>();
	private final AndroidAnalysisContext mAnalysisCtx;
	private Set<CGNode> mScannedNodes = new HashSet<CGNode>();
	
	// We use {@LinkedHashSet} so that the iteration order is the same as insertion order.
	// It enables us to use it as a queue, as well as checking the existence of an element
	// efficiently.
	private final LinkedHashSet<CGNode> mPendingNodes = new LinkedHashSet<CGNode>();
	private final Map<CGNode, Set<BehaviorMethod>> mReachableMethodsForNode = new HashMap<CGNode, Set<BehaviorMethod>>();
	private CGNode mCurrNode = null;
	private final Map<InstanceKey, Set<CGNode>> mInstance2EntryNodes = new HashMap<InstanceKey, Set<CGNode>>();
	private final Collection<ComponentUnit> mCompUnits = new ArrayList<ComponentUnit>();
	private final Set<Pair<ICCParamCallerUnit, ICCReturnCallerUnit>> mCall2Rets = new HashSet<Pair<ICCParamCallerUnit, ICCReturnCallerUnit>>();
	private final Set<Statement> mActInFlows = new LinkedHashSet<Statement>();
	private final Set<Statement> mActOutFlows = new LinkedHashSet<Statement>();
	private final Map<CGNode, Set<CGNode>> mExtraReachNodes = new HashMap<CGNode, Set<CGNode>>();
	public UnitsResolverContext(AndroidAnalysisContext analysisCtx)
	{
		mAnalysisCtx = analysisCtx;
	}
	public Collection<Pair<ICCParamCallerUnit, ICCReturnCallerUnit>> getCall2ReturnRelations()
	{
		return mCall2Rets;
	}
	public void addCall2ReturnRelation(ICCParamCallerUnit call, ICCReturnCallerUnit ret)
	{
		mCall2Rets.add(Pair.of(call, ret));
	}
	public AndroidAnalysisContext getAnalysisContext()
	{
		return mAnalysisCtx;
	}
	public ConcreteValueSolver getValueSolver()
	{
		return mAnalysisCtx.getValueSolver();
	}
	public Collection<BehaviorMethod> getReachableMethods(UnitEntity unit)
	{
		CGNode node = mUnits.get(unit);
		if(node == null)
			throw new IllegalArgumentException();
		Set<BehaviorMethod> comps = mReachableMethodsForNode.get(node);
		if(comps == null)
		{
			return Collections.emptySet();
		}
		else
		{
			return comps;
		}
	}
	public Collection<UnitEntity> getUnits()
	{
		return mUnits.keySet();
	}
	public boolean isScanned(CGNode node)
	{
		return mScannedNodes.contains(node);
	}
	public void setScanned(CGNode node)
	{
		mScannedNodes.add(node);
	}
	public CGNode processNext()
	{
		Iterator<CGNode> itr = mPendingNodes.iterator();
		if(!itr.hasNext())
			return null;
		CGNode node = itr.next();
		itr.remove();
		mCurrNode = node;
		return mCurrNode;
	}
	public CGNode getCurrentNode()
	{
		return mCurrNode;
	}
	public void addUnit(UnitEntity unit)
	{
		if(mCurrNode == null)
			throw new IllegalStateException("Call graph node must be set");
		if(mUnits.containsKey(unit))
			throw new IllegalArgumentException("Duplicate unit");
		mUnits.put(unit, mCurrNode);
	}
	public void addPendingNode(CGNode node, Set<BehaviorMethod> comps)
	{
		Set<BehaviorMethod> oldMethods = mReachableMethodsForNode.get(node);
		if(oldMethods == null)
		{
			oldMethods = new HashSet<BehaviorMethod>();
			mReachableMethodsForNode.put(node, oldMethods);
		}
		boolean isMod = oldMethods.addAll(comps);
		if(isMod || !mScannedNodes.contains(node))
		{
			mPendingNodes.add(node);
		}
	}
	public Set<BehaviorMethod> getReachableMethods()
	{
		if(mCurrNode == null)
			throw new IllegalStateException();
		return getReachableMethods(mCurrNode);
	}
	public Set<BehaviorMethod> getReachableMethods(CGNode node)
	{
		Set<BehaviorMethod> comps = mReachableMethodsForNode.get(node);
		if(comps == null)
			return Collections.emptySet();
		else
			return comps;
	}
	public Map<CGNode, Set<BehaviorMethod>> getNode2ReachableMethodsMap()
	{
		return mReachableMethodsForNode;
	}
	public Set<CGNode> getEntryNodesForInstance(InstanceKey instance)
	{
		Set<CGNode> nodes = mInstance2EntryNodes.get(instance);
		if(nodes == null)
			return Collections.emptySet();
		else
			return nodes;
	}
	public void addEntryNodeForInstance(InstanceKey instance, CGNode entryNode)
	{
		Set<CGNode> nodes = mInstance2EntryNodes.get(instance);
		if(nodes == null)
		{
			nodes = new HashSet<CGNode>();
			mInstance2EntryNodes.put(instance, nodes);
		}
		nodes.add(entryNode);
	}
	public void addComponentUnit(ComponentUnit comp)
	{
		mCompUnits.add(comp);
	}
	public Collection<ComponentUnit> getComponentUnits()
	{
		return mCompUnits;
	}
	public void addActivityInFlow(Statement stm)
	{
		mActInFlows.add(stm);
	}
	public Set<Statement> getActivityInFlows()
	{
		return mActInFlows;
	}
	public void addActivityOutFlow(Statement stm)
	{
		mActOutFlows.add(stm);
	}
	public Set<Statement> getActivityOutFlows()
	{
		return mActOutFlows;
	}
	public void addExtraReachNode(CGNode node, CGNode reachedNode)
	{
		Set<CGNode> reachedNodes = mExtraReachNodes.get(node);
		if(reachedNodes == null)
		{
			reachedNodes = new HashSet<CGNode>();
			mExtraReachNodes.put(node, reachedNodes);
		}
		reachedNodes.add(reachedNode);
	}
	public Collection<CGNode> getExtraReachedNodes(CGNode node)
	{
		Collection<CGNode> result = mExtraReachNodes.get(node);
		if(result == null)
			return Collections.emptySet();
		else
			return result;
	}
}
