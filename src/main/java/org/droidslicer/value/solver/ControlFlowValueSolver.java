package org.droidslicer.value.solver;

import org.droidslicer.value.ConcreteValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.intset.IntSet;

public abstract class ControlFlowValueSolver extends ValueUsageWalker
{
	private final ConcreteValueSolver mValSolver;
	private final int mMaxDepth;
	public ControlFlowValueSolver(ConcreteValueSolver valSolver, CGNode startNode, int startInstIdx, IntSet valNums,
			EndCriterion endCriterion, int maxDepth)
	{
		super(valSolver.getAnalysisContext().getCallGraph(), startNode, startInstIdx, valNums, endCriterion);
		mValSolver = valSolver;
		mMaxDepth = maxDepth;
	}
	public int getMaxDepth()
	{
		return mMaxDepth;
	}
	public ConcreteValueSolver getValueSolver()
	{
		return mValSolver;
	}
	public abstract ConcreteValue getValue();
}
