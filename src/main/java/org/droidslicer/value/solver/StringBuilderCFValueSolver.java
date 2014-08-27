package org.droidslicer.value.solver;

import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;
import org.droidslicer.value.ConcatValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.UnknownValue;

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

public class StringBuilderCFValueSolver extends ControlFlowValueSolver
{
	private static class State
	{
		private ConcatValue mValue = new ConcatValue();
		public State()
		{
			
		}
		public State(State state)
		{
			mValue.addValue(state.mValue);
		}
		public ConcreteValue generateValue()
		{
			return mValue.simplify();
		}
		protected static ConcreteValue filterValue(ConcreteValue val)
		{
			if(val instanceof ConstantStringValue || val instanceof ConcatValue || val instanceof UnknownValue)
				return val;
			OrValue result = new OrValue();
			Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
			while(itr.hasNext())
			{
				ConcreteValue singleVal = itr.next();
				if(singleVal instanceof ConstantStringValue || singleVal instanceof ConcatValue)
					result.addValue(singleVal);
				else
					result.addValue(UnknownValue.getInstance());
			}
			return result.simplify();
		}
		public void appendValue(ConcreteValue val)
		{
			mValue.addValue(filterValue(val));
		}
		public void clear()
		{
			mValue = new ConcatValue();
		}
	}
	private final static int MAX_NUM_PATH = 100;
	private final static int MAX_NUM_VALUE = 20;
	private final static int MAX_CACHE_SIZE = 10000;
	private int mNumPaths;
	private final Stack<State> mStates = new Stack<State>();
	private OrValue mValue = null;
	private CGNode mCurrNode = null;
	private Cache<Integer, ConcreteValue> mValsCache = CacheBuilder.newBuilder()
			.softValues()
			.concurrencyLevel(1)
			.maximumSize(MAX_CACHE_SIZE)
			.build();
			
