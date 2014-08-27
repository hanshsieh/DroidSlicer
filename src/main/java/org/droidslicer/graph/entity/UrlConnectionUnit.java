package org.droidslicer.graph.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.android.AndroidPermission;
import org.droidslicer.ifds.BypassFlowFunctions;
import org.droidslicer.ifds.CallRecords;
import org.droidslicer.ifds.DependencySolver;
import org.droidslicer.util.EntityUtils;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.UnknownValue;
import org.droidslicer.value.UrlConnectionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterators;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.SparseIntSet;

/**
 * This entity abstractly represents a URL connection. It doesn't necessary correspond to 
 * a java.net.URLConnection instance. It may, for example, represent a URL connection established by 
 * {@link org.apache.http.client.HttpClient#execute(org.apache.http.client.methods.HttpUriRequest)}.
 *
 */
public abstract class UrlConnectionUnit extends SUseUnit
{
	private final static Logger mLogger = LoggerFactory.getLogger(UrlConnectionUnit.class);
	private ConcreteValue mUrl = NullValue.getInstance();
	public UrlConnectionUnit()
	{}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(UrlConnectionUnit.class.getSimpleName());
		builder.append(" url=");
		builder.append(mUrl);
		builder.append(']');
		return builder.toString();
	}
	public void setUrlValue(ConcreteValue val)
	{
		if(val == null)
			throw new IllegalArgumentException();
		mUrl = val;
	}
	public ConcreteValue getUrlValue()
	{
		return mUrl;
	}
	@Override
	public Collection<String> getPermissions()
	{
		return Collections.singleton(AndroidPermission.INTERNET.getValue());
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitUrlConnectionUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
	protected static ConcreteValue getUrlFromUrlConnection(ConcreteValue urlConnVal)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> itr;
		if(urlConnVal instanceof OrValue)
			itr = ((OrValue)urlConnVal).iterator();
		else
			itr = Iterators.singletonIterator(urlConnVal);
		while(itr.hasNext())
		{
			ConcreteValue val = itr.next();
			if(val instanceof UrlConnectionValue)
			{
				UrlConnectionValue urlConnValReal = (UrlConnectionValue)val;
				result.addValue(urlConnValReal.getUrl());
			}
			else
				result.addValue(UnknownValue.getInstance());
		}
		return result.simplify();
	}
	protected static void addInOutFlowStatements(
			AndroidAnalysisContext analysisCtx, 
			UrlConnectionInputUnit inputUnit, 
			UrlConnectionOutputUnit outputUnit, 
			Statement defStm, 
			ProgressMonitor monitor)
			throws CancelException
	{
		monitor.beginTask("Finding in/out flow statements for UrlConnectionEntity", 100);
		try
		{
			Map<Statement, IntSet> seeds = new HashMap<Statement, IntSet>();
			
			// Prepare seeds
			seeds.put(defStm, SparseIntSet.singleton(1));
			
			// Find the invocations of library methods using the instance as 'this' pointer
					
			Set<Statement> reachedStms;
			CallRecords callRecords;
			{
				Predicate<Statement> terminatorsPred = Predicate.falsePred();
				BypassFlowFunctions functs = new BypassFlowFunctions(terminatorsPred, 0);
				functs.setCutReturnToSynthetic(false);
				functs.setRecordBypassedCalls(false);
				DependencySolver dependSolver = new DependencySolver(analysisCtx, functs);
				dependSolver.solve(seeds, new CallRecords(analysisCtx.getCallGraph()), new SubProgressMonitor(monitor, 70));
				reachedStms = dependSolver.getReachedStatements().keySet();
				callRecords = dependSolver.getCallRecords();
			}
			
			if(mLogger.isDebugEnabled())
			{
				StringBuilder builder = new StringBuilder();
				builder.append("Reached statement from seed ");
				builder.append(defStm);
				builder.append(": \n");
				for(Statement reachedStm : reachedStms)
				{
					builder.append('\t');
					builder.append(reachedStm);
					builder.append('\n');
				}					
				mLogger.debug("{}", builder.toString());
			}
			
			IClassHierarchy cha = analysisCtx.getClassHierarchy();
			
			// For each invocation
			for(Statement reachedStm : reachedStms)
			{
				// If it is a parameter
				if(!reachedStm.getKind().equals(Statement.Kind.PARAM_CALLER))
					continue;
				ParamCaller paramStm = (ParamCaller)reachedStm;
				SSAAbstractInvokeInstruction invokeInst = paramStm.getInstruction();
				
				// Check if it is the 'this' parameter of the invoke
				if(invokeInst.isStatic() || invokeInst.getNumberOfUses() <= 0)
					continue;
				int receiver = invokeInst.getReceiver();
				int valNum = paramStm.getValueNumber();
				if(receiver != valNum)
					continue;
				
				// For each callee of the invoke
				CallSiteReference callSiteRef = invokeInst.getCallSite();
				Set<CGNode> calleeNodes = analysisCtx.getCallGraph().getPossibleTargets(paramStm.getNode(), callSiteRef);
				if(!calleeNodes.isEmpty())
				{
					for(CGNode calleeNode : calleeNodes)
					{
						IMethod calleeMethod = calleeNode.getMethod();
						ClassLoaderReference calleeClassLoaderRef = calleeMethod.getDeclaringClass().getClassLoader().getReference();
						if(!calleeClassLoaderRef.equals(ClassLoaderReference.Primordial))
							continue;
						handleReceiverInvoke(analysisCtx, paramStm, calleeMethod.getReference(), inputUnit, outputUnit, callRecords, new SubProgressMonitor(monitor, 5));
					}
				}
				else // If no callee is found for the call, conservatively use the declared target
				{
					MethodReference declaredTarget = invokeInst.getDeclaredTarget();
					IMethod calleeMethod = cha.resolveMethod(declaredTarget);
					if(calleeMethod != null)
					{
						ClassLoaderReference calleeClassLoaderRef = calleeMethod.getDeclaringClass().getClassLoader().getReference();
						if(!calleeClassLoaderRef.equals(ClassLoaderReference.Primordial))
							continue;
						handleReceiverInvoke(analysisCtx, paramStm, calleeMethod.getReference(), inputUnit, outputUnit, callRecords, new SubProgressMonitor(monitor, 5));
					}
				}
			}
		}
		finally
		{
			monitor.done();
		}
	}
	protected static void handleReceiverInvoke(
			AndroidAnalysisContext analysisCtx, 
			ParamCaller paramCaller, 
			MethodReference calleeMethodRef, 
			UrlConnectionInputUnit inputUnit,
			UrlConnectionOutputUnit outputUnit,
			CallRecords callRecords,
			ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Solving method invocation of URLConnection", 100);
			CGNode callerNode = paramCaller.getNode();
			int instIdx = paramCaller.getInstructionIndex();
			switch(MethodId.getMethodId(calleeMethodRef))
			{
			case URL_CONNECTION_GET_OUTPUT_STREAM:
				{
					NormalReturnCaller retStm = new NormalReturnCaller(callerNode, instIdx);
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 100);
					Collection<Statement> streamStms = EntityUtils.computeInOutStreamFlowStatements(analysisCtx, retStm, false, callRecords, subMonitor);
					for(Statement streamStm : streamStms)
					{
						outputUnit.addInflowStatement(streamStm);
					}
					break;
				}
			case URL_CONNECTION_GET_INPUT_STREAM:
				{
					NormalReturnCaller retStm = new NormalReturnCaller(callerNode, instIdx);
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 100);
					Collection<Statement> streamStms = EntityUtils.computeInOutStreamFlowStatements(analysisCtx, retStm, true, callRecords, subMonitor);
					for(Statement streamStm : streamStms)
					{
						inputUnit.addOutflowStatement(streamStm);
					}
					break;
				}
			default:
				{
					SSAAbstractInvokeInstruction invokeInst = paramCaller.getInstruction();
					String calleeName = calleeMethodRef.getName().toString();
					if(calleeName.startsWith("get"))
					{
						NormalReturnCaller retStm = new NormalReturnCaller(callerNode, instIdx);
						inputUnit.addOutflowStatement(retStm);
					}
					
					// Else if the method name starts with "set", then mark the parameters as in-flow 
					// statements
					else if(calleeName.startsWith("set") || calleeName.startsWith("add"))
					{
						int nUse = invokeInst.getNumberOfUses();
						int valNum = paramCaller.getValueNumber();
						for(int useIdx = 1; useIdx < nUse; ++useIdx)
						{
							int useValNum = invokeInst.getUse(useIdx);
							if(useValNum == valNum)
								continue;
							ParamCaller otherParamStm = new ParamCaller(callerNode, instIdx, useValNum);
							outputUnit.addInflowStatement(otherParamStm);
						}
					}	
				}
			}
		}
		finally
		{
			monitor.done();
		}
	}
	public static Pair<UrlConnectionInputUnit, UrlConnectionOutputUnit> fromUrlConnectionValue(AndroidAnalysisContext analysisCtx, StatementWithInstructionIndex defStm, ConcreteValue urlConnVal, ProgressMonitor monitor)
		throws CancelException
	{
		ConcreteValue urlVal = getUrlFromUrlConnection(urlConnVal);
		CGNode node = defStm.getNode();
		int instIdx = defStm.getInstructionIndex();
		UrlConnectionInputUnit inputUnit = new UrlConnectionInputUnit(node, instIdx);
		UrlConnectionOutputUnit outputUnit = new UrlConnectionOutputUnit(node, instIdx);
		inputUnit.setUrlValue(urlVal);
		outputUnit.setUrlValue(urlVal);
		addInOutFlowStatements(analysisCtx, inputUnit, outputUnit, defStm, monitor);
		return Pair.of(inputUnit, outputUnit);
	}
}
