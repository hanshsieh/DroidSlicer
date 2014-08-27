package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.SQLiteDbValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public class SQLiteDbSliceValueSolver extends ImmutableSliceValueSolver
{

	public SQLiteDbSliceValueSolver(ConcreteValueSolver valSolver, Statement startStm)
	{
		super(valSolver, startStm);
	}

	@Override
	protected void onInit(CGNode node, int instIdx, SSAAbstractInvokeInstruction invokeInst)
		throws CancelException
	{
		MethodReference declaredTarget = invokeInst.getDeclaredTarget();
		ProgressMonitor monitor = getProgressMonitor();
		CallRecords callRecords = getCallRecords();
		int maxDepth = getMaxDepth();
		switch(MethodId.getMethodId(declaredTarget))
		{
		case ANDROID_SQLITE_DB_INIT_STR_INT_FACTORY_HANDLER:
			{
				ConcreteValueSolver valSolver = getValueSolver();
				int pathValNum = invokeInst.getUse(1);
				ParamCaller paramStm = new ParamCaller(node, instIdx, pathValNum);
				ConcreteValue pathVal;
				ProgressMonitor subMonitor = new ProgressMonitor();
				monitor.setSubProgressMonitor(subMonitor);
				try
				{
					pathVal = valSolver.solve(paramStm, node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				finally
				{
					monitor.setSubProgressMonitor(null);
				}
				addPossibleValue(new SQLiteDbValue(pathVal));
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
		getProgressMonitor().beginTask("Solving SQLiteDatabase", 100);
	}

	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();		
	}

}