	public StringBuilderCFValueSolver(
			ConcreteValueSolver valSolver, CGNode startNode, int startInstIdx, int valNum, EndCriterion endCriterion, int maxDepth)
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
		++mNumPaths;
		mValue.addValue(val);
		if(mValue.size() > MAX_NUM_VALUE || mNumPaths >= MAX_NUM_PATH)
			setTerminate(true);
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
		if(block.isExitBlock())
		{
			State state = mStates.peek();
			ConcreteValue val = state.generateValue();
			addPossibleValue(val);
			addPossibleValue(UnknownValue.getInstance());
		}
		mStates.pop();
	}
	@Override
	public void onStartNode(CGNode node)
	{
		mCurrNode = node;
	}
	@Override
	public void onRevisitBasicBlock(BasicBlock block, int instIdx)
	{
		State state = mStates.peek();
		State newState = new State(state);
		newState.appendValue(UnknownValue.getInstance());
		ConcreteValue val = newState.generateValue();
		addPossibleValue(val);
	}
	@Override
	public void onCutPath(BasicBlock block)
	{
		State state = mStates.peek();
		State newState = new State(state);
		newState.appendValue(UnknownValue.getInstance());
		ConcreteValue val = newState.generateValue();
		addPossibleValue(val);
		addPossibleValue(UnknownValue.getInstance());
	}
	@Override
	public void onTerminate(BasicBlock block)
	{
		State state = mStates.peek();
		State newState = new State(state);
		newState.appendValue(UnknownValue.getInstance());
		ConcreteValue val = newState.generateValue();
		addPossibleValue(val);
		addPossibleValue(UnknownValue.getInstance());
	}
	@Override
	public void onEndCondition(BasicBlock block)
	{
		addPossibleValue(mStates.peek().generateValue());
	}
	@Override
	public void onStart()
	{
		mValue = new OrValue();
		mStates.clear();
		mStates.push(new State());
		mNumPaths = 0;
		mValsCache.invalidateAll();
		getProgressMonitor().beginTask("Solving StringBuilder value", 100);
	}
	@Override
	public void onEndNode(CGNode node)
	{
		mCurrNode = null;
	}
	@Override
	public void visitInvoke(SSAInvokeInstruction invokeInst) 
	{
		if(invokeInst.isStatic() || getFirstUseIndex() != 0)
		{
			setCutPath(true);
			return;
		}
		int instIdx = getInstructionIndex();
		ConcreteValueSolver valSolver = getValueSolver();
		CallSiteReference callSite = invokeInst.getCallSite();
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
			CallRecords callRecords = getCallRecords();
			CGNode target = targets.iterator().next();
			IMethod method = target.getMethod();
			if(Utils.isReturnThis(target))
			{
				int def = invokeInst.getDef();
				addAlias(def);
			}
			int maxDepth = getMaxDepth();
			boolean processed = true;
			switch(MethodId.getMethodId(method.getReference()))
			{
			case STR_BUILDER_INIT:
			case STR_BUILDER_INIT_INT:
				{
					mStates.peek().clear();
					break;
				}
			case STR_BUILDER_INIT_STR:
				{
					int strValNum = invokeInst.getUse(1);
					ConcreteValue val = mValsCache.getIfPresent(instIdx);
					if(val == null)
					{
						val = valSolver.solve(
							new ParamCaller(mCurrNode, instIdx, strValNum), 
							mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, new SubProgressMonitor(monitor, 10));
						mValsCache.put(instIdx, val);
					}
					State state = mStates.peek();
					state.clear();
					state.appendValue(val);
					break;
				}
			case STR_BUILDER_APPEND_STR:
				{
					int strValNum = invokeInst.getUse(1);
					ConcreteValue appendStr = mValsCache.getIfPresent(instIdx);
					if(appendStr == null)
					{
						appendStr = valSolver.solve(
								new ParamCaller(mCurrNode, instIdx, strValNum), 
								mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, new SubProgressMonitor(monitor, 10));
						mValsCache.put(instIdx, appendStr);
					}
					mStates.peek().appendValue(appendStr);
					break;
				}
			case STR_BUILDER_APPEND_BOOL:
				{
					int boolValNum = invokeInst.getUse(1);
					ConcreteValue appendBool = mValsCache.getIfPresent(instIdx);
					if(appendBool == null)
					{
						appendBool = valSolver.solve(
								new ParamCaller(mCurrNode, instIdx, boolValNum),
								mCurrNode, instIdx, TypeReference.Boolean, callRecords, maxDepth, new SubProgressMonitor(monitor, 10));
						appendBool = appendBool.getStringValue();
						mValsCache.put(instIdx, appendBool);
					}
					mStates.peek().appendValue(appendBool);
					break;
				}
			case STR_BUILDER_APPEND_CHAR:
				{
					int charValNum = invokeInst.getUse(1);
					ConcreteValue appendChar = mValsCache.getIfPresent(instIdx);
					if(appendChar == null)
					{
						appendChar = valSolver.solve(
								new ParamCaller(mCurrNode, instIdx, charValNum),
								mCurrNode, instIdx, TypeReference.Char, callRecords, maxDepth, new SubProgressMonitor(monitor, 10));
						appendChar = appendChar.getStringValue();
						mValsCache.put(instIdx, appendChar);
					}
					mStates.peek().appendValue(appendChar);
					break;
				}
			case STR_BUILDER_APPEND_DOUBLE:
				{
					int doubleValNum = invokeInst.getUse(1);
					ConcreteValue appendDouble = mValsCache.getIfPresent(instIdx);
					if(appendDouble == null)
					{
						appendDouble = valSolver.solve(
								new ParamCaller(mCurrNode, instIdx, doubleValNum),
								mCurrNode, instIdx, TypeReference.Double, callRecords, maxDepth, new SubProgressMonitor(monitor, 10));
						appendDouble = appendDouble.getStringValue();
						mValsCache.put(instIdx, appendDouble);
					}
					mStates.peek().appendValue(appendDouble);
					break;
				}
			case STR_BUILDER_APPEND_FLOAT:
				{
					int floatValNum = invokeInst.getUse(1);
					ConcreteValue appendFloat = mValsCache.getIfPresent(instIdx);
					if(appendFloat == null)
					{
						appendFloat = valSolver.solve(
								new ParamCaller(mCurrNode, instIdx, floatValNum),
								mCurrNode, instIdx, TypeReference.Float, callRecords, maxDepth, new SubProgressMonitor(monitor, 10));
						appendFloat = appendFloat.getStringValue();
						mValsCache.put(instIdx, appendFloat);
					}
					mStates.peek().appendValue(appendFloat);
					break;
				}
			case STR_BUILDER_APPEND_INT:
				{
					int intValNum = invokeInst.getUse(1);
					ConcreteValue appendInt = mValsCache.getIfPresent(instIdx);
					if(appendInt == null)
					{
						appendInt = valSolver.solve(
								new ParamCaller(mCurrNode, instIdx, intValNum),
								mCurrNode, instIdx, TypeReference.Int, callRecords, maxDepth, new SubProgressMonitor(monitor, 10));
						appendInt = appendInt.getStringValue();
						mValsCache.put(instIdx, appendInt);
					}
					mStates.peek().appendValue(appendInt);
					break;
				}
			case STR_BUILDER_APPEND_LONG:
				{
					int longValNum = invokeInst.getUse(1);
					ConcreteValue appendLong = mValsCache.getIfPresent(instIdx);
					if(appendLong == null)
					{
						appendLong = valSolver.solve(
								new ParamCaller(mCurrNode, instIdx, longValNum),
								mCurrNode, instIdx, TypeReference.Long, callRecords, maxDepth, new SubProgressMonitor(monitor, 10));
						appendLong = appendLong.getStringValue();
						mValsCache.put(instIdx, appendLong);
					}
					mStates.peek().appendValue(appendLong);
					break;
				}
			default:
				{
					processed = false;
					TypeReference targetTypeRef = method.getDeclaringClass().getReference();
					
					// If the class of the target method is StringBuilder
					if(TypeId.STR_BUILDER.getTypeReference().getName().equals(targetTypeRef.getName()))
					{					
						// TODO Ugly, can we do better?
						if(method.getName().toString().startsWith("append"))
						{
							mStates.peek().appendValue(UnknownValue.getInstance());
							processed = true;
						}
					}
				}
				break;
			}
			setCutPath(!processed);
		}
		catch(CancelException ex)
		{
			throw CancelRuntimeException.make("Operation canceled");
		}
	}
}
