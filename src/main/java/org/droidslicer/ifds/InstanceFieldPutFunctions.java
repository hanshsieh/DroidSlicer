package org.droidslicer.ifds;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.ClassLoaderReference;

public class InstanceFieldPutFunctions implements IPartiallyBalancedFlowFunctions<Statement>
{
	private final Set<NormalStatement> mWritePointers = new HashSet<NormalStatement>();
	public Set<NormalStatement> getPutStatements()
	{
		return mWritePointers;
	}
	@Override
	public IUnaryFlowFunction getNormalFlowFunction(Statement src,
			Statement dest)
	{
		switch(dest.getKind())
		{
		case PARAM_CALLER:
			return IdentityFlowFunction.identity();
		case NORMAL:
			{
				NormalStatement normalDestStm = (NormalStatement)dest;
				SSAInstruction destInst = normalDestStm.getInstruction();
				if(destInst instanceof SSAPutInstruction)
				{
					SSAPutInstruction putInst = (SSAPutInstruction)destInst;
					boolean isRef = false, isUse = false;
					int ref = putInst.isStatic() ? -1 : putInst.getRef();
					int val = putInst.getVal();
					switch(src.getKind())
					{
					case PARAM_CALLEE:
						{
							ParamCallee paramSrcStm = (ParamCallee)src;
							int paramSrcValNum = paramSrcStm.getValueNumber();
							if(ref >= 0 && ref == paramSrcValNum)
								isRef = true;
							if(val == paramSrcValNum)
								isUse = true;
							break;
						}							
					case NORMAL:
						{
							NormalStatement normalSrcStm = (NormalStatement)src;
							SSAInstruction srcInst = normalSrcStm.getInstruction();
							int nDef = srcInst.getNumberOfDefs();
							for(int i = 0; i < nDef; ++i)
							{
								int instSrcDef = srcInst.getDef(i);
								if(ref >= 0 && instSrcDef == ref)
								{
									isRef = true;
								}
								if(val == instSrcDef)
								{
									isUse = true;
								}
							}
							break;
						}
					default:
						break;
					}
					if(isRef)
					{
						if(ref != val)
							mWritePointers.add(normalDestStm);
						if(!isUse)
							return KillEverything.singleton();
					}
					if(isUse)
						return IdentityFlowFunction.identity();
					else
						return KillEverything.singleton();
				}
				else if(destInst instanceof SSAReturnInstruction)
				{
					return IdentityFlowFunction.identity();
				}
				else
					return KillEverything.singleton();
			}
		default:
			return KillEverything.singleton();
		}
	}

	@Override
	public IUnaryFlowFunction getCallFlowFunction(Statement src,
			Statement dest, Statement ret)
	{
		IMethod destMethod = dest.getNode().getMethod();
		ClassLoaderReference destClassLoaderRef = destMethod.getDeclaringClass().getClassLoader().getReference();
		if(destMethod.isSynthetic() || !destClassLoaderRef.equals(ClassLoaderReference.Application))
			return KillEverything.singleton();
		else
			return IdentityFlowFunction.identity();
	}

	@Override
	public IFlowFunction getReturnFlowFunction(Statement call, Statement src,
			Statement dest)
	{
		return getUnbalancedReturnFlowFunction(src, dest);
	}

	@Override
	public IUnaryFlowFunction getCallToReturnFlowFunction(Statement src,
			Statement dest)
	{
		return KillEverything.singleton();
	}

	@Override
	public IUnaryFlowFunction getCallNoneToReturnFlowFunction(Statement src,
			Statement dest)
	{
		return KillEverything.singleton();
	}

	@Override
	public IFlowFunction getUnbalancedReturnFlowFunction(Statement src,
			Statement dest)
	{
		IMethod destMethod = dest.getNode().getMethod();
		ClassLoaderReference destClassLoaderRef = destMethod.getDeclaringClass().getClassLoader().getReference();
		if(destMethod.isSynthetic() || !destClassLoaderRef.equals(ClassLoaderReference.Application))
			return KillEverything.singleton();
		else
			return IdentityFlowFunction.identity();
	}

}
