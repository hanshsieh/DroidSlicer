package org.droidslicer.graph.entity.resolver;

import org.droidslicer.graph.entity.CSQLiteDbInputUnit;
import org.droidslicer.graph.entity.SQLiteDbUnit;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.solver.ConcreteValueSolver;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;


public class ReturnSQLiteDbResolver extends ReturnTypeResolver
{
	public ReturnSQLiteDbResolver()
	{
		super(TypeId.ANDROID_SQLITE_DB.getTypeReference());
	}

	@Override
	public void resolve(UnitsResolverContext ctx, 
			SSAInvokeInstruction invokeInst, int instIdx, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Solving unit for invocation instruction that returns SQLiteDatabase", 100);
			CGNode node = ctx.getCurrentNode();
			TypeReference retTypeRef = invokeInst.getDeclaredResultType();
			if(!retTypeRef.getName().equals(getReturnType().getName()))
				return;
			// Resolve the information of the database
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			ConcreteValue dbVal;
			int resolveDepth = getResolveDepth();
			dbVal = valSolver.solve(new NormalReturnCaller(node, instIdx), node, instIdx, invokeInst.getDeclaredResultType(), resolveDepth, new SubProgressMonitor(monitor, 100));
			SQLiteDbUnit entity = new CSQLiteDbInputUnit(node, instIdx);
			ctx.addUnit(entity);
			entity.setValue(dbVal);
		}
		finally
		{
			monitor.done();
		}
	}
}
