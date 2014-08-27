package org.droidslicer.graph.entity.resolver;

import org.droidslicer.graph.entity.CICCReturnCallerUnit;
import org.droidslicer.graph.entity.CUriCommUnit;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;
import org.droidslicer.value.ConcreteValue;
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

public class UriCommResolver extends InvocationEntityResolver
{
	private final int mUriParamIdx; 
	private final MethodReference mTargetMethodRef;
	private final MutableIntSet mTrackParamsAndRet = new BitVectorIntSet();
	public UriCommResolver(MethodReference targetMethodRef, MethodReference methodRef, boolean isStatic, int uriParamIdx)
	{
		super(methodRef, isStatic);
		int nParam = getNumberOfParameters();
		if(uriParamIdx < 0 || uriParamIdx >= nParam)
			throw new IllegalArgumentException("Illegal intent parameter index");
		TypeReference uriTypeRef;
		if(isStatic)
		{
			uriTypeRef = methodRef.getParameterType(uriParamIdx);
		}
		else
		{
			if(uriParamIdx == 0)
				uriTypeRef = methodRef.getDeclaringClass();
			else
				uriTypeRef = methodRef.getParameterType(uriParamIdx - 1);
		}
		if(!Utils.equalIgnoreLoader(uriTypeRef, TypeId.ANDROID_URI.getTypeReference()))
			throw new IllegalArgumentException("Parameter at index " + uriParamIdx + " isn't android.net.Uri");
		mUriParamIdx = uriParamIdx;
		mTargetMethodRef = targetMethodRef;
	}
	protected void checkParamIndex(int paramIdx)
	{
		int nParam = getNumberOfParameters();
		if(paramIdx < 0 || paramIdx >= nParam)
			throw new IllegalArgumentException();
	}
	/**
	 * Set whether a parameter should be tracked.
	 * The parameter index starts from 0, and it includes the implicit
	 * 'this' if it is an instance method. 
	 * @param paramIdx the parameter index
	 * @param shouldTrack
	 */
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
			if(TypeReference.Void.equals(getMethodReference().getReturnType()))
			{
				throw new IllegalArgumentException("void return type cannot be tracked");
			}
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
		monitor.beginTask("Solving URI ICC entity", 100);
		try
		{
			if(invokeInst.isStatic())
				return;
			CGNode node = ctx.getCurrentNode();
			MethodReference declaredTarget = invokeInst.getDeclaredTarget();
			MethodReference methodRef = getMethodReference();
			if(!methodRef.getDescriptor().equals(declaredTarget.getDescriptor()))
				return;
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			int uriValNum = invokeInst.getUse(mUriParamIdx);
			ConcreteValue uriVal;
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 100);
				int resolveDepth = getResolveDepth();
				uriVal = valSolver.solve(new ParamCaller(node, instIdx, uriValNum), node, instIdx, TypeId.ANDROID_URI.getTypeReference(), resolveDepth, subMonitor);
			}
			CUriCommUnit iccParamUnit = new CUriCommUnit(mTargetMethodRef, node, instIdx);
			iccParamUnit.setUriValue(uriVal);
			int nParam = getNumberOfParameters();
			for(int paramIdx = 0; paramIdx < nParam; ++paramIdx)
			{
				if(isParamTrack(paramIdx))
				{
					ParamCaller paramStm = new ParamCaller(node, instIdx, invokeInst.getUse(paramIdx));
					iccParamUnit.addInflowStatement(paramStm);
				}
			}
			ctx.addUnit(iccParamUnit);
			if(isReturnTrack() && !TypeReference.Void.equals(methodRef.getReturnType()))
			{
				CICCReturnCallerUnit retUnit = new CICCReturnCallerUnit(node, instIdx);
				NormalReturnCaller retStm = new NormalReturnCaller(node, instIdx);
				retUnit.addOutflowStatement(retStm);
				ctx.addUnit(retUnit);
				ctx.addCall2ReturnRelation(iccParamUnit, retUnit);
			}
		}
		finally
		{
			monitor.done();
		}
	}
	public int getUriParameterIndex()
	{
		return mUriParamIdx;
	}
	public MethodReference getTargetMethod()
	{
		return mTargetMethodRef;
	}
	
}
