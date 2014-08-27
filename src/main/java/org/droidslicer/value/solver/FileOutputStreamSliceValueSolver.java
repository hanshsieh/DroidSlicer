package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.FileOutputStreamValue;
import org.droidslicer.value.FileValue;
import org.droidslicer.value.UnknownValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;


public class FileOutputStreamSliceValueSolver extends ImmutableSliceValueSolver
{

	public FileOutputStreamSliceValueSolver(ConcreteValueSolver valSolver, Statement startStm)
	{
		super(valSolver, startStm);
	}

	@Override
	protected void onInit(CGNode node, int instIdx, SSAAbstractInvokeInstruction invokeInst)
		throws CancelException
	{
		MethodReference declaredTarget = invokeInst.getDeclaredTarget();
		ConcreteValueSolver valSolver = getValueSolver();
		ProgressMonitor monitor = getProgressMonitor();
		int maxDepth = getMaxDepth();
		switch(MethodId.getMethodId(declaredTarget))
		{
		case FILE_OUTPUT_STREAM_INIT_FILE:
		case FILE_OUTPUT_STREAM_INIT_FILE_BOOL:
			{
				int fileValNum = invokeInst.getUse(1);
				CallRecords callRecords = getCallRecords();
				ConcreteValue val;
				ProgressMonitor subMonitor = new ProgressMonitor();
				monitor.setSubProgressMonitor(subMonitor);
				try
				{
					val = valSolver.solve(new ParamCaller(node, instIdx, fileValNum), node, instIdx, TypeId.FILE.getTypeReference(), callRecords, maxDepth, subMonitor);
				}
				finally
				{
					monitor.setSubProgressMonitor(null);
				}
				addPossibleValue(new FileOutputStreamValue(FileValue.resolveAbsolutePath(val)));
				break;
			}
		case FILE_OUTPUT_STREAM_INIT_FD:
			{
				// TODO Maybe we should do better
				addPossibleValue(new FileOutputStreamValue(UnknownValue.getInstance()));
				break;
			}
		case FILE_OUTPUT_STREAM_INIT_STR:
		case FILE_OUTPUT_STREAM_INIT_STR_BOOL:
			{
				int strValNum = invokeInst.getUse(1);
				CallRecords callRecords = getCallRecords();
				ConcreteValue val;
				ProgressMonitor subMonitor = new ProgressMonitor();
				monitor.setSubProgressMonitor(subMonitor);
				try
				{
					val = valSolver.solve(new ParamCaller(node, instIdx, strValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				finally
				{
					monitor.setSubProgressMonitor(null);
				}

				// On Android, the current directory is "/"
				addPossibleValue(new FileOutputStreamValue(FileValue.makeAbsolutePath(val, false)));
				break;
			}
		default:
			break;
		}
		getProgressMonitor().worked(10);
	}

	@Override
	protected void onStart()
	{
		getProgressMonitor().beginTask("Solving FileOutpuStream value", 100);
	}

	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();		
	}

}
