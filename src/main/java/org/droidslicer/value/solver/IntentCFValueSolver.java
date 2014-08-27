package org.droidslicer.value.solver;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;
import org.droidslicer.value.ComponentNameValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.IntentValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.UnknownValue;
import org.droidslicer.value.UriValue;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import com.ibm.wala.util.intset.SparseIntSet;

public class IntentCFValueSolver extends ControlFlowValueSolver
{
	protected static class State
	{
		private ConcreteValue mAction = NullValue.getInstance();
		private ConcreteValue mUri = NullValue.getInstance();
		private Set<ConcreteValue> mCategories = new HashSet<ConcreteValue>();
		private ConcreteValue mType = NullValue.getInstance();
		private ConcreteValue mCompName = NullValue.getInstance();
		public State()
		{}
		public State(ConcreteValue action, ConcreteValue data, Collection<ConcreteValue> categories, ConcreteValue type, ConcreteValue clazz)
		{
			mAction = action;
			mUri = data;
			mCategories.addAll(categories);
			mType = type;
			mCompName = clazz;
		}
		public ConcreteValue getIntentAction()
		{
			return mAction;
		}
		public ConcreteValue getIntentData()
		{
			return mUri;
		}
		public ConcreteValue getComponentName()
		{
			return mCompName;
		}
		public Collection<ConcreteValue> getCategories()
		{
			return mCategories;
		}
		public ConcreteValue getIntentType()
		{
			return mType;
		}
		public void setIntentAction(ConcreteValue action)
		{
			if(action == null)
				throw new IllegalArgumentException();
			mAction = action;
		}
		public void setIntentData(ConcreteValue uri)
		{
			if(uri == null)
				throw new IllegalArgumentException();
			mUri = uri;
		}
		public void setComponentName(ConcreteValue compName)
		{
			if(compName == null)
				throw new IllegalArgumentException();
			mCompName = compName;
		}
		public void addCategory(ConcreteValue val)
		{
			mCategories.add(val);
		}
		public void removeCategory(ConcreteValue val)
		{
			if(val instanceof UnknownValue)
				return;
			mCategories.remove(val);
		}
		public void setIntentType(ConcreteValue val)
		{
			mType = val;
		}
	}
	private final static int MAX_NUM_PATH = 100;
	private final static int MAX_CACHE_SIZE = 10000;
	private Cache<Integer, ConcreteValue[]> mValsCache = CacheBuilder.newBuilder()
			.softValues()
			.concurrencyLevel(1)
			.maximumSize(MAX_CACHE_SIZE)
			.build();
	private int mNumPaths;
	private OrValue mValue = null;
	private CGNode mCurrNode = null;
	
