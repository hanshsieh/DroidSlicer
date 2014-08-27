package org.droidslicer.value.solver;

import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.Utils;
import org.droidslicer.value.AndroidUriBuilderValue;
import org.droidslicer.value.ConcatValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;
import org.droidslicer.value.IntValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.UnknownValue;
import org.droidslicer.value.UriEncodedValue;
import org.droidslicer.value.UriValue;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;
import com.ibm.wala.util.intset.SparseIntSet;

public class AndroidUriBuilderCFValueSolver extends ControlFlowValueSolver
{
	protected static class State
	{
		public State()
		{}
		public State(
				boolean opaque, 
				ConcreteValue scheme, 
				ConcreteValue authority, 
				ConcreteValue path, 
				ConcreteValue query, 
				ConcreteValue fragment, 
				ConcreteValue ssp)
		{
			mOpaque = opaque;
			mScheme = scheme;
			mAuthority = authority;
			mPath = path;
			mQuery = query;
			mFragment = fragment;
			mSsp = ssp;
		}
		// When in opaque mode, the URI only
		// contains the following information
		// scheme, ssp, fragment, and the URI will be 
		// <scheme>:ssp#<fragment>
		// When not using ssp, the URI will contain the following information:
		// scheme, authority, path, query, fragment, and the URI will be
		// <scheme>://<authority><path>?<query>#<fragment>
		// Notice that <path> should have a leading '/'.
		private boolean mOpaque = false;
		private ConcreteValue mScheme = NullValue.getInstance();
		private ConcreteValue mAuthority = NullValue.getInstance();
		private ConcreteValue mPath = NullValue.getInstance();
		private ConcreteValue mQuery = NullValue.getInstance();
		private ConcreteValue mFragment = NullValue.getInstance();
		private ConcreteValue mSsp = NullValue.getInstance();
		public boolean isOpaque()
		{
			return mOpaque;
		}
		public ConcreteValue getScheme()
		{
			return mScheme;
		}
		public void setScheme(ConcreteValue scheme)
		{
			mScheme = scheme;
		}
		public ConcreteValue getEncodedAuthority()
		{
			return mAuthority;
		}
		public void setEncodedAuthority(ConcreteValue authority)
		{
			if(authority == null)
				throw new IllegalArgumentException();
			mOpaque = false;
			mAuthority = authority;
		}
		public ConcreteValue getEncodedPath()
		{
			return mPath;
		}
		public void setEncodedPath(ConcreteValue val)
		{
			if(val == null)
				throw new IllegalArgumentException();
			mOpaque = false;
			mPath = val;
		}
		public void appendEncodedPath(ConcreteValue val)
		{
			if(val == null)
				throw new IllegalArgumentException();
			mOpaque = false;
			if(mPath instanceof NullValue)
				mPath = val;
			else
				mPath = UriValue.makePathWithAppendedEncoded(mPath, val);
		}
		public ConcreteValue getEncodedOpaquePart()
		{
			return mSsp;
		}
		public void setEncodedOpaquePart(ConcreteValue val)
		{
			if(val == null)
				throw new IllegalArgumentException();
			mOpaque = true;
			mSsp = val;
		}
		public void clearQuery()
		{
			mOpaque = false;
			mQuery = null;
		}
		public ConcreteValue getEncodedQuery()
		{
			return mQuery;
		}
		public void appendEncodedQuery(ConcreteValue val)
		{
			if(val == null)
				throw new IllegalArgumentException();
			mOpaque = false;
			if(mQuery instanceof NullValue)
				mQuery = val;
			else
				mQuery = new ConcatValue(mQuery, new ConstantStringValue("&"), val);
		}
		public void setEncodedQuery(ConcreteValue val)
		{
			if(val == null)
				throw new IllegalArgumentException();
			mOpaque = false;
			mQuery = val;
		}
		public ConcreteValue getEncodedFragment()
		{
			return mFragment;
		}
		public void setEncodedFragment(ConcreteValue frag)
		{
			if(frag == null)
				throw new IllegalArgumentException();
			mFragment = frag;
		}
		public ConcreteValue genValue()
		{
			UriValue uriVal;
			if(mOpaque)
			{
				uriVal = new UriValue(mScheme, mSsp, mFragment);
			}
			else
			{
				uriVal = new UriValue(mScheme, mAuthority, mPath, mQuery, mFragment);
			}
			return new AndroidUriBuilderValue(uriVal);
		}
	}
	private final static int MAX_NUM_PATH = 100;
	private int mNumPaths;
	private State mInitState = null;
	private OrValue mValue = null;
	private CGNode mCurrNode = null;
	private Stack<State> mStates = new Stack<State>();
	public AndroidUriBuilderCFValueSolver(ConcreteValueSolver valSolver, CGNode startNode, int startInstIdx, int valNum, EndCriterion endCriterion, int maxDepth)
	{
		super(valSolver, startNode, startInstIdx, SparseIntSet.singleton(valNum), endCriterion, maxDepth);
	}
	private static State genStateFromUriValue(UriValue uriVal)
	{
		State state = new State();
		switch(uriVal.getMode())
		{
		case OPAQUE:
			state.setScheme(uriVal.getScheme());
			state.setEncodedOpaquePart(uriVal.getSchemeSpecificPart());
			state.setEncodedFragment(uriVal.getFragment());
			break;
		case HIERARCHICAL_SERVER_BASED:
			{
				state.setScheme(uriVal.getScheme());
				ConcatValue authVal = new ConcatValue();
				ConcreteValue userInfo = uriVal.getUserInfo();
				if(NullValue.isPossibleNotNull(userInfo))
				{
					if(!NullValue.isPossibleNull(userInfo))
					{
						authVal.addValue(NullValue.excludeNullValue(userInfo));
						authVal.addValue(new ConstantStringValue("@"));
					}
					else
					{
						OrValue orVal = new OrValue();
						orVal.addValue(new ConcatValue(userInfo, new ConstantStringValue("@")));
						orVal.addValue(ConstantStringValue.getEmptyString());
						authVal.addValue(orVal);
					}
				}
				ConcreteValue host = uriVal.getHost();
				if(NullValue.isPossibleNotNull(host))
				{
					authVal.addValue(NullValue.excludeNullValue(host));
				}
				ConcreteValue portVal = uriVal.getPort();
				if(IntValue.isPossibleNotNegative(portVal))
				{
					ConcreteValue portStrVal = IntValue.excludeNegative(portVal).getStringValue();
					if(!IntValue.isPossibleNegative(portVal))
					{
						authVal.addValue(new ConstantStringValue(":"));
						authVal.addValue(portStrVal);
					}
					else
					{
						OrValue orVal = new OrValue();
						orVal.addValue(
								new ConcatValue(new ConstantStringValue(":"), 
										portStrVal));
						orVal.addValue(ConstantStringValue.getEmptyString());
						authVal.addValue(orVal);
					}
				}
				state.setEncodedAuthority(authVal.simplify());
				state.setEncodedPath(uriVal.getPath());
				state.setEncodedQuery(uriVal.getQuery());
				state.setEncodedFragment(uriVal.getFragment());
				break;
			}
		case HIERARCHICAL_REGISTRY_BASED:
			state.setScheme(uriVal.getScheme());
			state.setEncodedAuthority(uriVal.getAuthority());
			state.setEncodedPath(uriVal.getPath());
			state.setEncodedQuery(uriVal.getQuery());
			state.setEncodedFragment(uriVal.getFragment());
		case WHOLE_URI:
			// TODO Maybe we can do better for this case
			state.setScheme(UnknownValue.getInstance());
			state.setEncodedAuthority(UnknownValue.getInstance());
			state.setEncodedPath(UnknownValue.getInstance());
			state.setEncodedQuery(UnknownValue.getInstance());
			state.setEncodedFragment(UnknownValue.getInstance());
			break;
		default:
			throw new RuntimeException("Unreachable");
		}				
		return state;
	}
	public void setInitialValue(ConcreteValue val)
	{
		mInitState = null;
		boolean multiVal = false;
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue valSingle = itr.next();
			if(valSingle instanceof AndroidUriBuilderValue)
			{
				AndroidUriBuilderValue builderVal = (AndroidUriBuilderValue)valSingle;
				ConcreteValue uriVal = builderVal.getUriValue();
				Iterator<ConcreteValue> uriItr = OrValue.getSingleValueIterator(uriVal);
				while(uriItr.hasNext())
				{
					ConcreteValue uriValSingle = uriItr.next();
					if(uriValSingle instanceof UriValue)
					{
						UriValue uriValReal = (UriValue)uriValSingle;
						if(mInitState == null)
							mInitState = genStateFromUriValue(uriValReal);
						else
							multiVal = true;
					}						
				}	
			}
		}
		if(multiVal || mInitState == null)
		{
			mInitState = new State(
					false, 
					UnknownValue.getInstance(), 					
					UnknownValue.getInstance(), 
					UnknownValue.getInstance(), 
					UnknownValue.getInstance(), 
					UnknownValue.getInstance(), 
					UnknownValue.getInstance());
		}
	}
	
	@Override
	public ConcreteValue getValue()
	{
		if(mValue == null)
			return null;
		else
			return mValue.simplify();
	}
	protected ConcreteValue generateValue()
	{
		State state = mStates.peek();
		return state.genValue();
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
				state.isOpaque(), 
				state.getScheme(), 
				state.getEncodedAuthority(), 
				state.getEncodedPath(), 
				state.getEncodedQuery(), 
				state.getEncodedFragment(),
				state.getEncodedOpaquePart());
		mStates.push(newState);
	}
	@Override
	public void onEndBasicBlock(SSACFG.BasicBlock block)
	{
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
	public void onStart()
	{
		assert mStates.isEmpty();
		ProgressMonitor monitor = getProgressMonitor();
		monitor.beginTask("Solving Uri value", 1000);
		mCurrNode = null;
		mStates.clear();
		mStates.add(mInitState == null ? new State() : mInitState);
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
		mInitState = null;
	}
	@Override
	public void onTerminate(BasicBlock block)
	{
		addPossibleValue(generateValue());
		addPossibleValue(UnknownValue.getInstance());
	}
	@Override
	public void onRevisitBasicBlock(BasicBlock block, int instIdx)
	{
		addPossibleValue(generateValue());
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
			return;
		}
		ProgressMonitor monitor = getProgressMonitor();
		try
		{
			CallRecords callRecords = getCallRecords();
			CGNode target = targets.iterator().next();
			IMethod method = target.getMethod();
			ClassLoaderReference classLoader = method.getDeclaringClass().getClassLoader().getReference();
			if(!classLoader.equals(ClassLoaderReference.Primordial))
			{
				setCutPath(true);
				return;
			}
			if(Utils.isReturnThis(target))
			{
				int def = invokeInst.getDef();
				addAlias(def);
			}
			int maxDepth = getMaxDepth();
			switch(MethodId.getMethodId(method.getReference()))
			{
			case ANDROID_URI_BUILDER_APPEND_ENCODED_PATH:
				{
					int pathSegValNum = invokeInst.getUse(1);
					ConcreteValue pathSegVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					pathSegVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, pathSegValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					mStates.peek().appendEncodedPath(pathSegVal);
					break;
				}
			case ANDROID_URI_BUILDER_APPEND_PATH:
				{
					int pathSegValNum = invokeInst.getUse(1);
					ConcreteValue pathSegVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					pathSegVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, pathSegValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					pathSegVal = UriEncodedValue.makeForAndroid(pathSegVal, true);
					mStates.peek().appendEncodedPath(pathSegVal);
					break;
				}
			case ANDROID_URI_BUILDER_APPEND_QUERY_PARAM:
				{
					int keyValNum = invokeInst.getUse(1);
					int valValNum = invokeInst.getUse(2);
					ConcreteValue keyVal;
					ConcreteValue valVal;
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						monitor.setSubProgressMonitor(subMonitor);
						keyVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, keyValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					}
						
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						monitor.setSubProgressMonitor(subMonitor);
						valVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, valValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					}
					// Encoded the key and value
					keyVal = UriEncodedValue.makeForAndroid(keyVal, true);
					valVal = UriEncodedValue.makeForAndroid(valVal, true);
					ConcatValue queryVal = new ConcatValue(keyVal, new ConstantStringValue("="), valVal);
					mStates.peek().appendEncodedQuery(queryVal);
					break;
				}
			case ANDROID_URI_BUILDER_AUTHORITY:
				{
					int authValNum = invokeInst.getUse(1);
					ConcreteValue authVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					authVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, authValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					// Encoded the key and value
					authVal = UriEncodedValue.makeForAndroid(authVal, true);
					mStates.peek().setEncodedAuthority(authVal);
					break;
				}
			case ANDROID_URI_BUILDER_CLEAR_QUERY:
				{
					mStates.peek().clearQuery();
					break;
				}
			case ANDROID_URI_BUILDER_ENCODED_AUTHORITY:
				{
					int authValNum = invokeInst.getUse(1);
					ConcreteValue authVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					authVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, authValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					mStates.peek().setEncodedAuthority(authVal);
					break;
				}
			case ANDROID_URI_BUILDER_ENCODED_FRAGMENT:
				{
					int fragValNum = invokeInst.getUse(1);
					ConcreteValue fragVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					fragVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, fragValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					mStates.peek().setEncodedFragment(fragVal);
					break;
				}
			case ANDROID_URI_BUILDER_ENCODED_OPAQUE_PART:
				{
					int sspValNum = invokeInst.getUse(1);
					ConcreteValue sspVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					sspVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, sspValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					mStates.peek().setEncodedOpaquePart(sspVal);
					break;
				}
			case ANDROID_URI_BUILDER_ENCODED_PATH:
				{
					int pathValNum = invokeInst.getUse(1);
					ConcreteValue pathVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					pathVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, pathValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					mStates.peek().setEncodedPath(pathVal);
					break;
				}
			case ANDROID_URI_BUILDER_ENCODED_QUERY:
				{
					int queryValNum = invokeInst.getUse(1);
					ConcreteValue queryVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					queryVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, queryValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					mStates.peek().setEncodedQuery(queryVal);
					break;
				}
			case ANDROID_URI_BUILDER_FRAGMENT:
				{
					int fragValNum = invokeInst.getUse(1);
					ConcreteValue fragVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					fragVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, fragValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					fragVal = UriEncodedValue.makeForAndroid(fragVal, true);
					mStates.peek().setEncodedFragment(fragVal);
					break;
				}
			case ANDROID_URI_BUILDER_OPAQUE_PART:
				{
					int sspValNum = invokeInst.getUse(1);
					ConcreteValue sspVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					sspVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, sspValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					sspVal = UriEncodedValue.makeForAndroid(sspVal, true);
					mStates.peek().setEncodedOpaquePart(sspVal);
					break;
				}
			case ANDROID_URI_BUILDER_PATH:
				{
					int pathValNum = invokeInst.getUse(1);
					ConcreteValue pathVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					pathVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, pathValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					pathVal = UriEncodedValue.makeForAndroidPath(pathVal, true);
					mStates.peek().setEncodedPath(pathVal);
					break;
				}
			case ANDROID_URI_BUILDER_QUERY:
				{
					int queryValNum = invokeInst.getUse(1);
					ConcreteValue queryVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					queryVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, queryValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					queryVal = UriEncodedValue.makeForAndroid(queryVal, true);
					mStates.peek().setEncodedQuery(queryVal);
					break;
				}
			case ANDROID_URI_BUILDER_SCHEME:
				{
					int schemeValNum = invokeInst.getUse(1);
					ConcreteValue schemeVal;
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					schemeVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, schemeValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					mStates.peek().setScheme(schemeVal);
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
