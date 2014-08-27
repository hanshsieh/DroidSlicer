package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.SQLiteOpenHelperValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;


public class SQLiteOpenHelperSliceValueSolver extends ImmutableSliceValueSolver {

	public SQLiteOpenHelperSliceValueSolver(ConcreteValueSolver valSolver, Statement startStm)
	{
		super(valSolver, startStm);
	}

	@Override
	protected void onInit(CGNode node, int instIdx, SSAAbstractInvokeInstruction invokeInst)
		throws CancelException
	{
		MethodReference declaredTarget = invokeInst.getDeclaredTarget();
		ProgressMonitor monitor = getProgressMonitor();
		ConcreteValueSolver valSolver = getValueSolver();
		CallRecords callFlows = getCallRecords();
		int maxDepth = getMaxDepth();
		switch(MethodId.getMethodId(declaredTarget))
		{
		case ANDROID_SQLITE_OPEN_HELPER_INIT_CTX_STR_FAC_INT:
		case ANDROID_SQLITE_OPEN_HELPER_INIT_CTX_STR_FAC_INT_HANDLER:
			{
				int pathValNum = invokeInst.getUse(2);
				ConcreteValue pathVal;
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
				pathVal = valSolver.solve(
						new ParamCaller(node, instIdx, pathValNum), node, instIdx, TypeReference.JavaLangString, callFlows, maxDepth, subMonitor);
				addPossibleValue(new SQLiteOpenHelperValue(pathVal));
				break;
			}
		default:
			break;
		}
	}

	@Override
	protected void onStart()
	{
		getProgressMonitor().beginTask("Solving SQLiteOpenHelper", 100);
	}

	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();
	}

}
