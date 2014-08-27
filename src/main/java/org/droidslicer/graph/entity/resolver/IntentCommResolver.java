package org.droidslicer.graph.entity.resolver;

import org.droidslicer.graph.entity.AppComponentUnit;
import org.droidslicer.graph.entity.CIntentCommUnit;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.solver.ConcreteValueSolver;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public class IntentCommResolver extends InvocationEntityResolver
{
	private final Class<? extends AppComponentUnit> mTargetType;
	private int mIntentParamIdx = -1; 
	public IntentCommResolver(Class<? extends AppComponentUnit> targetEntityType, MethodReference methodRef, boolean isStatic, int intentParamIdx)
	{
		super(methodRef, isStatic);
		mTargetType = targetEntityType;
		int nParam = getNumberOfParameters();
		if(intentParamIdx < 0 || intentParamIdx >= nParam)
			throw new IllegalArgumentException("Illegal intent parameter index");
		TypeReference intentTypeRef;
		if(isStatic)
		{
			intentTypeRef = methodRef.getParameterType(intentParamIdx);
		}
		else
		{
			if(intentParamIdx == 0)
				intentTypeRef = methodRef.getDeclaringClass();
			else
				intentTypeRef = methodRef.getParameterType(intentParamIdx - 1);
		}
		if(!Utils.equalIgnoreLoader(intentTypeRef, TypeId.ANDROID_INTENT.getTypeReference()))
			throw new IllegalArgumentException("Parameter at index " + intentParamIdx + " isn't Intent");
		mIntentParamIdx = intentParamIdx;
	}
	public Class<? extends AppComponentUnit> getTargetType()
	{
		return mTargetType;
	}
	public int getIntentParameterIndex()
	{
		return mIntentParamIdx;
	}
	@Override
	public void resolve(UnitsResolverContext ctx, 
			SSAInvokeInstruction invokeInst, int instIdx,
			ProgressMonitor monitor) throws CancelException
	{
		try
		{
			CGNode node = ctx.getCurrentNode();
			monitor.beginTask("Solving Intent ICC entity", 100);
			if(invokeInst.isStatic())
				return;
			MethodReference declaredTarget = invokeInst.getDeclaredTarget();
			MethodReference methodRef = getMethodReference();
			if(!methodRef.getDescriptor().equals(declaredTarget.getDescriptor()))
				return;
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			int intentValNum = invokeInst.getUse(mIntentParamIdx);
			ConcreteValue intentVal;
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 100);
				int resolveDepth = getResolveDepth();
				intentVal = valSolver.solve(new ParamCaller(node, instIdx, intentValNum), node, instIdx, TypeId.ANDROID_INTENT.getTypeReference(), resolveDepth, subMonitor);
			}
			CIntentCommUnit entity = new CIntentCommUnit(mTargetType, node, instIdx);
			entity.setIntentValue(intentVal);
			ParamCaller intentParamStm = new ParamCaller(node, instIdx, intentValNum);
			entity.addInflowStatement(intentParamStm);
			ctx.addUnit(entity);
		}
		finally
		{
			monitor.done();
		}
	}
}
