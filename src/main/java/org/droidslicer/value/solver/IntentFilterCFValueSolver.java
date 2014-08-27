package org.droidslicer.value.solver;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcatValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;
import org.droidslicer.value.IntentFilterValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.UnknownValue;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.SparseIntSet;

public class IntentFilterCFValueSolver extends ControlFlowValueSolver
{
	protected static class State
	{
		private final Set<ConcreteValue> mActions = new HashSet<ConcreteValue>();
		private final Set<ConcreteValue> mCategories = new HashSet<ConcreteValue>();
		private final Set<ConcreteValue> mDataAuthorities = new HashSet<ConcreteValue>();
		private final Set<ConcreteValue> mDataSchemes = new HashSet<ConcreteValue>();
		private final Set<Pair<ConcreteValue, ConcreteValue>> mDataPaths = new HashSet<Pair<ConcreteValue, ConcreteValue>>();
		private final Set<Pair<ConcreteValue, ConcreteValue>> mDataSsps = new HashSet<Pair<ConcreteValue, ConcreteValue>>();
		private final Set<ConcreteValue> mDataMimeTypes = new HashSet<ConcreteValue>();		
		public State()
		{}
		public State(State that)
		{
			mActions.addAll(that.mActions);
			mCategories.addAll(that.mCategories);
			mDataAuthorities.addAll(that.mDataAuthorities);
			mDataSchemes.addAll(that.mDataSchemes);
			mDataPaths.addAll(that.mDataPaths);
			mDataSsps.addAll(that.mDataSsps);
			mDataMimeTypes.addAll(that.mDataMimeTypes);
		}
		public IntentFilterValue generateValue()
		{
			IntentFilterValue filterVal = new IntentFilterValue();
			for(ConcreteValue action : mActions)
				filterVal.addAction(action);
			for(ConcreteValue cat : mCategories)
				filterVal.addCategory(cat);
			for(ConcreteValue auth : mDataAuthorities)
				filterVal.addDataAuthority(auth);
			for(ConcreteValue scheme : mDataSchemes)
				filterVal.addDataScheme(scheme);
			for(Pair<ConcreteValue, ConcreteValue> pair : mDataPaths)
				filterVal.addDataPath(pair.fst, pair.snd);
			for(Pair<ConcreteValue, ConcreteValue> pair : mDataSsps)
				filterVal.addDataSchemeSpecificPart(pair.fst, pair.snd);
			for(ConcreteValue mimeType : mDataMimeTypes)
				filterVal.addDataMimeType(mimeType);
			return filterVal;
		}
		private ConcreteValue filterValue(ConcreteValue val)
		{
			val = UnknownValue.excludeUnknownValue(NullValue.excludeNullValue(val));
			if(val instanceof UnknownValue)
				return null;
			return val;
		}
		public void addIntentAction(ConcreteValue action)
		{
			if(action == null)
				throw new IllegalArgumentException();
			action = filterValue(action);
			if(action != null)
				mActions.add(action);
		}
		public void addIntentCategory(ConcreteValue cat)
		{
			if(cat == null)
				throw new IllegalArgumentException();
			cat = filterValue(cat);
			if(cat != null)
				mCategories.add(cat);
		}
		public void addIntentAuthority(ConcreteValue auth)
		{
			if(auth == null)
				throw new IllegalArgumentException();
			auth = filterValue(auth);
			if(auth != null)
				mDataAuthorities.add(auth);
		}
		public void addDataScheme(ConcreteValue scheme)
		{
			if(scheme == null)
				throw new IllegalArgumentException();
			scheme = filterValue(scheme);
			if(scheme != null)
				mDataSchemes.add(scheme);
		}
		public void addDataPath(ConcreteValue path, ConcreteValue pathType)
		{
			if(path == null || pathType == null)
				throw new IllegalArgumentException();
			path = filterValue(path);
			if(path != null)
				mDataPaths.add(Pair.make(path, pathType));
		}
		public void addDataSsp(ConcreteValue ssp, ConcreteValue sspType)
		{
			if(ssp == null || sspType == null)
				throw new IllegalArgumentException();
			ssp = filterValue(ssp);
			if(ssp != null)
				mDataSsps.add(Pair.make(ssp, sspType));
		}
		public void addDataMimeType(ConcreteValue mimeType)
		{
			if(mimeType == null)
				throw new IllegalArgumentException();
			mimeType = filterValue(mimeType);
			if(mimeType != null)
				mDataMimeTypes.add(mimeType);
		}
	}
	private final static int MAX_NUM_PATH = 100;
	private int mNumPaths;
	private OrValue mValue = null;
	private CGNode mCurrNode = null;
	
