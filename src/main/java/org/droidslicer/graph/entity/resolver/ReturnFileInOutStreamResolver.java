package org.droidslicer.graph.entity.resolver;

import org.droidslicer.graph.entity.CFileInputUnit;
import org.droidslicer.graph.entity.CFileOutputUnit;
import org.droidslicer.graph.entity.FileUnit;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.value.AbstractFileStreamValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.solver.ConcreteValueSolver;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public class ReturnFileInOutStreamResolver extends InvocationEntityResolver
{
	private final boolean mIsInput;
	public ReturnFileInOutStreamResolver(MethodReference methodRef, boolean isStatic, boolean isInput)
	{
		super(methodRef, isStatic);
		mIsInput = isInput;
		TypeReference retTypeRef = methodRef.getReturnType();
		if(retTypeRef.equals(TypeReference.Void) || retTypeRef.isArrayType() || !retTypeRef.isReferenceType())
			throw new IllegalArgumentException("The method must return a non-array object");
	}

	@Override
	public void resolve(UnitsResolverContext ctx, 
			SSAInvokeInstruction invokeInst, int instIdx, ProgressMonitor monitor) 
		throws CancelException
	{
		try
		{
			monitor.beginTask("Resolve entity for the returned InputStream/OutputStream", 100);
			CGNode node = ctx.getCurrentNode();
			
			// Resolve the information of the file
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			ConcreteValue pathVal;
			NormalReturnCaller retStm = new NormalReturnCaller(node, instIdx);
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				int resolveDepth = getResolveDepth();
				ConcreteValue streamVal = valSolver.solve(retStm, node, instIdx, invokeInst.getDeclaredResultType(), resolveDepth, subMonitor);
				pathVal = AbstractFileStreamValue.resolvePathValue(streamVal);
			}
		
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				FileUnit entity;
				if(mIsInput)
				{
					entity = CFileInputUnit.make(ctx.getAnalysisContext(), retStm, pathVal, subMonitor);
				}
				else
				{
					entity = CFileOutputUnit.make(ctx.getAnalysisContext(), retStm, pathVal, subMonitor);
				}
				ctx.addUnit(entity);
			}
		}
		finally
		{
			monitor.done();
		}
	}

}
