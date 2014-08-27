package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.IntValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.UnknownValue;
import org.droidslicer.value.UriValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public class UrlSliceValueSolver extends ImmutableSliceValueSolver
{

	public UrlSliceValueSolver(ConcreteValueSolver valSolver, Statement startStm)
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
		switch(MethodId.getMethodId(declaredTarget))
		{
		case URL_INIT_STR:
			{
				int urlStrValNum = invokeInst.getUse(1);
				ConcreteValue urlStrVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					urlStrVal = valSolver.solve(new ParamCaller(node, instIdx, urlStrValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				addPossibleValue(new UriValue(urlStrVal));
				break;
			}
		case URL_INIT_URL_STR:
		case URL_INIT_URL_STR_HANDLER:
			{
				// TODO Handle this case
				addPossibleValue(UnknownValue.getInstance());
				break;
			}
		case URL_INIT_STR_STR_STR:
			{
				int protocolValNum = invokeInst.getUse(1);
				int hostValNum = invokeInst.getUse(2);
				int fileValNum = invokeInst.getUse(3);
				ConcreteValue schemeVal, hostVal, fileVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					schemeVal = valSolver.solve(new ParamCaller(node, instIdx, protocolValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					
					// Passing null as the protocol will make it throw exception
					if(schemeVal instanceof OrValue)
					{
						OrValue orVal = (OrValue)schemeVal;
						orVal.remove(NullValue.getInstance());
					}
					else if(schemeVal instanceof NullValue)
						schemeVal = UnknownValue.getInstance();
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					hostVal = valSolver.solve(new ParamCaller(node, instIdx, hostValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					fileVal = valSolver.solve(new ParamCaller(node, instIdx, fileValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				
				addPossibleValue(new UriValue(schemeVal, NullValue.getInstance(), hostVal, new IntValue(-1), fileVal, NullValue.getInstance(), NullValue.getInstance()));
				break;
			}
		case URL_INIT_STR_STR_INT_STR:
		case URL_INIT_STR_STR_INT_STR_HANDLER:
			{
				int protocolValNum = invokeInst.getUse(1);
				int hostValNum = invokeInst.getUse(2);
				int portValNum = invokeInst.getUse(3);
				int fileValNum = invokeInst.getUse(4);
				ConcreteValue protocolVal, hostVal, portVal, fileVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					protocolVal = valSolver.solve(new ParamCaller(node, instIdx, protocolValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					
					// Passing null as the protocol will make it throw exception
					if(protocolVal instanceof OrValue)
					{
						OrValue orVal = (OrValue)protocolVal;
						orVal.remove(NullValue.getInstance());
					}
					else if(protocolVal instanceof NullValue)
						protocolVal = UnknownValue.getInstance();
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					hostVal = valSolver.solve(new ParamCaller(node, instIdx, hostValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					portVal = valSolver.solve(new ParamCaller(node, instIdx, portValNum), node, instIdx, TypeReference.Int, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					fileVal = valSolver.solve(new ParamCaller(node, instIdx, fileValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				
				addPossibleValue(new UriValue(protocolVal, NullValue.getInstance(), hostVal, portVal, fileVal, NullValue.getInstance(), NullValue.getInstance()));
				break;
			}
		default:
			break;
		}
	}
	@Override
	protected void onStart()
	{
		getProgressMonitor().beginTask("Solving java.net.URL value", 100);		
	}
	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();
	}
}