	// Current state
	private Stack<State> mStates = new Stack<State>();
	public IntentFilterCFValueSolver(ConcreteValueSolver valSolver, CGNode startNode, int startInstIdx, int valNum, EndCriterion endCriterion, int maxDepth)
	{
		super(valSolver, startNode, startInstIdx, SparseIntSet.singleton(valNum), endCriterion, maxDepth);
	}
	@Override
	public ConcreteValue getValue()
	{
		if(mValue == null)
			return null;
		else
			return mValue.simplify();
	}
	protected void addPossibleValue(ConcreteValue val)
	{
		if(val != null)
			mValue.addValue(val);
		++mNumPaths;
	}
	@Override
	public void onStartBasicBlock(SSACFG.BasicBlock block, int instIdx)
	{
		State state = mStates.peek();
		State newState = new State(state);
		mStates.push(newState);		
	}
	@Override
	public void onEndBasicBlock(SSACFG.BasicBlock block)
	{
		// Because we currently only handle intra-procedural control 
		// flow, if the call node of the end point of the value 
		// is different from the current node, then we won't reach the end condition.
		// TODO Do better when we can handle inter-procedural control flow
		if(block.isExitBlock())
			addPossibleValue(generateValue());
		mStates.pop();
	}
	@Override
	public void onStartNode(CGNode node)
	{
		mCurrNode = node;
	}
	@Override
	public void onEndNode(CGNode node)
	{
		mCurrNode = null;	
	}
	@Override
	public void onRevisitBasicBlock(BasicBlock block, int instIdx)
	{
		// TODO Maybe we should do better. e.g. Which value may have possibility
		// to be unknown?
		addPossibleValue(generateValue());
	}
	protected IntentFilterValue generateValue()
	{
		State state = mStates.peek();
		IntentFilterValue val = state.generateValue();
		return val;
	}
	@Override
	public void onCutPath(BasicBlock block)
	{
		addPossibleValue(generateValue());
	}
	@Override
	public void onEndCondition(BasicBlock block)
	{
		addPossibleValue(generateValue());
	}
	protected void cloneIntent(State state, ConcreteValue target)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(target);
		while(itr.hasNext())
		{
			ConcreteValue val = itr.next();
			if(val instanceof IntentFilterValue)
			{
				IntentFilterValue filterVal = (IntentFilterValue)val;
				for(Iterator<ConcreteValue> actItr = filterVal.getActions().iterator(); actItr.hasNext(); )
					state.addIntentAction(actItr.next());
				for(Iterator<ConcreteValue> catItr = filterVal.getCategories().iterator(); catItr.hasNext(); )
					state.addIntentCategory(catItr.next());
				for(Iterator<ConcreteValue> authItr = filterVal.getDataAuthorities().iterator(); authItr.hasNext(); )
					state.addIntentAuthority(authItr.next());
				for(Iterator<ConcreteValue> mimeItr = filterVal.getDataMimeTypes().iterator(); mimeItr.hasNext(); )
					state.addDataMimeType(mimeItr.next());
				for(Iterator<Pair<ConcreteValue, ConcreteValue>> pathItr = filterVal.getDataPaths().iterator(); pathItr.hasNext(); )
				{
					Pair<ConcreteValue, ConcreteValue> pair = pathItr.next();
					state.addDataPath(pair.fst, pair.snd);
				}
				for(Iterator<ConcreteValue> schemeItr = filterVal.getDataSchemes().iterator(); schemeItr.hasNext(); )
					state.addDataScheme(schemeItr.next());
				for(Iterator<Pair<ConcreteValue, ConcreteValue>> pathItr = filterVal.getSchemeSpecificParts().iterator(); pathItr.hasNext(); )
				{
					Pair<ConcreteValue, ConcreteValue> pair = pathItr.next();
					state.addDataSsp(pair.fst, pair.snd);
				}
			}
			else
			{
				polluteState(state);
			}
		}
	}
	private void polluteState(State state)
	{
		state.addIntentAction(UnknownValue.getInstance());
		state.addIntentCategory(UnknownValue.getInstance());
		state.addIntentAuthority(UnknownValue.getInstance());
		state.addDataMimeType(UnknownValue.getInstance());
		state.addDataPath(UnknownValue.getInstance(), UnknownValue.getInstance());
		state.addDataScheme(UnknownValue.getInstance());
		state.addDataSsp(UnknownValue.getInstance(), UnknownValue.getInstance());
	}
	@Override
	public void onStart()
	{
		ProgressMonitor monitor = getProgressMonitor();
		monitor.beginTask("Solving Intent value", 1000);
		assert mStates.isEmpty();
		mCurrNode = null;
		mStates.clear();
		mStates.add(new State());
		mValue = new OrValue();
		mNumPaths = 0;
	}
	@Override
	public void onEnd()
	{
		ProgressMonitor monitor = getProgressMonitor();
		monitor.done();
		assert mStates.size() == 1;
		mCurrNode = null;
		mStates.clear();
	}
	@Override
	public void onTerminate(BasicBlock block)
	{
		addPossibleValue(generateValue());
		addPossibleValue(UnknownValue.getInstance());
	}
	@Override
	public void visitInvoke(SSAInvokeInstruction invokeInst) 
	{
		if(invokeInst.isStatic() || getFirstUseIndex() != 0)
		{
			setCutPath(true);
			// TODO Handle the case, for example, the IntentFilter is passed as arguments
			// to other methods
			return;
		}
		if(mNumPaths >= MAX_NUM_PATH)
		{
			setTerminate(true);
			return;
		}
		ConcreteValueSolver valSolver = getValueSolver();
		CallSiteReference callSite = invokeInst.getCallSite();
		int instIdx = getInstructionIndex();
		Set<CGNode> targets = valSolver.getAnalysisContext().getCallGraph().getPossibleTargets(mCurrNode, callSite);
		
		// TODO Currently, we only handle the case there're only one possible target
		if(targets.size() != 1)
		{
			setCutPath(true);
			return;
		}
		ProgressMonitor monitor = getProgressMonitor();
		try
		{
			setCutPath(false);
			CallRecords callRecords = getCallRecords();
			CGNode target = targets.iterator().next();
			IMethod method = target.getMethod();
			int maxDepth = getMaxDepth();
			
			switch(MethodId.getMethodId(method.getReference()))
			{
			case ANDROID_INTENT_FILTER_INIT:
				break;
			case ANDROID_INTENT_FILTER_INIT_STR:
				{
					int actValNum = invokeInst.getUse(1);
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					ConcreteValue actVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, actValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
					mStates.peek().addIntentAction(actVal);
					break;
				}
			case ANDROID_INTENT_FILTER_INIT_STR_STR:
				{
					int actValNum = invokeInst.getUse(1);
					int dataTypeValNum = invokeInst.getUse(2);
					ConcreteValue actVal, dataTypeVal;
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						actVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, actValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
					}
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						dataTypeVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, dataTypeValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
					}
					State state = mStates.peek();
					state.addIntentAction(actVal);
					state.addDataMimeType(dataTypeVal);
					break;
				}
			case ANDROID_INTENT_FILTER_INIT_FILTER:
				{
					int filterValNum = invokeInst.getUse(1);
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					ConcreteValue filterVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, filterValNum), mCurrNode, instIdx, TypeId.ANDROID_INTENT_FILTER.getTypeReference(), callRecords, maxDepth, subMonitor);
					cloneIntent(mStates.peek(), filterVal);
					break;
				}
			case ANDROID_INTENT_FILTER_ADD_ACTION:
				{
					int actValNum = invokeInst.getUse(1);
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					ConcreteValue actVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, actValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
					mStates.peek().addIntentAction(actVal);
					break;
				}
			case ANDROID_INTENT_FILTER_ADD_CATEGORY:
				{
					int catValNum = invokeInst.getUse(1);
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					ConcreteValue catVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, catValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
					mStates.peek().addIntentCategory(catVal);
					break;
				}
			case ANDROID_INTENT_FILTER_ADD_DATA_AUTHORITY:
				{
					int hosthValNum = invokeInst.getUse(1);
					int portValNum = invokeInst.getUse(2);
					ConcreteValue hostVal, portVal;
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						hostVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, hosthValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
					}
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						portVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, portValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
					}
					State state = mStates.peek();
					if(NullValue.isPossibleNull(portVal))
						state.addIntentAuthority(hostVal);
					if(NullValue.isPossibleNotNull(portVal))
						state.addIntentAuthority(new ConcatValue(hostVal, new ConstantStringValue(":"), NullValue.excludeNullValue(portVal)).simplify());
					break;
				}
			case ANDROID_INTENT_FILTER_ADD_DATA_PATH:
				{
					int pathhValNum = invokeInst.getUse(1);
					int typeValNum = invokeInst.getUse(2);
					ConcreteValue pathVal, typeVal;
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						pathVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, pathhValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
					}
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						typeVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, typeValNum), mCurrNode, instIdx, TypeReference.Int, callRecords, maxDepth, subMonitor);
					}
					mStates.peek().addDataPath(pathVal, typeVal);
					break;
				}
			case ANDROID_INTENT_FILTER_ADD_DATA_SCHEME:
				{
					int schemeValNum = invokeInst.getUse(1);
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					ConcreteValue schemeVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, schemeValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
					mStates.peek().addDataScheme(schemeVal);
					break;
				}
			case ANDROID_INTENT_FILTER_ADD_DATA_SSP:
				{
					int sspValNum = invokeInst.getUse(1);
					int typeValNum = invokeInst.getUse(2);
					ConcreteValue sspVal, typeVal;
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						sspVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, sspValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
					}
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						typeVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, typeValNum), mCurrNode, instIdx, TypeReference.Int, callRecords, maxDepth, subMonitor);
					}
					mStates.peek().addDataSsp(sspVal, typeVal);
					break;
				}
			case ANDROID_INTENT_FILTER_ADD_DATA_TYPE:
				{
					int typeValNum = invokeInst.getUse(1);
					ConcreteValue typeVal;
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						typeVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, typeValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
					}
					mStates.peek().addDataMimeType(typeVal);
					break;
				}
			default:
				break;
			}
		}
		catch(CancelException ex)
		{
			throw CancelRuntimeException.make("Operation canceled");
		}
		finally
		{
			monitor.setSubProgressMonitor(null);
		}
	}
}
