package org.droidslicer.graph.entity.resolver;

import org.droidslicer.util.ProgressMonitor;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public abstract class ReturnTypeResolver extends EntityResolver
{
	private final TypeReference mRetType;
	public ReturnTypeResolver(TypeReference retType)
	{
		mRetType = retType;
	}
	public TypeReference getReturnType()
	{
		return mRetType;
	}
	public abstract void resolve(
			UnitsResolverContext ctx, 
			SSAInvokeInstruction invokeInst, 
			int instIdx, 
			ProgressMonitor monitor)
		throws CancelException;
}
