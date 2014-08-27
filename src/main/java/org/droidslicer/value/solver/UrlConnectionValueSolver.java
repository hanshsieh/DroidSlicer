package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.UrlConnectionValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;

public class UrlConnectionValueSolver extends ImmutableSliceValueSolver
{

	public UrlConnectionValueSolver(ConcreteValueSolver valSolver,
			Statement startStm)
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
		CallRecords callRecords = getCallRecords();
		int maxDepth = getMaxDepth();
		switch(MethodId.getMethodId(declaredTarget))
		{
		case URL_CONNECTION_INIT_URL:
			{
				int urlValNum = invokeInst.getUse(1);
				ConcreteValue urlVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					urlVal = valSolver.solve(new ParamCaller(node, instIdx, urlValNum), node, instIdx, TypeId.URL.getTypeReference(), callRecords, maxDepth, subMonitor);
				}
				addPossibleValue(new UrlConnectionValue(urlVal));
				break;
			}
		default:
			break;
		}
	}
	@Override
	protected void onStart()
	{
		getProgressMonitor().beginTask("Solving java.net.URLConnection value", 100);		
	}
	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();
	}
}
