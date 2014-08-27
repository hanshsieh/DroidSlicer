package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;
import org.droidslicer.value.HttpHostValue;
import org.droidslicer.value.IntValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

/**
 * A value solver for org.apache.http.HttpHost using slicing.
 *
 */
public class HttpHostSliceValueSolver extends ImmutableSliceValueSolver
{
	public HttpHostSliceValueSolver(ConcreteValueSolver valSolver, Statement startStm)
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
		CallRecords callFlows = getCallRecords();
		int maxDepth = getMaxDepth();
		switch(MethodId.getMethodId(declaredTarget))
		{
		case APACHE_HTTP_HOST_INIT_STR_INT_STR:
			{
				int hostValNum = invokeInst.getUse(1);
				int portValNum = invokeInst.getUse(2);
				int schemeValNum = invokeInst.getUse(3);
				ConcreteValue hostVal, portVal, schemeVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					hostVal = valSolver.solve(new ParamCaller(node, instIdx, hostValNum), node, instIdx, TypeReference.JavaLangString, callFlows, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					portVal = valSolver.solve(new ParamCaller(node, instIdx, portValNum), node, instIdx, TypeReference.Int, callFlows, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					schemeVal = valSolver.solve(new ParamCaller(node, instIdx, schemeValNum), node, instIdx, TypeReference.JavaLangString, callFlows, maxDepth, subMonitor);
				}
				addPossibleValue(new HttpHostValue(hostVal, portVal, schemeVal));
				break;
			}
		case APACHE_HTTP_HOST_INIT_STR_INT:
			{
				int hostValNum = invokeInst.getUse(1);
				int portValNum = invokeInst.getUse(2);
				ConcreteValue hostVal, portVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					hostVal = valSolver.solve(new ParamCaller(node, instIdx, hostValNum), node, instIdx, TypeReference.JavaLangString, callFlows, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					portVal = valSolver.solve(new ParamCaller(node, instIdx, portValNum), node, instIdx, TypeReference.Int, callFlows, maxDepth, subMonitor);
				}
				addPossibleValue(new HttpHostValue(hostVal, portVal, new ConstantStringValue("http")));
				break;
			}
		case APACHE_HTTP_HOST_INIT_STR:
			{
				int hostValNum = invokeInst.getUse(1);
				ConcreteValue hostVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					hostVal = valSolver.solve(new ParamCaller(node, instIdx, hostValNum), node, instIdx, TypeReference.JavaLangString, callFlows, maxDepth, subMonitor);
				}
				addPossibleValue(new HttpHostValue(hostVal, new IntValue(80), new ConstantStringValue("http")));
				break;
			}
		case APACHE_HTTP_HOST_INIT_HTTP_HOST:
			{
				int httpHostValNum = invokeInst.getUse(1);
				ConcreteValue httpHostVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					httpHostVal = valSolver.solve(new ParamCaller(node, instIdx, httpHostValNum), node, instIdx, TypeId.APACHE_HTTP_HOST.getTypeReference(), callFlows, maxDepth, subMonitor);
				}
				addPossibleValue(httpHostVal);
				break;
			}
		default:
			break;
		}
	}
	@Override
	protected void onStart()
	{
		getProgressMonitor().beginTask("Solving HttpHost value", 100);		
	}
	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();
	}

}
