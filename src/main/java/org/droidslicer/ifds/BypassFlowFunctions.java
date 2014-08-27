package org.droidslicer.ifds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.droidslicer.util.StatementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.intset.IntSet;

/**
 * Notice that the design of this class only support forward slicing.
 * @author someone
 *
 */
public class BypassFlowFunctions implements IDependencyFlowFunctions
{
	private final static Logger mLogger = LoggerFactory.getLogger(BypassFlowFunctions.class);
	protected class NormalFlowHandler extends SSAInstruction.Visitor
	{
		protected IUnaryFlowFunction mFlowFunct = null;
		protected Statement mSrcStm;
		public IUnaryFlowFunction process(Statement src, Statement dest)
		{
			mFlowFunct = null;
			mSrcStm = src;
			switch(dest.getKind())
			{
			case NORMAL:
				{
					NormalStatement destNormal = (NormalStatement)dest;
					SSAInstruction destInst = destNormal.getInstruction();
					destInst.visit(this);
					break;
				}
			case HEAP_RET_CALLEE:
			case HEAP_RET_CALLER:
			case HEAP_PARAM_CALLER:
			case HEAP_PARAM_CALLEE:
				mFlowFunct = KillEverything.singleton();
				break;
			default:
				break;
			}
			if(mFlowFunct == null)
				return getFlowFunction(mSrcStm);
			else
				return mFlowFunct;
		}
		@Override
		public void visitGet(SSAGetInstruction getInst) 
		{
			if(getInst.isStatic())
				mFlowFunct = KillEverything.singleton();
		}
		@Override
		public void visitPut(SSAPutInstruction putInst)
		{
			if(putInst.isStatic())
				return;
			int valNum = putInst.getVal();
			if(!StatementUtils.isDefiningStatement(mSrcStm, valNum))
				mFlowFunct = KillEverything.singleton();
		}
		@Override
		public void visitArrayStore(SSAArrayStoreInstruction astoreInst)
		{
			int valNum = astoreInst.getValue();
			if(!StatementUtils.isDefiningStatement(mSrcStm, valNum))
				mFlowFunct = KillEverything.singleton();
		}
	}
	private final static int MAX_CACHE_SIZE = 20000;
	private final Predicate<Statement> mTerminators;
	private final Map<Statement, IntSet> mSeeds = new LinkedHashMap<Statement, IntSet>();
	private final int mZeroFact;
	private final Map<Statement, Set<Statement>> mBypassedCallers = new HashMap<Statement, Set<Statement>>();
	private NormalFlowHandler mNormalFlowHandler = null;
	private boolean mRecordBypassCalls = false;
	private boolean mCutRetToSynthetic = false;
	private final LoadingCache<Statement, IUnaryFlowFunction> mFlowFunctCache = CacheBuilder.newBuilder()
			.maximumSize(MAX_CACHE_SIZE)
			.softValues()
			.concurrencyLevel(1)
			.build(new CacheLoader<Statement, IUnaryFlowFunction>()
			{
				@Override
				public IUnaryFlowFunction load(Statement src) throws Exception				
				{
					IntSet seed = mSeeds.get(src);
					if(mTerminators.test(src))
					{
						if(seed == null)
							return KillEverything.singleton();
						else
							return new KillAllGenFlowFunction(seed);
					}
					else
					{
						if(seed == null)
							return IdentityFlowFunction.identity();
						else
							return new CondVectorGenFlowFunction(seed, mZeroFact);
					}
				}
			});
	public BypassFlowFunctions(Predicate<Statement> terminators, int zeroFact)
	{
		if(terminators == null)
			throw new IllegalArgumentException();
		if(zeroFact < 0)
			throw new IllegalArgumentException("Zero fact cannot be negative");
		mTerminators = terminators;
		mZeroFact = zeroFact;
	}
	@Override
	public void addSeed(Statement stm, IntSet facts)
	{
		mSeeds.put(stm, facts);
	}
	@Override
	public void clearSeeds()
	{
		mSeeds.clear();
	}
	@Override
	public Map<Statement, ? extends IntSet> getSeeds()
	{
		return mSeeds;
	}
	@Override
	public int getZeroFact()
	{
		return mZeroFact;
	}
	public void setRecordBypassedCalls(boolean val)
	{
		mRecordBypassCalls = val;
	}
	public boolean isRecordBypassedCalls()
	{
		return mRecordBypassCalls;
	}
	public Map<Statement, Set<Statement>> getBypassedCallers()
	{
		if(!mRecordBypassCalls)
			return null;
		return mBypassedCallers;
	}
	protected IUnaryFlowFunction getFlowFunction(Statement src)
	{
		return mFlowFunctCache.getUnchecked(src);
	}

