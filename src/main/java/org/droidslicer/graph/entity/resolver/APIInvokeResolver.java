package org.droidslicer.graph.entity.resolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.android.appSpec.AndroidListenerSpec;
import org.droidslicer.android.appSpec.EntryMethodSpec;
import org.droidslicer.config.AbstractAnalysisConfig;
import org.droidslicer.graph.BehaviorMethod;
import org.droidslicer.graph.entity.InvocationUnit;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.solver.ConcreteValueSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.MutableIntSet;

public class APIInvokeResolver extends InvocationEntityResolver
{
	private final static Logger mLogger = LoggerFactory.getLogger(APIInvokeResolver.class);
	/**
	 * Whether a parameter's value should be resolved.
	 */
	private final MutableIntSet mResolveParams = new BitVectorIntSet();
	
	/**
	 * Which parameters and whether the return value should be tracked.
	 * For a method accepting n parameters (including implicit 'this'), the 
	 * indices 0 to n - 1 are for the parameters, and the index n is for the 
	 * return value.
	 */
	private final MutableIntSet mTrackParamsAndRet = new BitVectorIntSet();
	
	/**
	 * Whether a parameter is a listener to be registered by the method.
	 */
	private final MutableIntSet mListenerParams = new BitVectorIntSet();
	private final MutableIntSet mTrackListenerParams = new BitVectorIntSet();
	private final Set<String> mPerms = new HashSet<String>(); 
	public APIInvokeResolver(MethodReference method, boolean isStatic)
	{
		super(method, isStatic);
	}
	protected void checkParamIndex(int paramIdx)
	{
		int nParam = getNumberOfParameters();
		if(paramIdx < 0 || paramIdx >= nParam)
			throw new IllegalArgumentException();
	}
	public void addPermission(String perm)
	{
		mPerms.add(perm);
	}
	public Set<String> getPermissions()
	{
		return mPerms;
	}
	public void setParamResolve(int paramIdx, boolean shouldResolve)
	{
		checkParamIndex(paramIdx);
		if(shouldResolve)
			mResolveParams.add(paramIdx);
		else
			mResolveParams.remove(paramIdx);
	}
	public boolean isParamResolve(int paramIdx)
	{
		checkParamIndex(paramIdx);
		return mResolveParams.contains(paramIdx);
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
	public void setParamListener(int paramIdx, boolean isListener)
	{
		checkParamIndex(paramIdx);
		if(isListener)
			mListenerParams.add(paramIdx);
		else
			mListenerParams.remove(paramIdx);
	}
	public boolean isParamListener(int paramIdx)
	{
		checkParamIndex(paramIdx);
		return mListenerParams.contains(paramIdx);
	}
	public void setTrackParamListener(int paramIdx, boolean track)
	{
		checkParamIndex(paramIdx);
		if(track)
			mTrackListenerParams.add(paramIdx);
		else
			mTrackListenerParams.remove(paramIdx);
	}
	public boolean isTrackParamListener(int paramIdx)
	{
		checkParamIndex(paramIdx);
		return mTrackListenerParams.contains(paramIdx);
	}
	protected void onListenerParam(UnitsResolverContext ctx, SSAInvokeInstruction invokeInst, int paramIdx, TypeReference paramTypeRef, InvocationUnit result)
	{
		CGNode node = ctx.getCurrentNode();
		AndroidAnalysisContext analysisCtx = ctx.getAnalysisContext();
		PointerAnalysis pa = analysisCtx.getPointerAnalysis();
		AbstractAnalysisConfig config = analysisCtx.getAnalysisConfig();
		IClassHierarchy cha = analysisCtx.getClassHierarchy();
		AndroidListenerSpec listenerSpec = config.getListenerSpec(paramTypeRef);
		
		IClass paramType = cha.lookupClass(paramTypeRef);
		if(listenerSpec == null)
		{
			mLogger.warn("Parameter {} of method {} is declared as a listener, but doesn't find a corresponding listener spec in analysis config. Ignore it.", paramIdx, getMethodReference());
			return;
		}
		if(paramType == null)
		{
			mLogger.warn("Parameter {} of method {} is declared as a listener, but doesn't find it in class hierarchy. Ignore it.", paramIdx, getMethodReference());
			return;
		}
		
		// Find what the concrete class the listener parameter can points to
		int paramValNum = invokeInst.getUse(paramIdx);
		LocalPointerKey pointer = new LocalPointerKey(node, paramValNum);
		for(InstanceKey instance : pa.getPointsToSet(pointer))
		{
			IClass argType = instance.getConcreteType();
			
			// Check whether the argument type is assignable to the parameter type
			if(!cha.isAssignableFrom(paramType, argType))
				continue;

			Set<CGNode> lEntryNodes = ctx.getEntryNodesForInstance(instance);
			
			// Collect the methods that should be called by the Android framework  
			Iterator<EntryMethodSpec> lMethodSpecsItr = listenerSpec.entryMethodsIterator();
			Map<IMethod, EntryMethodSpec> lMethods = new HashMap<IMethod, EntryMethodSpec>();
			while(lMethodSpecsItr.hasNext())
			{
				EntryMethodSpec lMethodSpec = lMethodSpecsItr.next();
				IMethod lMethod = argType.getMethod(lMethodSpec.getMethod().getSelector());
				if(lMethod == null)
					continue;
				lMethods.put(lMethod, lMethodSpec);
			}
			
			
			// Get the components reachable to the current node
			Set<BehaviorMethod> comps = ctx.getReachableMethods();
			
			// For each call node with the instance as 'this' pointer
			for(CGNode lEntryNode : lEntryNodes)
			{				
				IMethod lEntryMethod = lEntryNode.getMethod();							
				
				// Check if the method of the call node is one of the listener methods that will 
				// be called by the Android system
				EntryMethodSpec lMethodSpec = lMethods.get(lEntryMethod);
				if(lMethodSpec == null)
					continue;
				
				// Should we track the out-flow of the arguments when the listener is invoked?
				if(isTrackParamListener(paramIdx))
				{
					int lNParam = lEntryMethod.getNumberOfParameters();
					IR lIr = lEntryNode.getIR();
					if(lIr == null)
						continue;
					int[] lParamValNums = lIr.getParameterValueNumbers();
					for(int lParamIdx = 0; lParamIdx < lNParam; ++lParamIdx)
					{
						if(lMethodSpec.isParamTrack(lParamIdx))
						{
							ParamCallee paramStm = new ParamCallee(lEntryNode, lParamValNums[lParamIdx]);
							result.addOutflowStatement(paramStm);
						}
					}
				}
				
				// Propagate the components that can reach the current node to the methods of the listener
				ctx.addPendingNode(lEntryNode, comps);
				ctx.addExtraReachNode(node, lEntryNode);
			}
		}
	}
	@Override
	public void resolve(
			UnitsResolverContext ctx, 
			SSAInvokeInstruction invokeInst, 
			int instIdx,
			ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			CGNode node = ctx.getCurrentNode();
			MethodReference declaredTarget = invokeInst.getDeclaredTarget();
			MethodReference methodRef = getMethodReference();
			int nParam = getNumberOfParameters();
			monitor.beginTask("Resolving entity for invocation of method " + methodRef, 10 * nParam);
			
			if(!declaredTarget.getDescriptor().equals(methodRef.getDescriptor()) || 
				invokeInst.isStatic() != isStatic() ||
				invokeInst.getNumberOfParameters() != nParam)
			{
				return;
			}
			
			// Check if it has a parameter of listener type 
			InvocationUnit result = new InvocationUnit(node, instIdx, getMethodReference());
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			
			for(int paramIdx = 0; paramIdx < nParam; ++paramIdx)
			{
				TypeReference paramTypeRef;
				if(isStatic())
					paramTypeRef = methodRef.getParameterType(paramIdx);
				else if(paramIdx == 0)
					paramTypeRef = methodRef.getDeclaringClass();
				else
					paramTypeRef = methodRef.getParameterType(paramIdx - 1);
				if(isParamTrack(paramIdx))
				{
					result.addInflowStatement(result.makeParamStatement(paramIdx));
				}
				if(isParamListener(paramIdx))
				{
					onListenerParam(ctx, invokeInst, paramIdx, paramTypeRef, result);
				}
				if(isParamResolve(paramIdx))
				{
					ParamCaller paramStm = result.makeParamStatement(paramIdx);
					ConcreteValue paramVal;
					try
					{
						paramVal = valSolver.solve(paramStm, node, instIdx, paramTypeRef, getResolveDepth(), new SubProgressMonitor(monitor, 10));
					}
					finally
					{
						monitor.setSubProgressMonitor(null);
					}
					result.setParamValue(paramIdx, paramVal);
				}
			}
			if(isReturnTrack())
			{
				if(!TypeReference.Void.equals(methodRef.getReturnType()))
				{
					result.addOutflowStatement(result.makeReturnStatement());
				}
			}
			for(String perm : mPerms)
				result.addPermission(perm);
			
			// For API like View.setOnClickListener, we don't need have a unit for it.
			if(!result.getInflowStatements().isEmpty() || 
				!result.getOutflowStatements().isEmpty() ||
				!result.getPermissions().isEmpty())
			{
				ctx.addUnit(result);
			}
		}
		finally
		{
			monitor.done();
		}
	}
}
