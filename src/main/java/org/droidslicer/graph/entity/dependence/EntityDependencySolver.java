package org.droidslicer.graph.entity.dependence;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.graph.BehaviorMethod;
import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.IStatementFlowUnit;
import org.droidslicer.graph.entity.resolver.UnitEntitiesInfo;
import org.droidslicer.ifds.BypassFlowFunctions;
import org.droidslicer.ifds.CallRecords;
import org.droidslicer.ifds.DependencySolver;
import org.droidslicer.ifds.DependencySolver.DependType;
import org.droidslicer.ifds.StatementSetPredicate;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.solver.ValueSourceFunctions;
import org.droidslicer.value.solver.ValueSourceSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.SparseIntSet;

public class EntityDependencySolver 
{
	private final static Logger mLogger = LoggerFactory.getLogger(EntityDependencySolver.class);
	public class HeapDependsTransformer implements DependencySolver.FactsTransformer
	{
		protected final Map<Statement, Pair<MutableIntSet, Integer>> mStm2Facts = 
				new LinkedHashMap<Statement, Pair<MutableIntSet, Integer>>();
		protected final List<Statement> mStmList = new ArrayList<Statement>();
		public void clear()
		{
			mStm2Facts.clear();
			mStmList.clear();
		}
		public IntSet getFactsForStatement(Statement stm)
		{
			Pair<MutableIntSet, Integer> pair = mStm2Facts.get(stm);
			if(pair == null)
				return EmptyIntSet.instance;
			else
				return pair.getLeft();
		}
		public Statement getStatementForFact(int fact)
		{
			return mStmList.get(fact - getFirstHeapFact());
		}
		public boolean isHeapFact(int fact)
		{
			int id = fact - getFirstHeapFact();
			return id >= 0 && id < mStmList.size();
		}
		public int addDepend(Statement stm, IntSet facts)
		{
			Pair<MutableIntSet, Integer> pair = mStm2Facts.get(stm);
			int dependFact;
			MutableIntSet oldFacts;
			if(pair == null)
			{
				dependFact = getFirstHeapFact() + mStmList.size();
				oldFacts = MutableSparseIntSet.makeEmpty();
				mStm2Facts.put(stm, Pair.of(oldFacts, new Integer(dependFact)));
				mStmList.add(stm);
			}
			else
			{
				dependFact = pair.getRight();
				oldFacts = pair.getLeft();
			}
			oldFacts.addAll(facts);
			return dependFact;
		}
		@Override
		public IntSet transform(DependType dependType, Statement srcStm, Statement dstStm,
				IntSet facts)
		{
			if(facts.isEmpty())
				return EmptyIntSet.instance;
			
			// Find the ICC entry points that can reach the two statements
			Set<BehaviorMethod> srcMethods = mUnitsInfo.getReachableMethodsForNode(srcStm.getNode());
			Set<ComponentUnit> dstComps = new HashSet<ComponentUnit>();
			{
				Set<BehaviorMethod> dstMethods = mUnitsInfo.getReachableMethodsForNode(dstStm.getNode());
				for(BehaviorMethod dstMethod : dstMethods)
				{
					dstComps.add(dstMethod.getComponent());
				}
			}
			
			// TODO To reduce false-positive, we currently only allow dependency via heap within a same component.
			// Thus, we check if there is a component that can reach both the statements. We should do better.
			boolean hasSameComponent = false;
			for(BehaviorMethod srcMethod : srcMethods)
			{
				if(dstComps.contains(srcMethod.getComponent()))
				{
					hasSameComponent = true;
					break;
				}
			}
			if(!hasSameComponent)
				return EmptyIntSet.instance;
			switch(dependType)
			{
			case CLASS_FIELD:
				{
					int dependFact = addDepend(dstStm, facts);
					return SparseIntSet.singleton(dependFact);
				}
			default:
				return facts;
			}
		}
	}
	public static class Dependence
	{
		private final IStatementFlowUnit mUnit;
		private final Set<CGNode> mChannelNodes = new LinkedHashSet<CGNode>();
		public Dependence(IStatementFlowUnit unit)
		{
			if(unit == null)
				throw new IllegalArgumentException();
			mUnit = unit;
		}
		public void addChannelNodes(Set<CGNode> nodes)
		{
			mChannelNodes.addAll(nodes);
		}
		public void addChannelNode(CGNode node)
		{
			mChannelNodes.add(node);
		}
		public IStatementFlowUnit getUnit()
		{
			return mUnit;
		}
		public Set<CGNode> getChannelNodes()
		{
			return mChannelNodes;
		}
	}
	private static class FilteredExtendedDependsInstructionVisitor extends DependencySolver.ExtendedDependsInstructionVisitor
	{
		public FilteredExtendedDependsInstructionVisitor(DependencySolver solver)
		{
			super(solver);
		}
		@Override
		public void visitPut(SSAPutInstruction putInst)
		{
			TypeReference typeRef = putInst.getDeclaredField().getDeclaringClass();
			IClassHierarchy cha = mSolver.getAnalysisContext().getClassHierarchy();
			IClass type = cha.lookupClass(typeRef);
			if(type == null)
				return;
			ClassLoaderReference classLoaderRef = type.getClassLoader().getReference();
			
			// TODO To reduce false-positive, we exclude the dependency via class fields of library
			// classes. The impreciseness is caused by the imprecise pointer analysis and call graph
			// construction. For example, for different "this" objects, but same method, only a single
			// call graph node is created. 
			if(!classLoaderRef.equals(ClassLoaderReference.Application))
				return;
			super.visitPut(putInst);
		}
	}
	private static class DependencySolvingEntry
	{
		private final Map<Statement, MutableIntSet> mSeedFacts = new LinkedHashMap<Statement, MutableIntSet>();
		protected final LinkedHashSet<Statement> mPendingSeedsQue = new LinkedHashSet<Statement>();	
		private CallRecords mCallRecords = null;
		public DependencySolvingEntry()
		{}
		public void addSeedFacts(Statement seed, IntSet facts)
		{
			MutableIntSet oldFacts = mSeedFacts.get(seed);
			if(oldFacts == null)
			{
				oldFacts = new BitVectorIntSet();
				mSeedFacts.put(seed, oldFacts);
			}
			boolean changed = oldFacts.addAll(facts);
			if(changed)
				mPendingSeedsQue.add(seed);
		}
		public boolean hasPendingSeeds()
		{
			return !mPendingSeedsQue.isEmpty();
		}
		public Statement pollPendingSeed()
		{
			Iterator<Statement> itr = mPendingSeedsQue.iterator();
			Statement seed = itr.next();
			itr.remove();
			return seed;
		}
		public IntSet getSeedFacts(Statement seed)
		{
			return mSeedFacts.get(seed);
		}
		public void setCallRecords(CallRecords callRecords)
		{
			mCallRecords = callRecords;
		}
		public CallRecords getCallRecords()
		{
			return mCallRecords;
		}
	}
	private static final int MAX_DEPENDS_DEPTH = 5;

