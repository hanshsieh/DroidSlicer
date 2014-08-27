package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.droidslicer.value.ConcreteValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ExceptionalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.SparseVector;
import com.ibm.wala.util.intset.IntIterator;

public class InvocationUnit extends SUseUnit implements IInstructionUnit, IStatementFlowUnit, IMutableStatementInflowUnit, IMutableStatementOutflowUnit
{
	private final SparseVector<ConcreteValue> mParamVals = new SparseVector<ConcreteValue>();
	private final Set<Statement> mInflowStms = new HashSet<Statement>();
	private final Set<Statement> mOutflowStms = new HashSet<Statement>();
	private final SparseVector<Set<TypeReference>> mRegListeners = new SparseVector<Set<TypeReference>>();
	private final Set<String> mPerms = new HashSet<String>();
	private final CGNode mNode;
	private final int mInstIdx;
	private final MethodReference mTargetMethodRef;
	public InvocationUnit(CGNode node, int instIdx, MethodReference targetMethodRef)
	{
		if(node == null || instIdx < 0 || targetMethodRef == null)
			throw new IllegalArgumentException();
		mNode = node;
		mInstIdx = instIdx;
		mTargetMethodRef = targetMethodRef;
		SSAInstruction inst = node.getIR().getInstructions()[instIdx];
		if(!(inst instanceof SSAAbstractInvokeInstruction))
			throw new IllegalArgumentException("The instruction must be invoke instruction");
	}
	public TypeReference getParamType(int paramIdx)
	{
		SSAAbstractInvokeInstruction invokeInst = getInvokeInstruction();
		int nParam = invokeInst.getNumberOfParameters();
		if(paramIdx < 0 || paramIdx >= nParam)
			throw new IllegalArgumentException();
		boolean isStatic = invokeInst.isStatic();
		MethodReference declaredTarget = invokeInst.getDeclaredTarget();
		if(isStatic)
			return declaredTarget.getParameterType(paramIdx);
		else
		{
			if(paramIdx == 0)
				return declaredTarget.getDeclaringClass();
			else
				return declaredTarget.getParameterType(paramIdx);
		}
	}
	public void setParamValue(int paramIdx, ConcreteValue paramVal)
	{
		SSAAbstractInvokeInstruction invokeInst = getInvokeInstruction();
		int nParam = invokeInst.getNumberOfParameters();
		if(paramIdx < 0 || paramIdx >= nParam)
			throw new IllegalArgumentException();
		mParamVals.set(paramIdx, paramVal);
	}
	/**
	 * Get the number of parameters including the implicit 'this'.
	 * @return
	 */
	public int getNumberOfParameters()
	{
		return getInvokeInstruction().getNumberOfParameters();
	}
	protected void checkParamIndex(int paramIdx)
	{
		int nParam = getNumberOfParameters();
		if(paramIdx < 0 || paramIdx >= nParam)
			throw new IllegalArgumentException();
	}
	public ConcreteValue getParamValue(int paramIdx)
	{
		checkParamIndex(paramIdx);
		return mParamVals.get(paramIdx);
	}
	public void addRegisteredListeners(int paramIdx, TypeReference clazzRef)
	{
		checkParamIndex(paramIdx);
		Set<TypeReference> types = mRegListeners.get(paramIdx);
		if(types == null)
		{
			types = new HashSet<TypeReference>();
			mRegListeners.set(paramIdx, types);
		}
		types.add(clazzRef);
	}
	public Collection<TypeReference> getRegisteredListeners(int paramIdx)
	{
		Collection<TypeReference> result = mRegListeners.get(paramIdx);
		if(result != null)
			return result;
		else
			return Collections.emptySet();
	}
	public SSAAbstractInvokeInstruction getInvokeInstruction()
	{
		return (SSAAbstractInvokeInstruction)mNode.getIR().getInstructions()[mInstIdx];
	}
	public NormalStatement makeInvokeStatement()
	{
		return new NormalStatement(getNode(), getInstructionIndex());
	}
	public NormalReturnCaller makeReturnStatement()
	{
		return new NormalReturnCaller(getNode(), getInstructionIndex());
	}
	public ExceptionalReturnCaller makeExceptionalStatement()
	{
		return new ExceptionalReturnCaller(getNode(), getInstructionIndex());
	}
	public ParamCaller makeParamStatement(int paramIdx)
	{
		SSAInstruction inst = getInvokeInstruction();
		int nUses = inst.getNumberOfUses();
		if(paramIdx < 0 || paramIdx >= nUses)
			throw new IllegalArgumentException();
		return new ParamCaller(getNode(), getInstructionIndex(), inst.getUse(paramIdx));
	}
	public ParamCaller[] makeParamStatements()
	{
		SSAInstruction inst = getInvokeInstruction();
		int nUses = inst.getNumberOfUses();
		ParamCaller[] result = new ParamCaller[nUses];
		for(int i = 0; i < result.length; ++i)
			result[i] = new ParamCaller(getNode(), getInstructionIndex(), inst.getUse(i));
		return result;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(InvocationUnit.class.getSimpleName());
		builder.append(" inst=[");
		builder.append(getInvokeInstruction());
		builder.append("], param=[");
		{
			boolean first = true;
			IntIterator intItr = mParamVals.iterateIndices();
			while(intItr.hasNext())
			{
				if(first)
					first = false;
				else
					builder.append(", ");
				int idx = intItr.next();
				ConcreteValue val = mParamVals.get(idx);
				builder.append(idx);
				builder.append("=>");
				builder.append(val);
			}
		}
		builder.append("]");
		builder.append(", node=[");
		builder.append(getNode());
		builder.append("], target=[");
		builder.append(mTargetMethodRef);
		builder.append(']');
		builder.append(", perm=[");
		{
			boolean first = true;
			for(String perm : getPermissions())
			{
				if(first)
					first = false;
				else
					builder.append(", ");
				builder.append(perm);
			}
		}
		builder.append(']');
		builder.append(']');
		return builder.toString();
	}
	@Override
	public void addInflowStatement(Statement stm)
	{
		mInflowStms.add(stm);
	}
	@Override
	public void addOutflowStatement(Statement stm)
	{
		mOutflowStms.add(stm);
	}
	public void addPermission(String perm) 
	{
		mPerms.add(perm);
	}
	@Override
	public Collection<String> getPermissions()
	{
		return mPerms;
	}
	@Override
	public Collection<Statement> getInflowStatements() 
	{
		return mInflowStms;
	}
	@Override
	public Collection<Statement> getOutflowStatements() 
	{
		return mOutflowStms;
	}
	@Override
	public CGNode getNode() 
	{
		return mNode;
	}
	@Override
	public int getInstructionIndex() 
	{
		return mInstIdx;
	}
	public MethodReference getTargetMethod()
	{
		return mTargetMethodRef;
	}
	@Override
	public boolean isAllowFlowThrough() 
	{
		return false;
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitInvocationUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
