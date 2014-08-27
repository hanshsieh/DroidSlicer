package org.droidslicer.ifds;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.PhiStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.intset.IntSet;

public class InstanceUseFlowFunctions implements IDependencyFlowFunctions
{
	protected static class InstanceNormalFlowHandler extends SSAInstruction.Visitor
	{
		protected IUnaryFlowFunction mFlowFunct = null;
		protected Statement mSrcStm;
		public IUnaryFlowFunction process(Statement src, Statement dest)
		{
			mFlowFunct = KillEverything.singleton();
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
			case PHI:
			case PARAM_CALLER:
			case NORMAL_RET_CALLEE:
				mFlowFunct = IdentityFlowFunction.identity();
				break;
			default:
				break;
			}
			return mFlowFunct;
		}
		@Override
		public void visitPhi(SSAPhiInstruction instruction) 
		{
			mFlowFunct = IdentityFlowFunction.identity();
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
		protected static boolean isDefiningInstruction(SSAInstruction inst, int valNum)
		{
			int nDef = inst.getNumberOfDefs();
			int defIdx;
			for(defIdx = 0; defIdx < nDef; ++defIdx)
			{
				if(inst.getDef(defIdx) == valNum)
					break;
			}
			if(defIdx >= nDef)
				return false;
			else
				return true;
		}
		@Override
		public void visitPut(SSAPutInstruction putInst)
		{
			// Check if the dependency of the put instruction is because of the 
			// value being put, not the object reference.
			
			if(putInst.isStatic())
				return;
			
			int valNum = putInst.getVal();
			switch(mSrcStm.getKind())
			{
			case PARAM_CALLEE:
				{
					ParamCallee paramCallee = (ParamCallee)mSrcStm;
					int srcValNum = paramCallee.getValueNumber();
					if(srcValNum == valNum)
						mFlowFunct = IdentityFlowFunction.identity();
					break;
				}
			case NORMAL_RET_CALLER:
				{
					NormalReturnCaller retCaller = (NormalReturnCaller)mSrcStm;
					if(valNum == retCaller.getValueNumber())
						mFlowFunct = IdentityFlowFunction.identity();
					break;
				}
			case PHI:
				{
					PhiStatement phiStm = (PhiStatement)mSrcStm;
					SSAPhiInstruction phiInst = phiStm.getPhi();
					if(isDefiningInstruction(phiInst, valNum))
						mFlowFunct = IdentityFlowFunction.identity();
					break;
				}
			case NORMAL:
				{
					NormalStatement normalStm = (NormalStatement)mSrcStm;
					SSAInstruction srcInst = normalStm.getInstruction();
					if(isDefiningInstruction(srcInst, valNum))
						mFlowFunct = IdentityFlowFunction.identity();
					break;
				}
			default:
				break;
			}
		}
	}
	private InstanceNormalFlowHandler mInsNormalFlowHandler = new InstanceNormalFlowHandler();
	private Map<Statement, IntSet> mSeeds = new LinkedHashMap<Statement, IntSet>();

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
		return 0;
	}
	protected boolean shouldBypassCall(Statement caller, Statement callee)
	{
		if(callee == null)
			return true;
		IMethod calleeMethod = callee.getNode().getMethod();
		ClassLoaderReference calleeLoaderRef = calleeMethod.getDeclaringClass().getClassLoader().getReference();
		if(!calleeLoaderRef.equals(ClassLoaderReference.Application))
			return true;
		else
			return false;
	}
	@Override
	public IUnaryFlowFunction getNormalFlowFunction(Statement src,
			Statement dest) 
	{
		return mInsNormalFlowHandler.process(src, dest);
	}

	@Override
	public IUnaryFlowFunction getCallFlowFunction(Statement caller,
			Statement callee, Statement ret)
	{
		switch(caller.getKind())
		{
		case PARAM_CALLER:
			if(shouldBypassCall(caller, callee))
				return KillEverything.singleton();
			else
				return IdentityFlowFunction.identity();
		default:
			return KillEverything.singleton();
		}		
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
					return IdentityFlowFunction.identity();
				else
				{
					ClassLoaderReference destClassLoaderRef = destMethod.getDeclaringClass().getClassLoader().getReference();
					if(!destClassLoaderRef.equals(ClassLoaderReference.Application))
						return KillEverything.singleton(); 
					else
						return IdentityFlowFunction.identity();
				}
			}
		default:
			return KillEverything.singleton();
		}
	}
}
