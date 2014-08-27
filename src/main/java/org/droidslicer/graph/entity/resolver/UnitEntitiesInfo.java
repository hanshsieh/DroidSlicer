package org.droidslicer.graph.entity.resolver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.graph.BehaviorMethod;
import org.droidslicer.graph.entity.ICCParamCallerUnit;
import org.droidslicer.graph.entity.ICCReturnCallerUnit;
import org.droidslicer.graph.entity.UnitEntity;

import com.ibm.wala.ipa.callgraph.CGNode;

public class UnitEntitiesInfo
{
	private final Map<UnitEntity, Set<BehaviorMethod>> mMethodsMap = new HashMap<UnitEntity, Set<BehaviorMethod>>();
	private final Set<UnitEntity> mUnits = new HashSet<UnitEntity>();
	private final Set<Pair<ICCParamCallerUnit, ICCReturnCallerUnit>> mCall2Rets = new HashSet<Pair<ICCParamCallerUnit, ICCReturnCallerUnit>>();
	private Map<CGNode, Set<BehaviorMethod>> mNode2ReachableMethods;
	public void addUnit(UnitEntity unit, Collection<BehaviorMethod> methods)
	{
		mUnits.add(unit);
		if(methods != null && !methods.isEmpty())
		{
			Set<BehaviorMethod> oldMethods = mMethodsMap.get(unit);
			if(oldMethods == null)
			{
				oldMethods = new HashSet<BehaviorMethod>();
				mMethodsMap.put(unit, oldMethods);
			}
			oldMethods.addAll(methods);
		}
	}
	public Set<BehaviorMethod> getReachableMethodsForNode(CGNode node)
	{
		Set<BehaviorMethod> methods = mNode2ReachableMethods.get(node);
		if(methods == null)
			return Collections.emptySet();
		else
			return methods;
	}
	protected void setNode2ReachableMap(Map<CGNode, Set<BehaviorMethod>> node2ReachableMethods)
	{
		mNode2ReachableMethods = node2ReachableMethods;
	}
	public void addCall2ReturnRelations(Collection<Pair<ICCParamCallerUnit, ICCReturnCallerUnit>> relations)
	{
		mCall2Rets.addAll(relations);
	}
	public Collection<Pair<ICCParamCallerUnit, ICCReturnCallerUnit>> getCall2ReturnRelation()
	{
		return mCall2Rets;
	}
	public Collection<UnitEntity> getUnits()
	{
		return mUnits;
	}
	public Collection<BehaviorMethod> getReachableMethods(UnitEntity unit)
	{
		Set<BehaviorMethod> methods = mMethodsMap.get(unit);
		if(methods == null)
			return Collections.emptySet();
		else
			return methods;
	}
}