	// Current state
	private Stack<State> mStates = new Stack<State>();
	public IntentCFValueSolver(ConcreteValueSolver valSolver, CGNode startNode, int startInstIdx, int valNum, EndCriterion endCriterion, int maxDepth)
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
		mValue.addValue(val);
		++mNumPaths;
	}
	@Override
	public void onStartBasicBlock(SSACFG.BasicBlock block, int instIdx)
	{
		State state = mStates.peek();
		State newState = new State(
				state.getIntentAction(),
				state.getIntentData(),
				state.getCategories(),
				state.getIntentType(),
				state.getComponentName());
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
		addPossibleValue(UnknownValue.getInstance());
	}
	protected IntentValue generateValue()
	{
		State state = mStates.peek();
		IntentValue intentVal = new IntentValue();
		intentVal.setIntentAction(state.getIntentAction());
		intentVal.setIntentComponentName(state.getComponentName());
		intentVal.setIntentUri(state.getIntentData());
		for(ConcreteValue cat : state.getCategories())
			intentVal.addIntentCategory(cat);
		intentVal.setIntentDataType(state.getIntentType());
		return intentVal;
	}
	@Override
	public void onCutPath(BasicBlock block)
	{
		addPossibleValue(UnknownValue.getInstance());
		addPossibleValue(generateValue());
	}
	@Override
	public void onEndCondition(BasicBlock block)
	{
		addPossibleValue(generateValue());
	}
	private void cloneIntent(ConcreteValue target)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(target);
		OrValue intentUri = new OrValue();
		OrValue intentAction = new OrValue();
		OrValue intentCompName = new OrValue();
		while(itr.hasNext())
		{
			ConcreteValue val = itr.next();
			if(val instanceof IntentValue)
			{
				IntentValue intentVal = (IntentValue)val;
				intentUri.addValue(intentVal.getIntentUri());
				intentAction.addValue(intentVal.getIntentAction());
				intentCompName.addValue(intentVal.getIntentComponentName());
			}
			else
			{
				intentUri.addValue(UnknownValue.getInstance());
				intentAction.addValue(UnknownValue.getInstance());
				intentCompName.addValue(UnknownValue.getInstance());
			}
		}
		State state = mStates.peek();
		state.setIntentAction(intentAction.simplify());
		state.setComponentName(intentCompName.simplify());
		state.setIntentData(intentUri.simplify());
	}
	private void polluteState()
	{
		State state = mStates.peek();
		state.setIntentAction(new OrValue(state.getIntentAction(), UnknownValue.getInstance()).simplify());
		state.setComponentName(new OrValue(state.getComponentName(), UnknownValue.getInstance()).simplify());
		state.setIntentData(new OrValue(state.getIntentData(), UnknownValue.getInstance()).simplify());
		state.addCategory(UnknownValue.getInstance());
		state.setIntentType(new OrValue(state.getIntentType(), UnknownValue.getInstance()).simplify());
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
			
			// Add unknown possible values for all fields of the Intent
			polluteState();
			return;
		}
		ProgressMonitor monitor = getProgressMonitor();
		try
		{
			setCutPath(false);
			CallRecords callRecords = getCallRecords();
			CGNode target = targets.iterator().next();
			IMethod method = target.getMethod();
			if(Utils.isReturnThis(target))
			{
				int def = invokeInst.getDef();
				addAlias(def);
			}
			int maxDepth = getMaxDepth();
			// TODO Handle java.content.Intent#setPackage()
			switch(MethodId.getMethodId(method.getReference()))
			{
			case ANDROID_INTENT_INIT:
				{
					break;
				}
			case ANDROID_INTENT_INIT_INTENT:
				{
					int intentValNum = invokeInst.getUse(1);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, intentValNum), mCurrNode, instIdx, TypeId.ANDROID_INTENT.getTypeReference(), callRecords, maxDepth, subMonitor);
						mValsCache.put(instIdx, vals);
					}
					assert vals != null && vals.length == 1;
					cloneIntent(vals[0]);
					break;
				}
			case ANDROID_INTENT_INIT_STR:
				{
					int actionValNum = invokeInst.getUse(1);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, actionValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
						mValsCache.put(instIdx, vals);
					}
					assert vals != null && vals.length == 1;
					mStates.peek().setIntentAction(vals[0]);
					break;
				}
			case ANDROID_INTENT_INIT_STR_URI:
				{
					int actionValNum = invokeInst.getUse(1);
					int uriValNum = invokeInst.getUse(2);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[2];
						{
							SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
							vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, actionValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
						}
						{
							SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
							vals[1] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, uriValNum), mCurrNode, instIdx, TypeId.ANDROID_URI.getTypeReference(), callRecords, maxDepth, subMonitor);
						}
						mValsCache.put(instIdx, vals);
					}
					assert vals != null && vals.length == 2;
					State state = mStates.peek();
					state.setIntentAction(vals[0]);
					state.setIntentData(vals[1]);
					break;
				}
			case ANDROID_INTENT_INIT_CTX_CLASS:
				{
					int classValNum = invokeInst.getUse(2);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, classValNum), mCurrNode, instIdx, TypeId.CLASS.getTypeReference(), callRecords, maxDepth, subMonitor);
						mValsCache.put(instIdx, vals);
					}
					assert vals != null && vals.length == 1;
					mStates.peek().setComponentName(new ComponentNameValue(vals[0]));
					break;
				}
			case ANDROID_INTENT_INIT_STR_URI_CTX_CLASS:
				{
					int actionValNum = invokeInst.getUse(1);
					int uriValNum = invokeInst.getUse(2);
					int classValNum = invokeInst.getUse(4);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[3];
						{
							SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
							vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, actionValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
						}
						{						
							SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
							vals[1] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, uriValNum), mCurrNode, instIdx, TypeId.ANDROID_URI.getTypeReference(), callRecords, maxDepth, subMonitor);
						}
							
						{
							SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
							vals[2] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, classValNum), mCurrNode, instIdx, TypeId.CLASS.getTypeReference(), callRecords, maxDepth, subMonitor);
						}
						mValsCache.put(instIdx, vals);
					}
					State state = mStates.peek();
					assert vals != null && vals.length == 3;
					state.setIntentAction(vals[0]);
					state.setIntentData(vals[1]);
					state.setComponentName(new ComponentNameValue(vals[2]));
					break;
				}
			case ANDROID_INTENT_FILL_IN:
			case ANDROID_INTENT_PARSE_INTENT:
			case ANDROID_INTENT_PARSE_URI:
				{
					// TODO
					polluteState();
					break;
				}
			case ANDROID_INTENT_REMOVE_CATEGORY:
				{
					int catValNum = invokeInst.getUse(1);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, catValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
						mValsCache.put(instIdx, vals);
					}
					assert vals != null && vals.length == 1;
					mStates.peek().removeCategory(vals[0]);
					break;
				}
			case ANDROID_INTENT_SET_ACTION:
				{
					int actionValNum = invokeInst.getUse(1);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, actionValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
						mValsCache.put(instIdx, vals);
					}
					assert vals != null && vals.length == 1;
					mStates.peek().setIntentAction(vals[0]);
					break;
				}
			case ANDROID_INTENT_SET_CLASS:
				{
					int classValNum = invokeInst.getUse(2);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, classValNum), mCurrNode, instIdx, TypeReference.JavaLangClass, callRecords, maxDepth, subMonitor);
						mValsCache.put(instIdx, vals);
					}
					assert vals != null && vals.length == 1;
					mStates.peek().setComponentName(new ComponentNameValue(vals[0]));
					break;
				}
			case ANDROID_INTENT_SET_CLASS_NAME_CTX_STR:
			case ANDROID_INTENT_SET_CLASS_NAME_STR_STR:
				{
					// TODO Handle the package name
					int classValNum = invokeInst.getUse(2);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, classValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
						mValsCache.put(instIdx, vals);
					}
					assert vals != null && vals.length == 1;
					mStates.peek().setComponentName(new ComponentNameValue(vals[0]));
					break;
				}
			case ANDROID_INTENT_SET_COMPONENT:
				{
					int compValNum = invokeInst.getUse(1);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, compValNum), mCurrNode, instIdx, TypeId.ANDROID_COMPONENT_NAME.getTypeReference(), callRecords, maxDepth, subMonitor);
						mValsCache.put(instIdx, vals);								
					}					
					assert vals != null && vals.length == 1;
					mStates.peek().setComponentName(vals[0]);
					break;
				}
			case ANDROID_INTENT_SET_DATA:
				{
					int uriValNum = invokeInst.getUse(1);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, uriValNum), mCurrNode, instIdx, TypeId.ANDROID_URI.getTypeReference(), callRecords, maxDepth, subMonitor);
						mValsCache.put(instIdx, vals);								
					}					
					State state = mStates.peek();
					assert vals != null && vals.length == 1;
					state.setIntentData(vals[0]);
					state.setIntentType(NullValue.getInstance());
					break;
				}
			case ANDROID_INTENT_SET_DATA_AND_NORMALIZE:
				{
					int uriValNum = invokeInst.getUse(1);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, uriValNum), mCurrNode, instIdx, TypeId.ANDROID_URI.getTypeReference(), callRecords, maxDepth, subMonitor);
						vals[0] = UriValue.normalizeSchemeForAndroid(vals[0]);
					}
					State state = mStates.peek();
					assert vals != null && vals.length == 1;
					state.setIntentData(vals[0]);
					state.setIntentType(NullValue.getInstance());
					break;
				}
			case ANDROID_INTENT_SET_DATA_AND_TYPE:
				{
					int uriValNum = invokeInst.getUse(1);
					int typeValNum = invokeInst.getUse(2);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[2];
						{
							SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
							vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, uriValNum), mCurrNode, instIdx, TypeId.ANDROID_URI.getTypeReference(), callRecords, maxDepth, subMonitor);
						}
						{
							SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
							vals[1] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, typeValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
						}
						mValsCache.put(instIdx, vals);
					}
					State state = mStates.peek();
					assert vals != null && vals.length == 2;
					state.setIntentData(vals[0]);
					state.setIntentType(vals[1]);
					break;
				}
			case ANDROID_INTENT_SET_DATA_AND_TYPE_AND_NORMALIZE:
				{	
					int uriValNum = invokeInst.getUse(1);
					int typeValNum = invokeInst.getUse(2);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[2];
						{
							SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
							vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, uriValNum), mCurrNode, instIdx, TypeId.ANDROID_URI.getTypeReference(), callRecords, maxDepth, subMonitor);
							vals[0] = UriValue.normalizeSchemeForAndroid(vals[0]);
						}
						{
							SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
							vals[1] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, typeValNum), mCurrNode, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
							vals[1] = IntentValue.normalizeMimeTypeForAndroid(vals[1]);
						}
						mValsCache.put(instIdx, vals);
					}
					State state = mStates.peek();
					assert vals != null && vals.length == 2;
					state.setIntentData(vals[0]);
					state.setIntentType(vals[1]);
					break;
				}
			case ANDROID_INTENT_SET_TYPE:
				{
					int typeValNum = invokeInst.getUse(1);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, typeValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
						mValsCache.put(instIdx, vals);
					}
					State state = mStates.peek();
					assert vals != null && vals.length == 1;
					state.setIntentType(vals[0]);
					state.setIntentData(NullValue.getInstance());
					break;
				}
			case ANDROID_INTENT_SET_TYPE_AND_NORMALIZE:
				{
					int typeValNum = invokeInst.getUse(1);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, typeValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
						vals[0] = IntentValue.normalizeMimeTypeForAndroid(vals[0]);
						mValsCache.put(instIdx, vals);
					}
					State state = mStates.peek();
					assert vals != null && vals.length == 1;
					state.setIntentType(vals[0]);
					state.setIntentData(NullValue.getInstance());
					break;
				}
			case ANDROID_INTENT_ADD_CATEGORY:
				{
					int catValNum = invokeInst.getUse(1);
					ConcreteValue[] vals = mValsCache.getIfPresent(instIdx);
					if(vals == null)
					{
						vals = new ConcreteValue[1];
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						vals[0] = valSolver.solve(new ParamCaller(mCurrNode, instIdx, catValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
						mValsCache.put(instIdx, vals);
					}
					assert vals != null && vals.length == 1;
					mStates.peek().addCategory(vals[0]);
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

