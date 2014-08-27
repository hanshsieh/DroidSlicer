package org.droidslicer.value.solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.ifds.KillAllGenFlowFunction;
import org.droidslicer.util.StatementUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.UnorderedDomain;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.PhiStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;

public abstract class ValueSourceFunctions implements IPartiallyBalancedFlowFunctions<Statement> 
{
	protected class NormalFlowVisitor extends SSAInstruction.Visitor
	{
		private IUnaryFlowFunction mFlowFunct = null;
		private Statement mDestStm = null;
		public IUnaryFlowFunction visit(SSAInstruction inst, Statement destStm)
		{
			mFlowFunct = KillEverything.singleton();
			mDestStm = destStm;
			inst.visit(this);
			mDestStm = null;
			return mFlowFunct;
		}
		public IUnaryFlowFunction getFlowFunction()
		{
			return mFlowFunct;
		}
		@Override
		public void visitGet(SSAGetInstruction instruction) 
		{
			mFlowFunct = KillEverything.singleton();
		}
		@Override
		public void visitNew(SSANewInstruction instruction) 
		{
			mFlowFunct = KillEverything.singleton();
		}
		@Override
		public void visitPhi(SSAPhiInstruction instruction) 
		{
			mFlowFunct = IdentityFlowFunction.identity();
		}
		@Override
		public void visitPut(SSAPutInstruction putInst)  
		{
			if(putInst.isStatic())
				mFlowFunct = IdentityFlowFunction.identity();
			else
			{
				int valNum = putInst.getVal();
				if(!StatementUtils.isDefiningStatement(mDestStm, valNum))
					mFlowFunct = KillEverything.singleton();
				else
					mFlowFunct = IdentityFlowFunction.identity();
			}
		}
		@Override
		public void visitReturn(SSAReturnInstruction instruction) 
		{
			mFlowFunct = IdentityFlowFunction.identity();
		}
		@Override
		public void visitCheckCast(SSACheckCastInstruction instruction)
		{
			mFlowFunct = IdentityFlowFunction.identity();
		}
		@Override
		public void visitLoadMetadata(SSALoadMetadataInstruction instruction)
		{
			mFlowFunct = KillEverything.singleton();
		}
	}
	protected class KillReportFlowFunction implements IUnaryFlowFunction
	{
		private Pair<Statement, Statement> mEdge;
		public KillReportFlowFunction(Statement src, Statement dest)
		{
			mEdge = Pair.of(src, dest);
		}
		@Override
		public IntSet getTargets(int d1)
		{
			if(mEdge != null)
			{
				mKilledEdges.add(mEdge);
				mEdge = null;
			}
			return null;
		}
	}
	private final Set<Pair<Statement, Statement>> mKilledEdges = new HashSet<Pair<Statement, Statement>>();
	
