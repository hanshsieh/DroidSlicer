package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;
import org.droidslicer.value.HttpRequestValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;

public class HttpRequestSliceValueSolver extends ImmutableSliceValueSolver
{
	protected enum InitType
	{
		EMPTY,
		URI,
		URI_STR
	}
	public HttpRequestSliceValueSolver(ConcreteValueSolver valSolver,
			Statement startStm)
	{
		super(valSolver, startStm);
	}

	@Override
	protected void onInit(CGNode node, int instIdx,
			SSAAbstractInvokeInstruction invokeInst) throws CancelException
	{
		MethodReference declaredTarget = invokeInst.getDeclaredTarget();
		ConcreteValueSolver valSolver = getValueSolver();
		ProgressMonitor monitor = getProgressMonitor();
		CallRecords callRecords = getCallRecords();
		int maxDepth = getMaxDepth();
		String httpMethod;
		InitType initType;
		switch(MethodId.getMethodId(declaredTarget))
		{
		// DELETE
		case APACHE_HTTP_DELETE_INIT:
			httpMethod = "DELETE";
			initType = InitType.EMPTY;
			break;
		case APACHE_HTTP_DELETE_INIT_URI:
			httpMethod = "DELETE";
			initType = InitType.URI;
			break;
		case APACHE_HTTP_DELETE_INIT_STR:
			httpMethod = "DELETE";
			initType = InitType.URI_STR;
			break;
		
		// GET
		case APACHE_HTTP_GET_INIT:
			httpMethod = "GET";
			initType = InitType.EMPTY;
			break;
		case APACHE_HTTP_GET_INIT_URI:
			httpMethod = "GET";
			initType = InitType.URI;
			break;
		case APACHE_HTTP_GET_INIT_STR:
			httpMethod = "GET";
			initType = InitType.URI_STR;
			break;
			
		// HEAD
		case APACHE_HTTP_HEAD_INIT:
			httpMethod = "HEAD";
			initType = InitType.EMPTY;
			break;
		case APACHE_HTTP_HEAD_INIT_URI:
			httpMethod = "HEAD";
			initType = InitType.URI;
			break;
		case APACHE_HTTP_HEAD_INIT_STR:
			httpMethod = "HEAD";
			initType = InitType.URI_STR;
			break;
			
		// OPTIONS
		case APACHE_HTTP_OPTIONS_INIT:
			httpMethod = "OPTIONS";
			initType = InitType.EMPTY;
			break;
		case APACHE_HTTP_OPTIONS_INIT_URI:
			httpMethod = "OPTIONS";
			initType = InitType.URI;
			break;
		case APACHE_HTTP_OPTIONS_INIT_STR:
			httpMethod = "OPTIONS";
			initType = InitType.URI_STR;
			break;
			
		// TRACE
		case APACHE_HTTP_TRACE_INIT:
			httpMethod = "TRACE";
			initType = InitType.EMPTY;
			break;
		case APACHE_HTTP_TRACE_INIT_URI:
			httpMethod = "TRACE";
			initType = InitType.URI;
			break;
		case APACHE_HTTP_TRACE_INIT_STR:
			httpMethod = "TRACE";
			initType = InitType.URI_STR;
			break;
			
		// POST
		case APACHE_HTTP_POST_INIT:
			httpMethod = "POST";
			initType = InitType.EMPTY;
			break;
		case APACHE_HTTP_POST_INIT_URI:
			httpMethod = "POST";
			initType = InitType.URI;
			break;
		case APACHE_HTTP_POST_INIT_STR:
			httpMethod = "POST";
			initType = InitType.URI_STR;
			break;
		
		// PUT
		case APACHE_HTTP_PUT_INIT:
			httpMethod = "PUT";
			initType = InitType.EMPTY;
			break;
		case APACHE_HTTP_PUT_INIT_URI:
			httpMethod = "PUT";
			initType = InitType.URI;
			break;
		case APACHE_HTTP_PUT_INIT_STR:
			httpMethod = "PUT";
			initType = InitType.URI_STR;
			break;
			
		default:
			return;
		}
		switch(initType)
		{
		case EMPTY:
			addPossibleValue(new HttpRequestValue(new ConstantStringValue(httpMethod), new ConstantStringValue("/")));
			monitor.worked(10);
			break;
		case URI:
			{
				int uriValNum = invokeInst.getUse(1);
				ConcreteValue uriVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					uriVal = valSolver.solve(new ParamCaller(node, instIdx, uriValNum), node, instIdx, TypeId.URI.getTypeReference(), callRecords, maxDepth, subMonitor);
				}
				addPossibleValue(new HttpRequestValue(new ConstantStringValue(httpMethod), uriVal));
			}
		case URI_STR:
			{
				int uriStrValNum = invokeInst.getUse(1);
				ConcreteValue uriStrVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					uriStrVal = valSolver.solve(new ParamCaller(node, instIdx, uriStrValNum), node, instIdx, TypeId.STRING.getTypeReference(), callRecords, maxDepth, subMonitor);
				}
				addPossibleValue(new HttpRequestValue(new ConstantStringValue(httpMethod), uriStrVal));
				break;
			}
		default:
			throw new RuntimeException("Unreachable");
		}
	}

	@Override
	protected void onStart()
	{
		getProgressMonitor().beginTask("Solving HttpRequest value", 100);
	}

	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();
	}

}
