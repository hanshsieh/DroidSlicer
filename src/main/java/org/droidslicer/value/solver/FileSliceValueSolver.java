package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.FileValue;
import org.droidslicer.value.UnknownValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;


public class FileSliceValueSolver extends ImmutableSliceValueSolver {

	public FileSliceValueSolver(ConcreteValueSolver valSolver, Statement startStm)
	{
		super(valSolver, startStm);
	}

	@Override
	protected void onInit(CGNode node, int instIdx,
			SSAAbstractInvokeInstruction invokeInst) throws CancelException
	{
		MethodReference declaredTarget = invokeInst.getDeclaredTarget();
		ConcreteValueSolver valSolver = getValueSolver();
		ProgressMonitor monitor = getProgressMonitor();
		int maxDepth = getMaxDepth();
		switch(MethodId.getMethodId(declaredTarget))
		{
		case FILE_INIT_STR:
			{
				int strValNum = invokeInst.getUse(1);
				CallRecords callRecords = getCallRecords();
				ConcreteValue strVal;
				ProgressMonitor subMonitor = new ProgressMonitor();
				monitor.setSubProgressMonitor(subMonitor);
				try
				{
					strVal = valSolver.solve(new ParamCaller(node, instIdx, strValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				finally
				{
					monitor.setSubProgressMonitor(null);
				}
				addPossibleValue(new FileValue(strVal));
				break;
			}					
		case FILE_INIT_STR_STR:
			{
				int str1ValNum = invokeInst.getUse(1);
				int str2ValNum = invokeInst.getUse(2);
				ConcreteValue strVal1, strVal2;
				CallRecords callFlows = getCallRecords();
				try
				{
					ProgressMonitor subMonitor1 = new ProgressMonitor();
					monitor.setSubProgressMonitor(subMonitor1);
					strVal1 = valSolver.solve(new ParamCaller(node, instIdx, str1ValNum), node, instIdx, TypeReference.JavaLangString, callFlows, maxDepth, subMonitor1);
					
					ProgressMonitor subMonitor2 = new ProgressMonitor();
					monitor.setSubProgressMonitor(subMonitor2);
					strVal2 = valSolver.solve(new ParamCaller(node, instIdx, str2ValNum), node, instIdx, TypeReference.JavaLangString, callFlows, maxDepth, subMonitor2);
				}
				finally
				{
					monitor.setSubProgressMonitor(null);
				}
				addPossibleValue(new FileValue(strVal1, strVal2));
				break;
			}
		case FILE_INIT_FILE_STR:
			{
				int fileValNum = invokeInst.getUse(1);
				int strValNum = invokeInst.getUse(2);
				CallRecords callRecords = getCallRecords();
				ConcreteValue fileVal, strVal;
				try
				{
					ProgressMonitor subMonitor1 = new ProgressMonitor();
					monitor.setSubProgressMonitor(subMonitor1);
					if(fileValNum == invokeInst.getUse(0))
						fileVal = UnknownValue.getInstance();
					else
						fileVal = valSolver.solve(new ParamCaller(node, instIdx, fileValNum), node, instIdx, TypeId.FILE.getTypeReference(), callRecords, maxDepth, subMonitor1);
					
					ProgressMonitor subMonitor2 = new ProgressMonitor();
					monitor.setSubProgressMonitor(subMonitor2);
					strVal = valSolver.solve(new ParamCaller(node, instIdx, strValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor2);
				}
				finally
				{
					monitor.setSubProgressMonitor(null);
				}
				addPossibleValue(new FileValue(fileVal, strVal));
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
		getProgressMonitor().beginTask("Solving File value", 100);		
	}

	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();
	}

}