	// Not used
	private final UnorderedDomain<Statement, Statement> mDomain = new UnorderedDomain<Statement, Statement>();
	private final NormalFlowVisitor mNormalSourceFlowInstVisitor = new NormalFlowVisitor();
	private final Map<Statement, MutableIntSet> mReachedStms = new HashMap<Statement, MutableIntSet>();
	private int mNextGenFact = 1;
	public ValueSourceFunctions()
	{}
	public Iterator<Pair<Statement, Statement>> getKilledEdges()
	{
		return mKilledEdges.iterator();
	}
	public TabulationDomain<Statement, Statement> getDomain()
	{
		return mDomain;
	}
	protected int getSourceDefValueNumber(Statement stm)
	{
		switch(stm.getKind())
		{
		case PARAM_CALLEE:
			{
				ParamCallee paramStm = (ParamCallee)stm;
				return paramStm.getValueNumber();
			}
		case NORMAL:
			{
				NormalStatement normalStm = (NormalStatement)stm;
				SSAInstruction inst = normalStm.getInstruction();
				return inst.getNumberOfDefs() > 0 ? inst.getDef() : -1;
			}
		case PHI:
			{
				PhiStatement phiStm = (PhiStatement)stm;
				SSAPhiInstruction inst = phiStm.getPhi();
				return inst.getDef();
			}
		case NORMAL_RET_CALLER:
			{
				NormalReturnCaller retStm = (NormalReturnCaller)stm;
				return retStm.getValueNumber();
			}
		default:
			return -1;
		}
	}
	protected NormalFlowVisitor getNormalSourceInstVisitor()
	{
		return mNormalSourceFlowInstVisitor;
	}
	@Override
	public IUnaryFlowFunction getNormalFlowFunction(Statement src,
			Statement dest)
	{
		Statement.Kind destKind = dest.getKind();
		switch(destKind)
		{
		case HEAP_RET_CALLER:
		case HEAP_RET_CALLEE:
		case HEAP_PARAM_CALLER:
		case HEAP_PARAM_CALLEE:
			return KillEverything.singleton();
		default:
			break;
		}
		Statement.Kind srcKind = src.getKind();
		switch(srcKind)
		{
		case HEAP_RET_CALLER:
		case HEAP_RET_CALLEE:
		case HEAP_PARAM_CALLER:
		case HEAP_PARAM_CALLEE:
		case NORMAL_RET_CALLER:
			return KillEverything.singleton();
		case NORMAL:
			{
				SSAInstruction inst = ((NormalStatement)src).getInstruction();
				return getNormalSourceInstVisitor().visit(inst, dest);
			}
		default:
			return IdentityFlowFunction.identity();
		}
	}

	@Override
	public abstract IUnaryFlowFunction getCallFlowFunction(Statement caller,
			Statement callee, Statement ret);

	@Override
	public IFlowFunction getReturnFlowFunction(Statement call, Statement exit,
			Statement ret)
	{
		return getUnbalancedReturnFlowFunction(exit, ret);
	}

	@Override
	public IUnaryFlowFunction getCallToReturnFlowFunction(Statement callStm,
			Statement retStm)
	{
		IntSet facts = mReachedStms.get(retStm);
		if(facts == null)
			return KillEverything.singleton();
		else
			return new KillAllGenFlowFunction(facts);
	}

	@Override
	public IUnaryFlowFunction getCallNoneToReturnFlowFunction(Statement src,
			Statement dest)
	{
		return KillEverything.singleton();
	}

	@Override
	public IFlowFunction getUnbalancedReturnFlowFunction(Statement exit,
			Statement ret)
	{
		return IdentityFlowFunction.identity();
	}

	public void clearReachedStatements()
	{
		mReachedStms.clear();
	}
	public IntSet getStatementFacts(Statement stm)
	{
		IntSet facts = mReachedStms.get(stm);
		if(facts == null)
			return EmptyIntSet.instance;
		else
			return facts;
	}
	public boolean addReachedStatement(Statement stm, IntSet facts)
	{
		if(facts.isEmpty())
			return false;
		MutableIntSet oldFacts = mReachedStms.get(stm);
		if(oldFacts == null)
		{
			oldFacts = new BitVectorIntSet();
			mReachedStms.put(stm, oldFacts);
		}
		return oldFacts.addAll(facts);
	}
	public Iterator<Statement> getReachedStatements(final IntSet facts)
	{
		return Iterators.filter(mReachedStms.keySet().iterator(), new Predicate<Statement>()
		{
			@Override
			public boolean apply(Statement stm)
			{
				IntSet oldFact = mReachedStms.get(stm);
				if(oldFact == null)
					return facts.isEmpty();
				else
					return facts.isSubset(oldFact);
			}	
		});
	}
	public void addRet2ParamFlow(Statement paramStm)
	{
		MutableIntSet oldFacts = mReachedStms.get(paramStm);
		if(oldFacts == null)
		{
			MutableIntSet facts = new BitVectorIntSet();
			facts.add(mNextGenFact++);
			mReachedStms.put(paramStm, facts);
		}
		else if(oldFacts.isEmpty())
		{
			oldFacts.add(mNextGenFact++);
		}
	}
	public void setNextGenFact(int val)
	{
		mNextGenFact = val;
	}
	public int getNextGenFact()
	{
		return mNextGenFact;
	}
}