	// The seeds of the next slicing
	protected final Map<Statement, MutableIntSet> mFacts = new LinkedHashMap<Statement, MutableIntSet>();
	protected final Queue<DependencySolvingEntry> mQue = new ArrayDeque<DependencySolvingEntry>();
	
	// The set of statements used as terminators in the next slicings
	protected final Set<Statement> mTerminators = new HashSet<Statement>();

	protected final List<IStatementFlowUnit> mUnits = new ArrayList<IStatementFlowUnit>();
	protected final AndroidAnalysisContext mAnalysisCtx;
	protected final UnitEntitiesInfo mUnitsInfo;
	private final IClass mCtxClass;
	private final HeapDependsTransformer mHeapDepends = new HeapDependsTransformer();
		
	public EntityDependencySolver(AndroidAnalysisContext analysisCtx, UnitEntitiesInfo unitsInfo)
	{
		mAnalysisCtx = analysisCtx;
		mUnitsInfo = unitsInfo;
		IClassHierarchy cha = analysisCtx.getClassHierarchy();
		mCtxClass = cha.lookupClass(TypeId.ANDROID_CONTEXT.getTypeReference());
		if(mCtxClass == null)
			throw new IllegalArgumentException("Fail to find " + TypeId.ANDROID_CONTEXT.getTypeReference().getName() + " in class hierarchy");
	}
	protected boolean addFacts(Statement stm, IntSet facts)
	{
		MutableIntSet oldFacts = mFacts.get(stm);
		if(oldFacts == null)
		{
			oldFacts = new BitVectorIntSet();
			mFacts.put(stm, oldFacts);
		}
		return oldFacts.addAll(facts);
	}
	public void addEntity(IStatementFlowUnit entity)
	{
		mUnits.add(entity);
	}
	public Collection<IStatementFlowUnit> getEntities()
	{
		return mUnits;
	}
	protected int getFirstNormalFact()
	{
		return 1;
	}
	protected int getFirstHeapFact()
	{
		return getFirstNormalFact() + mUnits.size(); 
	}
	public boolean isNormalFact(int fact)
	{
		int id = fact - getFirstNormalFact();
		return id >= 0 && id < mUnits.size();
	}
	public HeapDependsTransformer getHeapDepends()
	{
		return mHeapDepends;
	}
	public IStatementFlowUnit getUnitFromFact(int fact)
	{
		return mUnits.get(fact - getFirstNormalFact());
	}
	public Map<Statement, ? extends IntSet> getStatementFacts()
	{
		return mFacts;
	}
	public IntSet getStatementFacts(Statement stm)
	{
		IntSet facts = mFacts.get(stm);
		if(facts == null)
			return EmptyIntSet.instance;
		else
			return facts;
	}
	private void makeDepends(Map<IStatementFlowUnit, Dependence> result, Set<CGNode> inheritedChannels, IntSet facts, int depth)
	{
		for(IntIterator itr = facts.intIterator(); itr.hasNext(); )
		{
			int fact = itr.next();
			if(isNormalFact(fact))
			{
				IStatementFlowUnit unit = getUnitFromFact(fact);
				Dependence entry = result.get(unit);
				if(entry == null)
				{
					entry = new Dependence(unit);
					result.put(unit, entry);
				}
				entry.addChannelNodes(inheritedChannels);
			}
			else if(mHeapDepends.isHeapFact(fact))
			{
				if(depth < MAX_DEPENDS_DEPTH)
				{
					Statement dependStm = mHeapDepends.getStatementForFact(fact);
					IntSet dependFacts = mHeapDepends.getFactsForStatement(dependStm);
					Set<CGNode> dependsNodes = new LinkedHashSet<CGNode>(inheritedChannels);
					dependsNodes.add(dependStm.getNode());
					makeDepends(result, dependsNodes, dependFacts, depth + 1);
				}
			}
		}
	}
	public Collection<Dependence> getDepends(IntSet facts)
	{
		Map<IStatementFlowUnit, Dependence> result = new HashMap<IStatementFlowUnit, Dependence>();
		makeDepends(result, Collections.<CGNode>emptySet(), facts, 0);
		return result.values();
	}
	protected void initialize()
		throws CancelException
	{
		mTerminators.clear();
		mQue.clear();
		mHeapDepends.clear();
		mFacts.clear();
		SDG sdg = mAnalysisCtx.getSDG();
		Map<Statement, MutableIntSet> seeds = new LinkedHashMap<Statement, MutableIntSet>();
		for(int idx = 0; idx < mUnits.size(); ++idx)
		{
			IStatementFlowUnit unit = mUnits.get(idx);
			int fact = idx + getFirstNormalFact();
			Collection<Statement> oStms = unit.getOutflowStatements();
			for(Statement oStm : oStms)
			{
				if(sdg.containsNode(oStm))
				{
					MutableIntSet oldFacts = seeds.get(oStm);
					if(oldFacts == null)
					{
						oldFacts = MutableSparseIntSet.makeEmpty();
						seeds.put(oStm, oldFacts);
					}
					oldFacts.add(fact);
					mTerminators.add(oStm);
				}
			}
			if(!unit.isAllowFlowThrough())
			{
				Collection<Statement> iStms = unit.getInflowStatements();
				for(Statement iStm : iStms)
				{
					if(sdg.containsNode(iStm))
						mTerminators.add(iStm);
				}
			}
		}
		for(Map.Entry<Statement, MutableIntSet> entry : seeds.entrySet())
		{
			Statement seed = entry.getKey();
			IntSet facts = entry.getValue();
			DependencySolvingEntry dependEntry = new DependencySolvingEntry();
			dependEntry.setCallRecords(new CallRecords(mAnalysisCtx.getCallGraph()));
			dependEntry.addSeedFacts(seed, facts);
			mQue.add(dependEntry);
		}
	}
	protected void addCallerSeeds(DependencySolvingEntry dependEntry, Statement callerStm, IntSet facts, ProgressMonitor monitor)
		throws CancelException
	{
		// To prevent over-taint, currently we only consider parameter dependency 
		try
		{
			if(monitor.isCanceled())
				throw CancelException.make("Operation canceled");
			monitor.beginTask("Preparing seeds for callers", 1000);
			mLogger.debug("Adding caller seeds for caller {} with facts {}", callerStm, facts);
			if(!callerStm.getKind().equals(Statement.Kind.PARAM_CALLER) || facts.isEmpty())
				return;
			ParamCaller paramCaller = (ParamCaller)callerStm;
			CGNode callerNode = callerStm.getNode();
			int instIdx = paramCaller.getInstructionIndex();
			IR callerIr = callerNode.getIR();
			if(callerIr == null)
				return;
			SSAInstruction[] insts = callerIr.getInstructions();
			SSAInstruction inst = insts[instIdx];
			if(!(inst instanceof SSAAbstractInvokeInstruction))
				throw new IllegalArgumentException();
			SSAAbstractInvokeInstruction invokeInst = (SSAAbstractInvokeInstruction)inst;
						
			int nParams = invokeInst.getNumberOfParameters();
			MethodReference declaredTarget = invokeInst.getDeclaredTarget();
			TypeReference declaredType = declaredTarget.getDeclaringClass();
			
			int[] taintParam = new int[nParams];
			Arrays.fill(taintParam, -1);
			
			// If it is the parameter that it tainted, prevent from tainting it again.
			int excludeValNum = paramCaller.getValueNumber();					
			
			// If the invocation is static
			if(invokeInst.isStatic())
			{
				//TypeReference declaredClassRef = declaredTarget.getDeclaringClass();
				switch(MethodId.getMethodId(declaredTarget))
				{
				case SYSTEM_ARRAYCOPY:
					if(paramCaller.getValueNumber() == invokeInst.getUse(0))
						taintParam[2] = invokeInst.getUse(2);
					break;
				default:
					for(int paramIdx = 0; paramIdx < taintParam.length; ++paramIdx)
					{
						TypeReference paramType = declaredTarget.getParameterType(paramIdx);
						
						// Only taint the parameter if it is an array
						if(paramType.isArrayType())
							taintParam[paramIdx] = invokeInst.getUse(paramIdx);
					}
					break;
				}
			}
			
			// Else, try to taint the receiver or the parameters
			else  
			{
				
				if(nParams <= 0)
				{
					mLogger.warn("Non-static invocation instruction with 0 parameter");
					return;
				}
	
				int paramIdx = 0;
				
				String methodName = declaredTarget.getName().toString();
				
				// Taint the receiver of the invocation (the object)
				{
					
					// Use some heuristic do decide whether we should taint the receiver
					// TODO Maybe we can do it better
					int receiver = invokeInst.getReceiver();
					taintParam[paramIdx] = -1;
					if(excludeValNum != receiver && (
						methodName.startsWith("insert") ||
						methodName.startsWith("add") ||
						methodName.startsWith("put") ||
						methodName.startsWith("set") || 
						methodName.startsWith("append") || 
						declaredTarget.getName().equals(MethodReference.initAtom)))
					{
						IClassHierarchy cha = mAnalysisCtx.getClassHierarchy();
						IClass targetType = cha.lookupClass(declaredType);
						if(targetType != null && !cha.isSubclassOf(targetType, mCtxClass))
							taintParam[paramIdx] = receiver;
					}
					++paramIdx;
				}
				
				// Taint the parameters
				if(methodName.startsWith("get"))
				{
					for(int i = 0; paramIdx < taintParam.length; ++paramIdx, ++i)
					{
						TypeReference paramType = declaredTarget.getParameterType(i);
						
						// Only taint the parameter if it is an array
						if(paramType.isArrayType())
							taintParam[paramIdx] = invokeInst.getUse(paramIdx);
					}
				}
			}
			for(int i = 0; i < taintParam.length; ++i)
			{
				int valNum = taintParam[i];
				
				// If this parameter doesn't need to be tainted
				if(valNum < 0 || excludeValNum == valNum)
					continue;
				ParamCaller paramStm = new ParamCaller(callerNode, instIdx, valNum);
				addValueSourceSeeds(dependEntry, paramStm, facts, new SubProgressMonitor(monitor, 1000 / taintParam.length));
			}
		}
		finally
		{
			monitor.done();
		}
	}
	protected void addValueSourceSeeds(DependencySolvingEntry dependEntry, Statement stm, IntSet facts, ProgressMonitor monitor)
		throws CancelException
	{
		if(monitor.isCanceled())
			throw CancelException.make("Operation canceled");
		ValueSourceSolver valSrcSolver = new ValueSourceSolver(mAnalysisCtx, new ValueSourceFunctions()
		{
			@Override
			public IUnaryFlowFunction getCallFlowFunction(Statement caller,
					Statement callee, Statement ret)
			{
				if(caller instanceof NormalReturnCaller)
				{
					NormalReturnCaller retCaller = (NormalReturnCaller)caller;
					CGNode calleeNode = callee.getNode();
					IMethod calleeMethod = calleeNode.getMethod();
					IClass calleeClass = calleeMethod.getDeclaringClass();
					ClassLoaderReference calleeClassLoaderRef = calleeClass.getClassLoader().getReference();
					if(!calleeClassLoaderRef.equals(ClassLoaderReference.Application))
						return new ValueSourceFunctions.KillReportFlowFunction(retCaller, callee);
					else
						return IdentityFlowFunction.identity();
				}
				else
					return KillEverything.singleton();
			}
			@Override
			public IFlowFunction getReturnFlowFunction(Statement call, Statement exit,
					Statement ret)
			{
				return getUnbalancedReturnFlowFunction(exit, ret);
			}
			@Override
			public IFlowFunction getUnbalancedReturnFlowFunction(Statement exit,
					Statement ret)
			{
				IMethod retMethod = ret.getNode().getMethod();
				if(retMethod.isSynthetic())
					return KillEverything.singleton();
				ClassLoaderReference retClassLoaderRef = retMethod.getDeclaringClass().getClassLoader().getReference();
				if(!retClassLoaderRef.equals(ClassLoaderReference.Application))
					return KillEverything.singleton();
				return IdentityFlowFunction.identity();
			}
		}, new CallRecords(dependEntry.getCallRecords()));
		
		// Run the value solver
		{
			valSrcSolver.setIsRecordCalls(true);
			valSrcSolver.solve(stm, monitor);
			
			// For example, the value is from the return value of a method, then
			// we would like to record the call records, so that we know where we 
			// should return when doing forward slicing from the source.
			dependEntry.setCallRecords(valSrcSolver.getCallRecords());
		}
		
		IntSet paramFacts = valSrcSolver.getStatementFacts(stm);
		for(Iterator<Pair<Statement, Statement>> itr = valSrcSolver.getCallSources(paramFacts); 
			itr.hasNext(); )
		{
			Pair<Statement, Statement> call = itr.next();
			Statement src = call.getLeft();
			switch(src.getKind())
			{
			case NORMAL_RET_CALLER:
				dependEntry.addSeedFacts(src, facts);
				break;
			default:
				break;
			}
		}
		for(Iterator<NormalStatement> itr = valSrcSolver.getAllocSources(paramFacts);
				itr.hasNext();)
		{
			NormalStatement normalStm = itr.next();
			SSAInstruction newInst = normalStm.getInstruction();
			if(newInst instanceof SSANewInstruction)
			{
				dependEntry.addSeedFacts(normalStm, facts);
			}
		}
	}
	protected void processDependEntry(DependencySolvingEntry dependEntry, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Solving dependency entry", 100);
			while(dependEntry.hasPendingSeeds())
			{
				Statement seed = dependEntry.pollPendingSeed();
				IntSet facts = dependEntry.getSeedFacts(seed);
				processSeed(dependEntry, seed, facts, new SubProgressMonitor(monitor, 10));
			}
		}
		finally
		{
			monitor.done();
		}
	}
	protected void processSeed(DependencySolvingEntry dependEntry, Statement seed, IntSet facts, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			if(monitor.isCanceled())
				throw CancelException.make("Operation canceled");
			monitor.beginTask("Calculating dependencies", 100);
			mLogger.debug("Calculating dependencies from seed {}, with facts {}", seed, facts);
			Map<Statement, ? extends IntSet> reachedStms;
			Map<Statement, Set<Statement>>  bypassedCallers;
			
			// Be sure to release reference to SDG as soon as possible
			{
				Map<Statement, ? extends IntSet> seedFacts = Collections.singletonMap(seed, facts);
				BypassFlowFunctions flowFuncts = new EntityDependencyFlowFunctions(mAnalysisCtx, new StatementSetPredicate(mTerminators), 0);
				DependencySolver dependSolver = new DependencySolver(mAnalysisCtx, flowFuncts);
				dependSolver.setExtendedFactsTransformer(mHeapDepends);
				dependSolver.setExtendedDependsInstructionVisitor(new FilteredExtendedDependsInstructionVisitor(dependSolver));
				//dependSolver.setMaxRounds(MAX_DEPENDS_DEPTH);
				//mLogger.info("DEBUG dependency solving start");
				dependSolver.solve(seedFacts, dependEntry.getCallRecords(), new SubProgressMonitor(monitor, 30));
				//mLogger.info("DEBUG dependency solving finished");
				dependEntry.setCallRecords(dependSolver.getCallRecords());
				reachedStms = dependSolver.getReachedStatements();
				bypassedCallers = flowFuncts.getBypassedCallers();
			}
			monitor.worked(20);
			
			CallGraph cg = mAnalysisCtx.getCallGraph();
			IClassHierarchy cha = mAnalysisCtx.getClassHierarchy();
			for(Map.Entry<Statement, ? extends IntSet> entry : reachedStms.entrySet())
			{
				Statement reachedStm = entry.getKey();
				IntSet reachedFacts = entry.getValue();
				if(reachedFacts.isEmpty())
					continue;
				addFacts(reachedStm, reachedFacts);
				switch(reachedStm.getKind())
				{
				case PARAM_CALLER:
					
					// If the statement is a bypassed parameter caller statement
					if(bypassedCallers.containsKey(reachedStm))
						break;
					ParamCaller callerStm = (ParamCaller)reachedStm;
					SSAAbstractInvokeInstruction invokeInst = callerStm.getInstruction();
					CallSiteReference callSiteRef = invokeInst.getCallSite();
					CGNode callerNode = callerStm.getNode();
					
					// If the caller statement doesn't have a target
					if(!cg.getPossibleTargets(callerNode, callSiteRef).isEmpty())
						break;
					IMethod method = cha.resolveMethod(callSiteRef.getDeclaredTarget());
					if(method == null)
						break;
					ClassLoaderReference classLoaderRef = method.getDeclaringClass().getClassLoader().getReference();
					if(!classLoaderRef.equals(ClassLoaderReference.Primordial))
						break;
					addCallerSeeds(dependEntry, reachedStm, reachedFacts, new SubProgressMonitor(monitor, 10));
					break;
				default:
					break;
				}
			}
			for(Statement caller : bypassedCallers.keySet())
			{
				IntSet reachedFacts = reachedStms.get(caller);
				if(reachedFacts == null || reachedFacts.isEmpty())
					continue;
				addCallerSeeds(dependEntry, caller, reachedFacts, new SubProgressMonitor(monitor, 10));
			}
		}
		finally
		{
			monitor.done();			
		}
	}
	public void solve(ProgressMonitor monitor) throws CancelException
	{
		mLogger.debug("======== Start solving entity dependencies ========");
		monitor.beginTask("Solving entity dependencies", 1000);
		try
		{
			initialize();
			while(!mQue.isEmpty() && !monitor.isCanceled())
			{
				DependencySolvingEntry dependEntry = mQue.poll();
				processDependEntry(dependEntry, new SubProgressMonitor(monitor, 10));
			}
			if(monitor.isCanceled())
				throw CancelException.make("Operation canceled");
			if(mLogger.isDebugEnabled())
			{
				StringBuilder builder = new StringBuilder();
				builder.append("Entities: ");
				for(IStatementFlowUnit entity : mUnits)
				{
					builder.append('\n');
					builder.append(entity.toString());
				}
				mLogger.debug("{}", builder.toString());
			}
			mLogger.debug("===================================================");
		}
		finally
		{
			mQue.clear();
			monitor.done();
		}
	}
}

