package org.droidslicer.graph.entity.resolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.graph.entity.UrlConnectionInputUnit;
import org.droidslicer.graph.entity.UrlConnectionOutputUnit;
import org.droidslicer.ifds.BypassFlowFunctions;
import org.droidslicer.ifds.CallRecords;
import org.droidslicer.ifds.DependencySolver;
import org.droidslicer.util.EntityUtils;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.HttpHostValue;
import org.droidslicer.value.HttpRequestValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.UnknownValue;
import org.droidslicer.value.UriValue;
import org.droidslicer.value.solver.ConcreteValueSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterators;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.SparseIntSet;

public class HttpClientExecResolver extends InvocationEntityResolver
{
	private final Logger mLogger = LoggerFactory.getLogger(HttpClientExecResolver.class);
	private final int mHostParamIdx;
	private final int mHttpReqParamIdx;
	public HttpClientExecResolver(MethodReference method)
	{
		super(method, false);
		int nParam = method.getNumberOfParameters();
		int hostIdx = -1, httpReqIdx = -1;
		for(int i = 0; i < nParam; ++i)
		{
			TypeReference paramType = method.getParameterType(i);
			if(Utils.equalIgnoreLoader(paramType, TypeId.APACHE_HTTP_HOST.getTypeReference()))
			{
				if(hostIdx >= 0)
					throw new IllegalArgumentException("Multiple HttpHost in the method parameters");
				hostIdx = i;
			}
			else if(Utils.equalIgnoreLoader(paramType, TypeId.APACHE_HTTP_REQUEST.getTypeReference()) || 
					Utils.equalIgnoreLoader(paramType, TypeId.APACHE_HTTP_URI_REQUEST.getTypeReference()))
			{
				if(httpReqIdx >= 0)
					throw new IllegalArgumentException("Multiple HttpRequest or HttpUriRequest in the method parameters");
				httpReqIdx = i;
			}
		}
		if(httpReqIdx < 0)
			throw new IllegalArgumentException("The method parameters don't contain HttpRequest or HttpUriRequest");
		mHttpReqParamIdx = httpReqIdx;
		mHostParamIdx = hostIdx;
	}
	private ConcreteValue resolveUrlFromHttpHost(ConcreteValue httpHost)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> httpHostVals;
		if(httpHost instanceof OrValue)
			httpHostVals = ((OrValue) httpHost).iterator();
		else
			httpHostVals = Iterators.singletonIterator(httpHost);
		while(httpHostVals.hasNext())
		{
			ConcreteValue httpHostValSingle = httpHostVals.next();
			if(httpHostValSingle instanceof HttpHostValue)
			{
				HttpHostValue httpHostValReal = (HttpHostValue)httpHostValSingle;
				ConcreteValue scheme = httpHostValReal.getScheme();
				ConcreteValue hostNameVal = httpHostValReal.getHostName();
				ConcreteValue portVal = httpHostValReal.getPort();
				result.addValue(new UriValue(scheme, NullValue.getInstance(), hostNameVal, portVal, NullValue.getInstance(), NullValue.getInstance(), NullValue.getInstance()));
			}
			else
				result.addValue(UnknownValue.getInstance());
		}
		return result.simplify();
	}
	private ConcreteValue resolveUrlFromRequest(ConcreteValue httpReqVal)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> httpReqVals;
		if(httpReqVal instanceof OrValue)
			httpReqVals = ((OrValue) httpReqVal).iterator();
		else
			httpReqVals = Iterators.singletonIterator(httpReqVal);
		while(httpReqVals.hasNext())
		{
			ConcreteValue httpReqValSingle = httpReqVals.next();
			if(httpReqValSingle instanceof HttpRequestValue)
			{
				HttpRequestValue httpReqValReal = (HttpRequestValue)httpReqValSingle;
				result.addValue(httpReqValReal.getHttpUri());
			}
			else
				result.addValue(UnknownValue.getInstance());
		}
		return result.simplify();
	}
	
	/**
	* TODO Handle HttpEntity.writeTo(OutputStream)
	*/
	protected Collection<Statement> getHttpEntityOutflowStatement(
			AndroidAnalysisContext analysisCtx, Statement defStm, CallRecords callRecords, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Finding out-flow statements for HttpEntity", 100);
			Set<Statement> result = new HashSet<Statement>();
			result.add(defStm);
			
			// Prepare seeds
			Map<Statement, IntSet> seeds = new HashMap<Statement, IntSet>();
			seeds.put(defStm, SparseIntSet.singleton(1));
			Collection<Statement> reachedStms;
			{
				Predicate<Statement> terminatorsPred = Predicate.falsePred();
				BypassFlowFunctions functs = new BypassFlowFunctions(terminatorsPred, 0);
				functs.setCutReturnToSynthetic(false);
				functs.setRecordBypassedCalls(false);
				DependencySolver dependSolver = new DependencySolver(analysisCtx, functs);
				dependSolver.solve(seeds, callRecords, new SubProgressMonitor(monitor, 50));
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
				
				// Check if the return type is InputStream
				MethodReference declaredTarget = invokeInst.getDeclaredTarget();
				TypeReference retTypeRef = declaredTarget.getReturnType();
				if(retTypeRef.getName().equals(TypeId.INPUT_STREAM.getTypeReference().getName()))
				{
					NormalReturnCaller retStm = new NormalReturnCaller(paramStm.getNode(), paramStm.getInstructionIndex());
					Collection<Statement> outflows = 
							EntityUtils.computeInOutStreamFlowStatements(analysisCtx, retStm, true, callRecords, new SubProgressMonitor(monitor, 50));
					result.addAll(outflows);
				}
			}
			return result;
		}
		finally
		{
			monitor.done();
		}
	}
	protected Collection<Statement> getHttpResponseOutflowStatements(
			AndroidAnalysisContext analysisCtx, Statement defStm, CallRecords callRecords, ProgressMonitor monitor)
			throws CancelException
	{
		try
		{
			monitor.beginTask("Finding out-flow statements for HttpResponse", 100);
			Set<Statement> result = new HashSet<Statement>();
			result.add(defStm);
			Map<Statement, IntSet> seeds = new HashMap<Statement, IntSet>();
			
			// Prepare seeds
			seeds.put(defStm, SparseIntSet.singleton(1));
			Collection<Statement> reachedStms;
			{
				Predicate<Statement> terminatorsPred = Predicate.falsePred();
				BypassFlowFunctions functs = new BypassFlowFunctions(terminatorsPred, 0);
				functs.setCutReturnToSynthetic(false);
				functs.setRecordBypassedCalls(false);
				DependencySolver dependSolver = new DependencySolver(analysisCtx, functs);
				dependSolver.solve(seeds, callRecords, new SubProgressMonitor(monitor, 50));
				callRecords = dependSolver.getCallRecords();
				reachedStms = dependSolver.getReachedStatements().keySet();
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
				
				// Check if the return type if org.apache.http.HttpEntity
				MethodReference declaredTarget = invokeInst.getDeclaredTarget();
				TypeReference retTypeRef = declaredTarget.getReturnType();
				if(!retTypeRef.getName().equals(TypeId.APACHE_HTTP_ENTITY.getTypeReference().getName()))
					continue;
				
				NormalReturnCaller retStm = new NormalReturnCaller(paramStm.getNode(), paramStm.getInstructionIndex());
				CallRecords callRecordsForEntity = new CallRecords(callRecords);
				Collection<Statement> entityOutflows = 
						getHttpEntityOutflowStatement(analysisCtx, retStm, callRecordsForEntity, new SubProgressMonitor(monitor, 50));
				result.addAll(entityOutflows);
			}
			return result;
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
		monitor.beginTask("Solving invocation of exec method HttpClient", 100);
		try
		{
			CGNode node = ctx.getCurrentNode();
			MethodReference declaredTarget = invokeInst.getDeclaredTarget();
			Descriptor descriptor = declaredTarget.getDescriptor();
			if(!descriptor.equals(getMethodReference().getDescriptor()) || invokeInst.isStatic())
				return;
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			
			// Resolve the URL
			ConcreteValue url;
			{
				int resolveDepth = getResolveDepth();
				if(mHostParamIdx >= 0)
				{
					int useIdx = mHostParamIdx + 1;
					int use = invokeInst.getUse(useIdx);
					ParamCaller paramStm = new ParamCaller(node, instIdx, use);
					assert descriptor.getParameters()[mHostParamIdx].equals(TypeId.APACHE_HTTP_HOST.getTypeReference().getName());
					ConcreteValue httpHostVal = 
							valSolver.solve(paramStm, node, instIdx, TypeId.APACHE_HTTP_HOST.getTypeReference(), resolveDepth, 
									new SubProgressMonitor(monitor, 30));
					url = resolveUrlFromHttpHost(httpHostVal);
				}
				else
				{
					assert mHttpReqParamIdx >= 0;
					int useIdx = mHttpReqParamIdx + 1;
					int use = invokeInst.getUse(useIdx);
					ParamCaller paramStm = new ParamCaller(node, instIdx, use);
					TypeName paramTypeName = descriptor.getParameters()[mHttpReqParamIdx];
					TypeReference paramType;
					if(paramTypeName.equals(TypeId.APACHE_HTTP_REQUEST.getTypeReference().getName()))
						paramType = TypeId.APACHE_HTTP_REQUEST.getTypeReference();
					else
					{
						assert paramTypeName.equals(TypeId.APACHE_HTTP_URI_REQUEST.getTypeReference().getName());
						paramType = TypeId.APACHE_HTTP_URI_REQUEST.getTypeReference();
					}
					ConcreteValue httpReqVal = 
							valSolver.solve(paramStm, node, instIdx, paramType, resolveDepth, new SubProgressMonitor(monitor, 30));
					url = resolveUrlFromRequest(httpReqVal);
				}
			}
			
			assert url != null;
			UrlConnectionInputUnit inputUnit = new UrlConnectionInputUnit(node, instIdx);
			UrlConnectionOutputUnit outputUnit = new UrlConnectionOutputUnit(node, instIdx);
			inputUnit.setUrlValue(url);
			outputUnit.setUrlValue(url);
			TypeReference retType = declaredTarget.getReturnType();
			
			{
				int nParam = declaredTarget.getNumberOfParameters();
				
				// For each parameter of the invoke instruction
				for(int paramIdx = 0; paramIdx < nParam; ++paramIdx)
				{
					
					// It the parameter type is HttpRequest or HttpUriRequest
					TypeReference paramType = declaredTarget.getParameterType(paramIdx);
					if(Utils.equalIgnoreLoader(paramType, TypeId.APACHE_HTTP_REQUEST.getTypeReference()) || 
						Utils.equalIgnoreLoader(paramType, TypeId.APACHE_HTTP_URI_REQUEST.getTypeReference()))
					{
						
						// Mark the parameter as in-flow statement
						// TODO If the request object is a user class, then the dependency will be missed
						int useIdx = invokeInst.isStatic() ? paramIdx : paramIdx + 1;
						int useValNum = invokeInst.getUse(useIdx);
						ParamCaller paramStm = new ParamCaller(node, instIdx, useValNum);
						outputUnit.addInflowStatement(paramStm);
					}
				}
			}
						
			CallRecords callRecords = new CallRecords(ctx.getAnalysisContext().getCallGraph());
			
			// If the return type is HttpResponse
			if(retType.getName().equals(TypeId.APACHE_HTTP_RESPONSE.getTypeReference().getName()))
			{
			
				// Taint the return value
				NormalReturnCaller retStm = new NormalReturnCaller(node, instIdx);
				Collection<Statement> outflows = 
						getHttpResponseOutflowStatements(ctx.getAnalysisContext(), retStm, callRecords, new SubProgressMonitor(monitor, 40));
				for(Statement outflow : outflows)
					inputUnit.addOutflowStatement(outflow);
			}
			else
			{
				// Otherwise, the methods of parameter ResponseHandler will be invoked.
				// The HttpResponse	object will be passed to ResponseHandler#handlerResponse(HttpResponse).
				// Thus, we want to taint the HttpResponse parameter when the ResponseHandler is invoked.
				AndroidAnalysisContext analysisCtx = ctx.getAnalysisContext();
				AnalysisScope scope = analysisCtx.getClassHierarchy().getScope();
				CallGraph cg = analysisCtx.getCallGraph();
				boolean hasPrimordialHandler = false;
				for(CGNode target : cg.getPossibleTargets(node, invokeInst.getCallSite()))
				{
					IMethod targetMethod = target.getMethod();
					ClassLoaderReference targetClassLoaderRef = targetMethod.getDeclaringClass().getClassLoader().getReference();
					if(!targetClassLoaderRef.equals(scope.getPrimordialLoader()))
						continue;
					Iterator<CGNode> succNodes = cg.getSuccNodes(target);
					while(succNodes.hasNext())
					{
						CGNode succNode = succNodes.next();
						IMethod succMethod = succNode.getMethod();
						ClassLoaderReference succClassLoaderRef = succMethod.getDeclaringClass().getClassLoader().getReference();
						if(!succClassLoaderRef.equals(scope.getApplicationLoader()))
						{
							hasPrimordialHandler = true;
							continue;
						}
						if(succMethod.getSelector().equals(MethodId.APACHE_RESPONSE_HANDLER_HANDLE_RESPONSE.getMethodReference().getSelector()))
						{
							IR ir = succNode.getIR();
							int[] paramValNums = ir.getParameterValueNumbers();
							
							// Taint the parameters of type HttpResponse
							for(int i = 1; i < paramValNums.length; ++i)
							{
								int valNum = paramValNums[i];
								TypeReference paramTypeRef = succMethod.getParameterType(i);
								if(!paramTypeRef.getName().equals(TypeId.APACHE_HTTP_RESPONSE.getTypeReference().getName()))
									continue;
								ParamCallee paramStm = new ParamCallee(succNode, valNum);
								Collection<Statement> outflows = 
										getHttpResponseOutflowStatements(ctx.getAnalysisContext(), paramStm, callRecords, new SubProgressMonitor(monitor, 40));
								for(Statement outflow : outflows)
									inputUnit.addOutflowStatement(outflow);
							}
						}
					}
				}
				if(hasPrimordialHandler && !retType.equals(TypeReference.Void))
				{
					// Taint the return value
					NormalReturnCaller retStm = new NormalReturnCaller(node, instIdx);
					inputUnit.addOutflowStatement(retStm);
				}
			}
			ctx.addUnit(inputUnit);
			ctx.addUnit(outputUnit);
		}
		finally
		{
			monitor.done();
		}
	}
}
