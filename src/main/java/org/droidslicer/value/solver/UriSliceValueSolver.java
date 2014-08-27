package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.IntValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.UriValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;


public class UriSliceValueSolver extends ImmutableSliceValueSolver
{
	public UriSliceValueSolver(ConcreteValueSolver valSolver, Statement startStm)
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
		case URI_INIT_STR:
			{
				int uriStrValNum = invokeInst.getUse(1);
				ConcreteValue uriStrVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					uriStrVal = valSolver.solve(new ParamCaller(node, instIdx, uriStrValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				addPossibleValue(new UriValue(uriStrVal));
				break;
			}
		case URI_INIT_STR_STR_STR:
			{
				int schemeValNum = invokeInst.getUse(1);
				int sspValNum = invokeInst.getUse(2);
				int fragValNum = invokeInst.getUse(3);
				ConcreteValue schemeVal, sspVal, fragVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					schemeVal = valSolver.solve(new ParamCaller(node, instIdx, schemeValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					sspVal = valSolver.solve(new ParamCaller(node, instIdx, sspValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					fragVal = valSolver.solve(new ParamCaller(node, instIdx, fragValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				addPossibleValue(new UriValue(schemeVal, sspVal, fragVal));
				break;
			}
		case URI_INIT_STR_STR_STR_INT_STR_STR_STR:
			{
				int schemeValNum = invokeInst.getUse(1);
				int userInfoValNum = invokeInst.getUse(2);
				int hostValNum = invokeInst.getUse(3);
				int portValNum = invokeInst.getUse(4);
				int pathValNum = invokeInst.getUse(5);
				int queryValNum = invokeInst.getUse(6);
				int fragValNum = invokeInst.getUse(7);
				ConcreteValue schemeVal, userInfoVal, hostVal, portVal, pathVal, queryVal, fragVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					schemeVal = valSolver.solve(new ParamCaller(node, instIdx, schemeValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					userInfoVal = valSolver.solve(new ParamCaller(node, instIdx, userInfoValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
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
					pathVal = valSolver.solve(new ParamCaller(node, instIdx, pathValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					queryVal = valSolver.solve(new ParamCaller(node, instIdx, queryValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					fragVal = valSolver.solve(new ParamCaller(node, instIdx, fragValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				addPossibleValue(new UriValue(schemeVal, userInfoVal, hostVal, portVal, pathVal, queryVal, fragVal));
				break;
			}
		case URI_INIT_STR_STR_STR_STR:
			{
				int schemeValNum = invokeInst.getUse(1);
				int hostValNum = invokeInst.getUse(2);
				int pathValNum = invokeInst.getUse(3);
				int fragValNum = invokeInst.getUse(4);
				ConcreteValue schemeVal, hostVal, pathVal, fragVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					schemeVal = valSolver.solve(new ParamCaller(node, instIdx, schemeValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					hostVal = valSolver.solve(new ParamCaller(node, instIdx, hostValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					pathVal = valSolver.solve(new ParamCaller(node, instIdx, pathValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					fragVal = valSolver.solve(new ParamCaller(node, instIdx, fragValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				addPossibleValue(new UriValue(schemeVal, NullValue.getInstance(), hostVal, new IntValue(-1), pathVal, NullValue.getInstance(), fragVal));
				break;
			}
		case URI_INIT_STR_STR_STR_STR_STR:
			{
				int schemeValNum = invokeInst.getUse(1);
				int authValNum = invokeInst.getUse(2);
				int pathValNum = invokeInst.getUse(3);
				int queryValNum = invokeInst.getUse(4);
				int fragValNum = invokeInst.getUse(5);
				ConcreteValue schemeVal, authVal, pathVal, queryVal, fragVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					schemeVal = valSolver.solve(new ParamCaller(node, instIdx, schemeValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					authVal = valSolver.solve(new ParamCaller(node, instIdx, authValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					pathVal = valSolver.solve(new ParamCaller(node, instIdx, pathValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					queryVal = valSolver.solve(new ParamCaller(node, instIdx, queryValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					fragVal = valSolver.solve(new ParamCaller(node, instIdx, fragValNum), node, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
				}
				addPossibleValue(new UriValue(schemeVal, authVal, pathVal, queryVal, fragVal));
				break;
			}
		default:
			break;
		}
	}
	@Override
	protected void onStart()
	{
		getProgressMonitor().beginTask("Solving java.net.URI value", 100);		
	}
	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();
	}
}
