package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.FileInputStreamValue;
import org.droidslicer.value.FileValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.UnknownValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;


public class FileInputStreamSliceValueSolver extends ImmutableSliceValueSolver {

	public FileInputStreamSliceValueSolver(ConcreteValueSolver valSolver, Statement startStm)
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
		case FILE_INPUT_STREAM_INIT_FILE:
			{
				int fileValNum = invokeInst.getUse(1);
				CallRecords callFlows = getCallRecords();
				ConcreteValue val;
				ProgressMonitor subMonitor = new ProgressMonitor();
				monitor.setSubProgressMonitor(subMonitor);
				try
				{
					val = valSolver.solve(new ParamCaller(node, instIdx, fileValNum), node, instIdx, TypeId.FILE.getTypeReference(), callFlows, maxDepth, subMonitor);
				}
				finally
				{
					monitor.setSubProgressMonitor(null);
				}
				val = NullValue.excludeNullValue(val);
				addPossibleValue(new FileInputStreamValue(FileValue.resolveAbsolutePath(val)));
				break;
			}
		case FILE_INPUT_STREAM_INIT_STR:
			{
				int strValNum = invokeInst.getUse(1);
				CallRecords callFlows = getCallRecords();
				ConcreteValue val;
				ProgressMonitor subMonitor = new ProgressMonitor();
				monitor.setSubProgressMonitor(subMonitor);
				try
				{
					val = valSolver.solve(new ParamCaller(node, instIdx, strValNum), node, instIdx, TypeReference.JavaLangString, callFlows, maxDepth, subMonitor);
				}
				finally
				{
					monitor.setSubProgressMonitor(null);
				}
				val = NullValue.excludeNullValue(val);
				// On Android, the current directory is "/"
				addPossibleValue(new FileInputStreamValue(FileValue.makeAbsolutePath(val, false)));
				break;
			}
		case FILE_INPUT_STREAM_INIT_FD:
			{
				// TODO Maybe we should do better
				addPossibleValue(new FileInputStreamValue(UnknownValue.getInstance()));
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
		getProgressMonitor().beginTask("Solving FileInputStream value", 100);
	}

	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();		
	}

}
