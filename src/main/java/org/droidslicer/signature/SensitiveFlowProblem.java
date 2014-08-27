package org.droidslicer.signature;

import heros.FlowFunctions;
import heros.template.DefaultIFDSTabulationProblem;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.droidslicer.graph.BehaviorMethod;
import org.droidslicer.graph.BehaviorNode;
import org.droidslicer.graph.BehaviorSupergraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class SensitiveFlowProblem extends DefaultIFDSTabulationProblem<BehaviorNode, Object, BehaviorMethod, BehaviorSupergraph>
{
	private final static Logger mLogger = LoggerFactory.getLogger(SensitiveFlowProblem.class);
	private final Map<BehaviorNode, Set<Object>> mSeeds;
	public SensitiveFlowProblem(BehaviorSupergraph icfg, Map<BehaviorNode, Set<Object>> seeds)
	{
		super(icfg);
		mSeeds = seeds;
	}

	@Override
	public Map<BehaviorNode, Set<Object>> initialSeeds()
	{
		return Maps.transformValues(mSeeds, new Function<Set<Object>, Set<Object>>()
		{
			@Override
			public Set<Object> apply(Set<Object> facts)
			{
				return Sets.union(facts, Collections.singleton(zeroValue()));
			}
		});
	}

	@Override
	protected FlowFunctions<BehaviorNode, Object, BehaviorMethod> createFlowFunctionsFactory()
	{
		return new SensitiveFlowFunctions(zeroValue());
	}

	@Override
	protected Object createZeroValue()
	{
		return new Object();
	}
	@Override
	public boolean followReturnsPastSeeds()
	{
		return true;
	}
	@Override
	public boolean autoAddZero()
	{
		return false;
	}
	@Override
	public int numThreads()
	{
		if(mLogger.isDebugEnabled())
			return 1;
		else
			return Runtime.getRuntime().availableProcessors();
	}
}
