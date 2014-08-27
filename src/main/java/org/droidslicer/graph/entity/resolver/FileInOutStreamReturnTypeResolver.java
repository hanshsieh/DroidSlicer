package org.droidslicer.graph.entity.resolver;

import org.droidslicer.graph.entity.CFileInputUnit;
import org.droidslicer.graph.entity.CFileOutputUnit;
import org.droidslicer.graph.entity.FileUnit;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.AbstractFileStreamValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.solver.ConcreteValueSolver;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;


public class FileInOutStreamReturnTypeResolver extends ReturnTypeResolver
{
	public FileInOutStreamReturnTypeResolver(boolean input)
	{
		super(input ? TypeId.FILE_INPUT_STREAM.getTypeReference() : TypeId.FILE_OUTPUT_STREAM.getTypeReference());
	}

	@Override
	public void resolve(UnitsResolverContext ctx, 
			SSAInvokeInstruction invokeInst, int instIdx, ProgressMonitor monitor) 
		throws CancelException
	{
		try
		{
			monitor.beginTask("Resolve entity for the returned FileInputStream/FileOutputStream", 100);
			CGNode node = ctx.getCurrentNode();
			TypeReference declaredRetType = invokeInst.getDeclaredResultType();
			boolean isInput;
			TypeName retTypeName = declaredRetType.getName();
			
			// Check whether the return value is a FileInputStream or FileOutputStream
			if(retTypeName.equals(TypeId.FILE_INPUT_STREAM.getTypeReference().getName()))
			{
				isInput = true;
			}
			else if(retTypeName.equals(TypeId.FILE_OUTPUT_STREAM.getTypeReference().getName()))
			{
				isInput = false;
			}
			else
				return;
			
			// Resolve the information of the file
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			ConcreteValue pathVal;
			NormalReturnCaller retStm = new NormalReturnCaller(node, instIdx);
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				int resolveDepth = getResolveDepth();
				ConcreteValue fileStreamVal = valSolver.solve(retStm, node, instIdx, invokeInst.getDeclaredResultType(), resolveDepth, subMonitor);
				pathVal = AbstractFileStreamValue.resolvePathValue(fileStreamVal);
			}
		
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				FileUnit entity;
				if(isInput)
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