	public void setCutReturnToSynthetic(boolean cut)
	{
		mCutRetToSynthetic = cut;
	}
	public boolean isCutReturnToSynthetic()
	{
		return mCutRetToSynthetic;
	}
	protected boolean shouldBypassCall(Statement caller, Statement callee)
	{
		if(callee == null)
			return true;
		IMethod calleeMethod = callee.getNode().getMethod();
		ClassLoaderReference calleeLoaderRef = calleeMethod.getDeclaringClass().getClassLoader().getReference();
		if(calleeLoaderRef.equals(ClassLoaderReference.Primordial))
			return true;
		else
			return false;
	}
	protected NormalFlowHandler getNormalFlowHandler()
	{
		if(mNormalFlowHandler == null)
			mNormalFlowHandler = new NormalFlowHandler();
		return mNormalFlowHandler;
	}
	@Override
	public IUnaryFlowFunction getNormalFlowFunction(Statement src,
			Statement dest) 
	{
		return getNormalFlowHandler().process(src, dest);
	}

	@Override
	public IUnaryFlowFunction getCallFlowFunction(Statement caller,
			Statement callee, Statement ret)
	{
		// If we should bypass this method
		if(shouldBypassCall(caller, callee))
		{
			if(mRecordBypassCalls)
				return new DelegateCallReportFlowFunction(caller, callee, KillEverything.singleton());
			else
				return KillEverything.singleton();
		}
		else
			return getFlowFunction(caller);
	}

	@Override
	public IFlowFunction getReturnFlowFunction(Statement call, Statement src,
			Statement dest) 
	{
		return getUnbalancedReturnFlowFunction(src, dest);
	}

	@Override
	public IUnaryFlowFunction getCallToReturnFlowFunction(Statement caller,
			Statement ret) 
	{
		return KillEverything.singleton();
	}

	/**
	 * Notice that this function is only invoked for the caller nodes without callee but with return nodes.
	 * If there's an invoke instruction without return value, and it has no resolvable callee, then this 
	 * function won't be called for the caller node.
	 */
	@Override
	public IUnaryFlowFunction getCallNoneToReturnFlowFunction(Statement caller,
			Statement ret) 
	{
		return KillEverything.singleton();
	}

	@Override
	public IFlowFunction getUnbalancedReturnFlowFunction(Statement src,
			Statement dest) 
	{
		switch(dest.getKind())
		{
		case NORMAL_RET_CALLER:
			{
				CGNode destNode = dest.getNode();
				IMethod destMethod = destNode.getMethod();
				if(destMethod instanceof FakeRootMethod)
					return KillEverything.singleton();
				if(destMethod.isSynthetic())
				{
					if(mCutRetToSynthetic)
						return KillEverything.singleton();
					else
						return getFlowFunction(src);
				}
				else
				{
					if(!destMethod.getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application))
						return KillEverything.singleton(); 
					else
						return getFlowFunction(src);
				}
			}
		default:
			return KillEverything.singleton();
		}
	}
	private class DelegateCallReportFlowFunction implements IUnaryFlowFunction
	{
		private Statement mCaller, mCallee;
		private final IUnaryFlowFunction mDelegate;
		public DelegateCallReportFlowFunction(Statement caller, Statement callee, IUnaryFlowFunction delegate)
		{
			if(caller == null || callee == null || delegate == null)
				throw new IllegalArgumentException();
			mCaller = caller;
			mCallee = callee;
			mDelegate = delegate;
		}
		@Override
		public IntSet getTargets(int d1)
		{
			if(mCaller != null && mCallee != null)
			{
				Set<Statement> old = mBypassedCallers.get(mCaller);
				if(old == null)
				{
					old = new HashSet<Statement>(3);
					mBypassedCallers.put(mCaller, old);
				}
				old.add(mCallee);
				mCaller = null;
				mCallee = null;
			}
			return mDelegate.getTargets(d1);
		}
	}
}
