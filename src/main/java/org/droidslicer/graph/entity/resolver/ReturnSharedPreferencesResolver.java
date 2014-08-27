package org.droidslicer.graph.entity.resolver;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;

import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.graph.entity.CSharedPreferencesUnit;
import org.droidslicer.ifds.CallRecords;
import org.droidslicer.ifds.InstanceUseFinder;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.solver.ConcreteValueSolver;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public class ReturnSharedPreferencesResolver extends ReturnTypeResolver
{
	public ReturnSharedPreferencesResolver()
	{
		super(TypeId.ANDROID_SHARED_PREFERENCES.getTypeReference());
	}

	protected boolean isPossiblePrimordialTarget(CallGraph cg, CGNode node, CallSiteReference callSite)
	{
		Collection<CGNode> targets = cg.getPossibleTargets(node, callSite);
		if(targets.isEmpty())
			return true;
		else
		{
			for(CGNode target : targets)
			{
				IMethod method = target.getMethod();
				ClassLoaderReference targetClassLoaderRef = method.getDeclaringClass().getClassLoader().getReference();
				if(!targetClassLoaderRef.equals(ClassLoaderReference.Primordial))
					continue;
				return true;
			}
		}
		return false;
	}
	protected void computeEditorOutflows(UnitsResolverContext ctx, CSharedPreferencesUnit unit, Statement rootStm, ProgressMonitor monitor)
		throws CancelException 
	{
		try
		{
			monitor.beginTask("Finding out flows of SharedPreferences.Editor", 100);
			Queue<Statement> rootStms = new ArrayDeque<Statement>();
			AndroidAnalysisContext analysisCtx = ctx.getAnalysisContext();
			rootStms.add(rootStm);
			while(!rootStms.isEmpty())
			{
				rootStm = rootStms.poll();
				
				// Find the invocation with the instance as receiver
				InstanceUseFinder instUse = new InstanceUseFinder(analysisCtx, Collections.singleton(rootStm));
				instUse.solve(new CallRecords(analysisCtx.getCallGraph()), new SubProgressMonitor(monitor, 10));
				CallGraph cg = analysisCtx.getCallGraph();
				for(Iterator<ParamCaller> itr = instUse.getInvokeReceivers(); itr.hasNext(); )
				{
					ParamCaller param = itr.next();
					CGNode node = param.getNode();
					SSAAbstractInvokeInstruction invokeInst = param.getInstruction();
					MethodReference declaredTarget = invokeInst.getDeclaredTarget();
					String methodName = declaredTarget.getName().toString();
					
					// The method name doesn't start with "put"
					if(!methodName.startsWith("put"))
						continue;					
					
					if(!isPossiblePrimordialTarget(cg, node, invokeInst.getCallSite()))
						continue;
					
					// Mark the parameters except receiver as in-flow
					int nParam = invokeInst.getNumberOfParameters();
					for(int i = 1; i < nParam; ++i)
					{
						ParamCaller nonReceiverParam = new ParamCaller(node, param.getInstructionIndex(), invokeInst.getUse(i));
						unit.addInflowStatement(nonReceiverParam);
					}
					
					// If the return type is SharedPreferences.Editor
					TypeReference retTypeRef = declaredTarget.getReturnType();
					if(retTypeRef.getName().equals(TypeId.ANDROID_SHARED_PREFERENCES_EDITOR.getTypeReference().getName()))
					{
						// Push the return value into the queue
						NormalReturnCaller retCaller = new NormalReturnCaller(node, param.getInstructionIndex());
						rootStms.add(retCaller);
					}
				}
			}
		}
		finally
		{
			monitor.done();
		}
	}
	protected void compouteInOutFlows(UnitsResolverContext ctx, CSharedPreferencesUnit unit, Statement rootStm, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Finding in/out flows of SharedPreferences", 100);
			// TODO Handle SharedPreferences.registerOnSharedPreferenceChangeListener
			AndroidAnalysisContext analysisCtx = ctx.getAnalysisContext();
			InstanceUseFinder instUse = new InstanceUseFinder(analysisCtx, Collections.singleton(rootStm));
			instUse.solve(new CallRecords(analysisCtx.getCallGraph()), new SubProgressMonitor(monitor, 50));
			CallGraph cg = analysisCtx.getCallGraph();
			for(Iterator<ParamCaller> itr = instUse.getInvokeReceivers(); itr.hasNext(); )
			{
				ParamCaller param = itr.next();
				CGNode node = param.getNode();
				SSAAbstractInvokeInstruction invokeInst = param.getInstruction();
				MethodReference declaredTarget = invokeInst.getDeclaredTarget();
				if(declaredTarget.getReturnType().equals(TypeReference.Void))
					continue;
				if(declaredTarget.getSelector().equals(MethodId.ANDROID_SHARED_PREFERENCES_EDIT.getMethodReference().getSelector()))
				{
					if(isPossiblePrimordialTarget(cg, node, invokeInst.getCallSite()))
					{
						NormalReturnCaller retCaller = new NormalReturnCaller(node, param.getInstructionIndex());
						computeEditorOutflows(ctx, unit, retCaller, new SubProgressMonitor(monitor, 50));
					}
				}
				else if(declaredTarget.getName().toString().startsWith("get"))
				{
					if(isPossiblePrimordialTarget(cg, node, invokeInst.getCallSite()))
					{
						NormalReturnCaller retCaller = new NormalReturnCaller(node, param.getInstructionIndex());
						unit.addOutflowStatement(retCaller);
					}
				}
			}
		}
		finally
		{
			monitor.done();
		}
	}
	@Override
	public void resolve(UnitsResolverContext ctx,
			SSAInvokeInstruction invokeInst, int instIdx,
			ProgressMonitor monitor) throws CancelException
	{
		try
		{
			monitor.beginTask("Resolve entity for the returned SharedPreference", 100);
			CGNode node = ctx.getCurrentNode();
			TypeReference declaredRetType = invokeInst.getDeclaredResultType();
			if(!declaredRetType.getName().equals(getReturnType().getName()))
				return;
			
			// Resolve the information of the file
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			ConcreteValue prefVal;
			NormalReturnCaller retStm = new NormalReturnCaller(node, instIdx);
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 5);
				int resolveDepth = getResolveDepth();
				prefVal = valSolver.solve(retStm, node, instIdx, TypeId.ANDROID_SHARED_PREFERENCES.getTypeReference(), resolveDepth, subMonitor);
			}
			
			CSharedPreferencesUnit unit = new CSharedPreferencesUnit(prefVal, node, instIdx);
			
			// Add the in/out flows of the unit
			compouteInOutFlows(ctx, unit, retStm, new SubProgressMonitor(monitor, 95));	
			ctx.addUnit(unit);
		}
		finally
		{
			monitor.done();
		}
		
	}
	
}
