package org.droidslicer.graph.entity.resolver;

import org.droidslicer.graph.entity.CSQLiteDbInputUnit;
import org.droidslicer.graph.entity.CSQLiteDbOutputUnit;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.solver.ConcreteValueSolver;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.MutableIntSet;

public class SQLiteDatabaseResolver extends InvocationEntityResolver
{
	private final MutableIntSet mTrackParamsAndRet = new BitVectorIntSet();
	public SQLiteDatabaseResolver(MethodReference method, boolean isStatic)
	{
		super(method, isStatic);
		if(isStatic)
			throw new IllegalArgumentException("API to SQLiteDatabase must be non-static");
		TypeReference clazzTypeRef = method.getDeclaringClass();
		if(!clazzTypeRef.getName().equals(TypeId.ANDROID_SQLITE_DB.getTypeReference().getName()))
			throw new IllegalArgumentException("The declaring class of the method must be android.database.sqlite.SQLiteDatabase");
	}
	protected void checkParamIndex(int paramIdx)
	{
		int nParam = getNumberOfParameters();
		if(paramIdx < 0 || paramIdx >= nParam)
			throw new IllegalArgumentException();
	}
	public void setParamTrack(int paramIdx, boolean shouldTrack)
	{
		checkParamIndex(paramIdx);
		if(shouldTrack)
			mTrackParamsAndRet.add(paramIdx);
		else
			mTrackParamsAndRet.remove(paramIdx);
	}
	public boolean isParamTrack(int paramIdx)
	{
		checkParamIndex(paramIdx);
		return mTrackParamsAndRet.contains(paramIdx);
	}
	public void setReturnTrack(boolean shouldTrack)
	{
		int nParam = getNumberOfParameters();
		if(shouldTrack)
		{
			MethodReference methodRef = getMethodReference();
			TypeReference retTypeRef = methodRef.getReturnType();
			if(retTypeRef.equals(TypeReference.Void))
				throw new IllegalArgumentException("The method doesn't have return value");
			mTrackParamsAndRet.add(nParam);
		}
		else
			mTrackParamsAndRet.remove(nParam);
	}
	public boolean isReturnTrack()
	{
		int nParam = getNumberOfParameters();
		return mTrackParamsAndRet.contains(nParam);
	}
	@Override
	public void resolve(UnitsResolverContext ctx,
			SSAInvokeInstruction invokeInst, int instIdx,
			ProgressMonitor monitor) throws CancelException
	{
		try
		{
			monitor.beginTask("Resolving SQLiteDatabase API access", 100);
			
			int nParam = getNumberOfParameters();
			MethodReference declaredTarget = invokeInst.getDeclaredTarget();
			MethodReference methodRef = getMethodReference();
			if(!declaredTarget.getDescriptor().equals(methodRef.getDescriptor()) || 
				invokeInst.isStatic() != isStatic() ||
				invokeInst.getNumberOfParameters() != nParam)
			{
				return;
			}
			
			CGNode node = ctx.getCurrentNode();
			
			CSQLiteDbOutputUnit outputUnit = null;			
			
			for(int paramIdx = 0; paramIdx < nParam; ++paramIdx)
			{
				if(!isParamTrack(paramIdx))
					continue;
				if(outputUnit == null)
					outputUnit = new CSQLiteDbOutputUnit(node, instIdx);
				ParamCaller paramStm = new ParamCaller(node, instIdx, invokeInst.getUse(paramIdx));
				outputUnit.addInflowStatement(paramStm);
			}
			
			CSQLiteDbInputUnit inputUnit = null;
			if(isReturnTrack())
			{
				inputUnit = new CSQLiteDbInputUnit(node, instIdx);
				NormalReturnCaller retStm = new NormalReturnCaller(node, instIdx);
				inputUnit.addOutflowStatement(retStm);
			}
			
			if(outputUnit == null && inputUnit == null)
				return;
			
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			ConcreteValue dbVal;
			{
				ParamCaller dbStm = new ParamCaller(node, instIdx, invokeInst.getReceiver());
				dbVal = valSolver.solve(dbStm, node, instIdx, TypeId.ANDROID_SQLITE_DB.getTypeReference(), getResolveDepth(), new SubProgressMonitor(monitor, 95));
				dbVal = NullValue.excludeNullValue(dbVal);
			}
			
			if(outputUnit != null)
			{
				outputUnit.setValue(dbVal);
				ctx.addUnit(outputUnit);
			}
			if(inputUnit != null)
			{
				inputUnit.setValue(dbVal);
				ctx.addUnit(inputUnit);
			}
		}
		finally
		{
			monitor.done();
		}
	}
	
}
