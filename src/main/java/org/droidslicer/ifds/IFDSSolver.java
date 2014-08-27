package org.droidslicer.ifds;

import heros.EdgeFunction;
import heros.EdgeFunctions;
import heros.FlowFunctions;
import heros.IDETabulationProblem;
import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.JoinLattice;
import heros.edgefunc.AllBottom;
import heros.edgefunc.AllTop;
import heros.edgefunc.EdgeIdentity;
import heros.solver.IDESolver;
import heros.solver.PathEdge;

import java.util.Map;
import java.util.Set;

import org.droidslicer.graph.BehaviorSupergraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;

/**
 * It is a clone of heros.solver.IFDSSolver to provide the interface for obtaining the reached nodes.
 * 
 * @param <N> The type of nodes in the interprocedural control-flow graph. Typically soot.Unit.
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 * @param <M> The type of objects used to represent methods. Typically soot.SootMethod.
 * @param <I> The type of inter-procedural control-flow graph being used.
 * @see IFDSTabulationProblem
 */
public class IFDSSolver<N,D,M,I extends InterproceduralCFG<N, M>> extends IDESolver<N,D,M,IFDSSolver.BinaryDomain,I> 
{
	private final static Logger mLogger = LoggerFactory.getLogger(IFDSSolver.class);
	protected static enum BinaryDomain { TOP,BOTTOM } 
	private int mMaxPropagation = -1;
	private long mLastRecordedProgCount = 0; // debug
	
	private final static EdgeFunction<BinaryDomain> ALL_BOTTOM = new AllBottom<BinaryDomain>(BinaryDomain.BOTTOM);
	public IFDSSolver(final IFDSTabulationProblem<N,D,M,I> ifdsProblem)
	{
		this(ifdsProblem, null);
	}
	/**
	 * Creates a solver for the given problem. The solver must then be started by calling
	 * {@link #solve()}.
	 */
	public IFDSSolver(final IFDSTabulationProblem<N,D,M,I> ifdsProblem, @SuppressWarnings("rawtypes") CacheBuilder flowFunctionCacheBuilder)
	{
		super(new IDETabulationProblem<N,D,M,BinaryDomain,I>() {

			public FlowFunctions<N,D,M> flowFunctions() {
				return ifdsProblem.flowFunctions();
			}

			public I interproceduralCFG() {
				return ifdsProblem.interproceduralCFG();
			}

			public Map<N,Set<D>> initialSeeds() {
				return ifdsProblem.initialSeeds();
			}

			public D zeroValue() {
				return ifdsProblem.zeroValue();
			}

			public EdgeFunctions<N,D,M,BinaryDomain> edgeFunctions() {
				return new IFDSEdgeFunctions();
			}

			public JoinLattice<BinaryDomain> joinLattice() {
				return new JoinLattice<BinaryDomain>() {

					public BinaryDomain topElement() {
						return BinaryDomain.TOP;
					}

					public BinaryDomain bottomElement() {
						return BinaryDomain.BOTTOM;
					}

					public BinaryDomain join(BinaryDomain left, BinaryDomain right) {
						// TODO In the original code of heros.solver.IFDSSolver, the return 
						// value is dependent on the arguments. However, I found that it would make the 
						// result incorrect. It would make some of the nodes not have the data flow facts
						// stored in the table even if they do reach the node during computation. 
						// This problem seems to be especially obvious for data flow facts through 
						// unbalanced return-site.
						// Check if it is really a bug.
						return BinaryDomain.BOTTOM;
					}
				};
			}

			@Override
			public EdgeFunction<BinaryDomain> allTopFunction() {
				return new AllTop<BinaryDomain>(BinaryDomain.TOP);
			}
			
			@Override
			public boolean followReturnsPastSeeds() {
				return ifdsProblem.followReturnsPastSeeds();
			}
			
			@Override
			public boolean autoAddZero() {
				return ifdsProblem.autoAddZero();
			}
			
			@Override
			public int numThreads() {
				return ifdsProblem.numThreads();
			}
			
			@Override
			public boolean computeValues() {
				return ifdsProblem.computeValues();
			}
			
			class IFDSEdgeFunctions implements EdgeFunctions<N,D,M,BinaryDomain> {
		
				public EdgeFunction<BinaryDomain> getNormalEdgeFunction(N src,D srcNode,N tgt,D tgtNode) {
					if(srcNode==ifdsProblem.zeroValue()) return ALL_BOTTOM;
					return EdgeIdentity.v(); 
				}
		
				public EdgeFunction<BinaryDomain> getCallEdgeFunction(N callStmt,D srcNode,M destinationMethod,D destNode) {
					if(srcNode==ifdsProblem.zeroValue()) return ALL_BOTTOM;
					return EdgeIdentity.v(); 
				}
		
				public EdgeFunction<BinaryDomain> getReturnEdgeFunction(N callSite, M calleeMethod,N exitStmt,D exitNode,N returnSite,D retNode) {
					if(exitNode==ifdsProblem.zeroValue()) return ALL_BOTTOM;
					return EdgeIdentity.v(); 
				}
		
				public EdgeFunction<BinaryDomain> getCallToReturnEdgeFunction(N callStmt,D callNode,N returnSite,D returnSideNode) {
					if(callNode==ifdsProblem.zeroValue()) return ALL_BOTTOM;
					return EdgeIdentity.v(); 
				}
			}

			}, flowFunctionCacheBuilder, null);
	}
	
	public void setMaxPropagationCount(int val)
	{
		mMaxPropagation = val;
	}
	public int getMaxPropagationCount()
	{
		return mMaxPropagation;
	}
	/**
	 * Returns the set of facts that hold at the given statement.
	 */
	public Set<D> ifdsResultsAt(N statement) {
		return resultsAt(statement).keySet();
	}
	public Set<N> getReachedNodes()
	{
		return val.rowKeySet();
	}
	@Override
	protected void scheduleEdgeProcessing(PathEdge<N,D> edge)
	{
		if(mMaxPropagation >= 0 && propagationCount > mMaxPropagation)
			return;

		if(mLogger.isDebugEnabled())
    	{
			if(propagationCount - mLastRecordedProgCount > 5000)
			{
				mLastRecordedProgCount = propagationCount;
	    		mLogger.debug("propagation count: {}, incoming: # rows: {}, # cols: {}", propagationCount, incoming.rowKeySet().size(), incoming.columnKeySet().size());
	    		{
	    			int size = 0;
	    			for(Map<N, Set<D>> map : incoming.values())
					{
	    				for(Set<D> set : map.values())
	    				{
	    					size += set.size();
	    				}
					}
	    			mLogger.debug("Total size of all sets in the values of incoming: {}", size);
	    		}
	    		mLogger.debug("val: # rows: {}, # cols: {}", val.rowKeySet().size(), val.columnKeySet().size());
	    		mLogger.debug("Propagation count: {}", propagationCount);
	    		if(icfg instanceof BehaviorSupergraph)
	    		{
	    			BehaviorSupergraph supergraph = (BehaviorSupergraph)icfg;
	    			supergraph.logCacheStatistic();
	    		}
			}
    	}
		super.scheduleEdgeProcessing(edge);
    }
}