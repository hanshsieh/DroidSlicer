package org.droidslicer.analysis;

import java.util.Collection;
import java.util.Iterator;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.ifds.CallRecords.CallSite;

import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationProblem;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationSolver;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

public class RecordCallTabulationSolver<F> extends TabulationSolver<Statement, PDG, F>
{
	protected final CallRecords mCallRecords;
	private boolean mRecordCalls = true;
	private final Collection<Pair<Statement, Integer>> mUnbalancedSeeds = HashSetFactory.make();
	private int mMaxHeapPropagate = -1;
	private int mNumHeapPropagate = 0;
	
	protected RecordCallTabulationSolver(PartiallyBalancedTabulationProblem<Statement, PDG, F> problem, IProgressMonitor monitor, CallRecords callRecords)
	{
		super(problem, monitor);
		if(callRecords == null)
			throw new IllegalArgumentException();
		mCallRecords = callRecords;
	}
	public static <F> RecordCallTabulationSolver<F> create(
			PartiallyBalancedTabulationProblem<Statement, PDG, F> problem, 
			CallRecords oldCallRecords,
			IProgressMonitor monitor)
	{
		CallRecords callRecords = new CallRecords(oldCallRecords);
		RecordCallTabulationSolver<F> result = new RecordCallTabulationSolver<F>(problem, monitor, callRecords);
		return result;
	}
	public void setRecordCalls(boolean val)
	{
		mRecordCalls = val;
	}
	public boolean isRecordCalls()
	{
		return mRecordCalls;
	}
	public void setMaxHeapPropagate(int val)
	{
		mMaxHeapPropagate = val;
	}
	public int getMaxHeapPropagate()
	{
		return mMaxHeapPropagate;
	}
	public int getNumHeapPropagate()
	{
		return mNumHeapPropagate;
	}
	protected void addCallFlow(Statement callNode, Statement callee, int resultFact)
	{
		if(!(callNode instanceof StatementWithInstructionIndex))
			return;
		StatementWithInstructionIndex stmWithIdx = (StatementWithInstructionIndex)callNode;
		mCallRecords.addCall(callNode.getNode(), stmWithIdx.getInstructionIndex(), callee.getNode());
	}
	
	@Override
	protected void initialize()
	{
		mNumHeapPropagate = 0;
		super.initialize();
	}
	
	@Override
	protected void recordCall(Statement callNode, Statement callee, int resultFact, boolean gotReuse)
	{
		super.recordCall(callNode, callee, resultFact, gotReuse);
		if(mRecordCalls)
			addCallFlow(callNode, callee, resultFact);
	}
	public CallRecords getCallRecords()
	{
		return mCallRecords;
	}
	private void proUnbalancedReturn(IFlowFunction retf, Statement retSite, int fact)
	{
		PartiallyBalancedTabulationProblem<Statement, PDG, F> problem = (PartiallyBalancedTabulationProblem<Statement, PDG, F>) getProblem();
		if (retf instanceof IUnaryFlowFunction)
		{
			IUnaryFlowFunction uf = (IUnaryFlowFunction)retf;
			IntSet facts = uf.getTargets(fact);
			if(facts != null)
			{
				for (IntIterator it4 = facts.intIterator(); it4.hasNext();)
				{
					int d3 = it4.next();
					// d3 would be reached if we ignored parentheses. use it as a new seed.
					Statement fakeEntry = problem.getFakeEntry(retSite);
					PathEdge<Statement> seed = PathEdge.createPathEdge(fakeEntry, d3, retSite, d3);
					addSeed(seed);
				}
			}
		}
		else
			throw new RuntimeException("Partially balanced logic not supported for binary return flow functions");
	}

	@Override
	protected void tendToSoftCaches()
	{
		/*if(mWipeCount >= WIPE_SOFT_CACHE_INTERVAL)
		{
			mWipeCount = 0;
			//mLogger.debug("Clearing cache");
			ReferenceCleanser.clearSoftCaches();
			TabulationProblem<Statement, PDG, F> problem = getProblem();
			ISupergraph<Statement, PDG> supergraph = problem.getSupergraph();
			if(supergraph instanceof SDGSupergraph)
			{
				SDGSupergraph sdgSupergraph = (SDGSupergraph)supergraph;
				ISDG sdg = sdgSupergraph.getSDG();
				if(sdg instanceof SDG)
				{
					IClassHierarchy cha = ((SDG)sdg).getClassHierarchy();
					for (IClass klass : cha)
					{
						if (klass instanceof ShrikeClass)
						{
							ShrikeClass c = (ShrikeClass) klass;
							c.clearSoftCaches();
						}
						else
						{
							for (IMethod method : klass.getDeclaredMethods())
							{
								if (method instanceof ShrikeCTMethod)
								{
									((ShrikeCTMethod)method).clearCaches();
								}
							}
						}
					}
				}
			}
			//mLogger.debug("Cache cleared");
		}
		else
			++mWipeCount;*/
	}
	
