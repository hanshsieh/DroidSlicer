package org.droidslicer.graph.entity.resolver;

import org.droidslicer.graph.entity.CFileInputUnit;
import org.droidslicer.graph.entity.CFileOutputUnit;
import org.droidslicer.graph.entity.FileUnit;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;
import org.droidslicer.value.AbstractFileStreamValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.solver.ConcreteValueSolver;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public abstract class FileInOutAllocResolver extends AllocationEntityResolver 
{
	public static final int DEFAULT_RESOLVE_DEPTH = 4;
	private final boolean mIsInput;
	public FileInOutAllocResolver(TypeReference type)
	{
		super(type);
		if(Utils.equalIgnoreLoader(type, TypeId.FILE_INPUT_STREAM.getTypeReference()))
			mIsInput = true;
		else if(Utils.equalIgnoreLoader(type, TypeId.FILE_OUTPUT_STREAM.getTypeReference()))
			mIsInput = false;
		else
			throw new IllegalArgumentException("The type must be java.io.FileInputStream or java.io.FileOutputStream");
	}
	@Override
	public void resolve(UnitsResolverContext ctx, 
			SSANewInstruction newInst, int instIdx, ProgressMonitor monitor)
		throws CancelException
	{
		monitor.beginTask("Solving allocation site of File in/out entity", 100);
		try
		{
			CGNode node = ctx.getCurrentNode();
			// Resolve the information of the file
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			ConcreteValue pathVal;
			NormalStatement newStm = new NormalStatement(node, instIdx);
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				ConcreteValue fileStreamVal = valSolver.solve(newStm, node, instIdx, newInst.getConcreteType(), getResolveDepth(), subMonitor);
				pathVal = AbstractFileStreamValue.resolvePathValue(fileStreamVal);
			}

			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				FileUnit entity;
				if(mIsInput)
				{
					entity = CFileInputUnit.make(ctx.getAnalysisContext(), newStm, pathVal, subMonitor);
				}
				else
				{
					entity = CFileOutputUnit.make(ctx.getAnalysisContext(), newStm, pathVal, subMonitor);
				}
				ctx.addUnit(entity);
			}
		}
		finally
		{
			monitor.done();
		}
	}
	/*
	private static class FileInOutUseVisitor extends DemandInstanceUseVisitor
	{
		private Set<ParamCaller> mPaths = new HashSet<ParamCaller>();
		private MethodReference mOpenMethod;
		public FileInOutUseVisitor(EntityResolverContext ctx, InstanceKeyAndState instance, boolean isInput)
		{
			super(instance, DemandInstanceUseVisitor.Usage.INVOCATION_RECEIVER);
			mOpenMethod = isInput ? MethodId.METHOD_FILE_INPUT_STREAM_OPEN : MethodId.METHOD_FILE_OUTPUT_STREAM_OPEN;
		}
		public Collection<ParamCaller> getPaths()
		{
			return mPaths;
		}
		@Override
		public void visitInvoke(SSAInvokeInstruction invokeInst)
		{
			CGNode node = getNode();
			MethodReference declaredTarget = invokeInst.getDeclaredTarget();
			
			// TODO Should we use concrete targets?
			if(!Utils.equalIgnoreLoader(mOpenMethod, declaredTarget))
				return;
			int pathValNum = invokeInst.getUse(1);
			mPaths.add(new ParamCaller(node, getInstructionIndex(), pathValNum));
		}
	}*/
}
