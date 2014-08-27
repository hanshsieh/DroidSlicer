package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public class StringSliceValueSolver extends ImmutableSliceValueSolver
{

	public StringSliceValueSolver(ConcreteValueSolver valSolver,
			Statement startStm)
	{
		super(valSolver, startStm);
	}

	@Override
	protected void onInit(CGNode node, int instIdx,
			SSAAbstractInvokeInstruction invokeInst)
		throws CancelException
	{
		MethodReference declaredTarget = invokeInst.getDeclaredTarget();
		ConcreteValueSolver valSolver = getValueSolver();
		CallRecords callFlows = getCallRecords();
		ProgressMonitor monitor = getProgressMonitor();
		int maxDepth = getMaxDepth();
		switch(MethodId.getMethodId(declaredTarget))
		{
		case STR_INIT_STR:
			{
				int strValNum = invokeInst.getUse(1);
				ConcreteValue val;
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
				val = valSolver.solve(new ParamCaller(node, instIdx, strValNum), node, instIdx, TypeReference.JavaLangString, callFlows, maxDepth, subMonitor);
				addPossibleValue(val);
				break;
			}
		case STR_INIT:
			{
				addPossibleValue(ConstantStringValue.getEmptyString());
				break;
			}
		case STR_INIT_STR_BUILDER:
			{
				int strBuilderValNum = invokeInst.getUse(1);
				ConcreteValue strBuilderVal;
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
				strBuilderVal = valSolver.solve(new ParamCaller(node, instIdx, strBuilderValNum), node, instIdx, TypeReference.JavaLangStringBuilder, callFlows, maxDepth, subMonitor);
				addPossibleValue(strBuilderVal.getStringValue());
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
		getProgressMonitor().beginTask("Solving String value", 100);		
	}

	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();		
	}
	
}
