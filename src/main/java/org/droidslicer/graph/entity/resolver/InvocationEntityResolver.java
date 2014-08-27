package org.droidslicer.graph.entity.resolver;

import org.droidslicer.util.ProgressMonitor;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public abstract class InvocationEntityResolver extends EntityResolver
{
	private final MethodReference mMethod;
	private final boolean mIsStatic;
	public InvocationEntityResolver(MethodReference method, boolean isStatic)
	{
		mMethod = method;
		mIsStatic = isStatic;
	}
	public boolean isStatic()
	{
		return mIsStatic;
	}
	public TypeReference getParameterType(int idx)
	{
		if(mIsStatic)
		{
			return mMethod.getParameterType(idx);
		}
		else
		{
			if(idx == 0)
				return mMethod.getDeclaringClass();
			else
				return mMethod.getParameterType(idx - 1);
		}
	}
	/**
	 * Number of parameters of the method, including implicit 'this'.
	 * @return
	 */
	public int getNumberOfParameters()
	{
		int nParam = mMethod.getNumberOfParameters();
		return mIsStatic ? nParam : nParam + 1; 
	}
	public MethodReference getMethodReference()
	{
		return mMethod;
	}
	public abstract void resolve(
			UnitsResolverContext ctx, 
			SSAInvokeInstruction invokeInst, 
			int instIdx,
			ProgressMonitor monitor) throws CancelException;
}
