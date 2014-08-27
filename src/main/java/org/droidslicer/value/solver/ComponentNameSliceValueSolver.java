package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.value.ComponentNameValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.UnknownValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;


public class ComponentNameSliceValueSolver extends ImmutableSliceValueSolver
{

	public ComponentNameSliceValueSolver(ConcreteValueSolver valSolver,
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
		case ANDROID_COMPONENT_NAME_INIT_STR_STR:
		case ANDROID_COMPONENT_NAME_INIT_CTX_STR:
			{
				int strValNum = invokeInst.getUse(2);
				ConcreteValue strVal;
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
				strVal = valSolver.solve(new ParamCaller(node, instIdx, strValNum), node, instIdx, TypeReference.JavaLangString, callFlows, maxDepth, subMonitor);
				addPossibleValue(new ComponentNameValue(strVal));
				break;
			}
		case ANDROID_COMPONENT_NAME_INIT_CTX_CLASS:
			{
				int classValNum = invokeInst.getUse(2);
				ConcreteValue classVal;
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
				classVal = valSolver.solve(new ParamCaller(node, instIdx, classValNum), node, instIdx, TypeReference.JavaLangClass, callFlows, maxDepth, subMonitor);
				addPossibleValue(new ComponentNameValue(classVal));
				break;
			}
		case ANDROID_COMPONENT_NAME_INIT_PARCEL:
			addPossibleValue(new ComponentNameValue(UnknownValue.getInstance()));
			break;
		default:
			break;
		}
		getProgressMonitor().worked(10);
	}

	@Override
	protected void onStart()
	{
		getProgressMonitor().beginTask("Solving ComonentName value", 100);		
	}

	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();		
	}
	
}