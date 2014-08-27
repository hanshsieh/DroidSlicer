package org.droidslicer.graph.entity.resolver;

import org.droidslicer.util.ProgressMonitor;

import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public abstract class AllocationEntityResolver extends EntityResolver 
{
	private final TypeReference mType;
	public AllocationEntityResolver(TypeReference type)
	{
		mType = type;
	}
	public TypeReference getType()
	{
		return mType;
	}
	public abstract void resolve(
			UnitsResolverContext ctx, 
			SSANewInstruction newInst, 
			int instIdx,
			ProgressMonitor monitor) 
		throws CancelException;
}