	@Override
	protected boolean propagate(Statement entry, int entryFact, Statement target, int targetFact)
	{
		if(mMaxHeapPropagate >= 0)
		{
			switch(target.getKind())
			{
			case HEAP_PARAM_CALLEE:
			case HEAP_PARAM_CALLER:
			case HEAP_RET_CALLEE:
			case HEAP_RET_CALLER:
				if(mNumHeapPropagate >= mMaxHeapPropagate)
					return false;
				++mNumHeapPropagate;
				break;
			default:
				break;
			}
			/*if(mNumHeapPropagate >= mMaxHeapPropagate)
				return false;
			++mNumHeapPropagate;*/
		}
		
		boolean result = super.propagate(entry, entryFact, target, targetFact);
		if (result && wasUsedAsUnbalancedSeed(entry, entryFact) && supergraph.isExit(target))
		{
			// targetFact was reached from an entry seed. if there are any facts which are reachable from targetFact, even without
			// balanced parentheses, we can use these as new seeds.
			CGNode entryNode = entry.getNode();
			CallRecords.CallSites callSites = mCallRecords.getCallSitesTo(entryNode);
			PartiallyBalancedTabulationProblem<Statement, PDG, F> problem = (PartiallyBalancedTabulationProblem<Statement, PDG, F>) getProblem();
			if(callSites == null || callSites.isEmpty())
			{
				for (Iterator<? extends Statement> retSitesItr = supergraph.getSuccNodes(target); retSitesItr.hasNext();)
				{
					Statement retSite = retSitesItr.next();
					IFlowFunction retf = problem.getFunctionMap().getUnbalancedReturnFlowFunction(target, retSite);
					proUnbalancedReturn(retf, retSite, targetFact);
				}				
			}
			else
			{
				for (Iterator<? extends Statement> retSitesItr = supergraph.getSuccNodes(target); retSitesItr.hasNext();)
				{
					Statement retSite = retSitesItr.next();
					if(!(retSite instanceof StatementWithInstructionIndex))
						continue;
					StatementWithInstructionIndex stmWithIdx = (StatementWithInstructionIndex)retSite;
					CallSite callSite = new CallSite(retSite.getNode().getGraphNodeId(), stmWithIdx.getInstructionIndex());
					if(callSites.contains(callSite))
					{
						IFlowFunction retf = problem.getFunctionMap().getReturnFlowFunction(retSite, target, retSite);
						proUnbalancedReturn(retf, retSite, targetFact);
					}
				}
			}
		}
		return result;
	}
	
	// From PartiallyBalancedTabulationSolver
	/*@Override
	  protected boolean propagate(Statement s_p, int i, Statement n, int j) {
	    boolean result = super.propagate(s_p, i, n, j);
	    if (result && wasUsedAsUnbalancedSeed(s_p, i) && supergraph.isExit(n)) {
	      // j was reached from an entry seed. if there are any facts which are reachable from j, even without
	      // balanced parentheses, we can use these as new seeds.
	      for (Iterator<? extends Statement> it2 = supergraph.getSuccNodes(n); it2.hasNext();) {
	    	  Statement retSite = it2.next();
	        PartiallyBalancedTabulationProblem<Statement, PDG, F> problem = (PartiallyBalancedTabulationProblem<Statement, PDG, F>) getProblem();
	        IFlowFunction f = problem.getFunctionMap().getUnbalancedReturnFlowFunction(n, retSite);
	        // for each fact that can be reached by the return flow ...
	        if (f instanceof IUnaryFlowFunction) {
	          IUnaryFlowFunction uf = (IUnaryFlowFunction) f;
	          IntSet facts = uf.getTargets(j);
	          if (facts != null) {
	            for (IntIterator it4 = facts.intIterator(); it4.hasNext();) {
	              int d3 = it4.next();
	              // d3 would be reached if we ignored parentheses. use it as a new seed.
	              Statement fakeEntry = problem.getFakeEntry(retSite);
	              PathEdge<Statement> seed = PathEdge.createPathEdge(fakeEntry, d3, retSite, d3);
	              addSeed(seed);
	            }
	          }
	        } else {
	          Assertions.UNREACHABLE("Partially balanced logic not supported for binary return flow functions");
	        }
	      }
	    }
	    return result;
	  }*/
	@Override
	public void addSeed(PathEdge<Statement> seed)
	{
		if (getSeeds().contains(seed))
		{
			return;
		}
		mUnbalancedSeeds.add(Pair.make(seed.getEntry(), seed.getD1()));
		super.addSeed(seed);
	}
	/**
	* Was the fact number i named at node s_p introduced as an "unbalanced" seed during partial tabulation?
	* If so, any facts "reached" from here can be further propagated with unbalanced parens.
	*/
	private boolean wasUsedAsUnbalancedSeed(Statement s_p, int i)
	{
		return mUnbalancedSeeds.contains(Pair.make(s_p, i));
	}
}
