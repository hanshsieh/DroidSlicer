package org.droidslicer.value.solver;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.InetAddressValue;
import org.droidslicer.value.InetSocketAddressValue;
import org.droidslicer.value.NullValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public class InetSocketAddrSliceValueSolver extends ImmutableSliceValueSolver
{

	public InetSocketAddrSliceValueSolver(ConcreteValueSolver valSolver,
			Statement startStm)
	{
		super(valSolver, startStm);
	}

	@Override
	protected void onInit(CGNode node, int instIdx,
			SSAAbstractInvokeInstruction invokeInst)
		throws CancelException
	{
		MethodReference declaredTarget = invokeInst.getDeclaredTarget();
		ConcreteValueSolver valSolver = getValueSolver();
		ProgressMonitor monitor = getProgressMonitor();
		CallRecords callFlows = getCallRecords();
		int maxDepth = getMaxDepth();
		switch(MethodId.getMethodId(declaredTarget))
		{
		case INET_SOCKET_ADDR_INIT_INT:
			{
				int portValNum = invokeInst.getUse(1);
				ConcreteValue portVal;
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
				portVal = valSolver.solve(new ParamCaller(node, instIdx, portValNum), node, instIdx, TypeReference.Int, callFlows, maxDepth, subMonitor);
				addPossibleValue(new InetSocketAddressValue(NullValue.getInstance(), portVal));
				break;
			}
		case INET_SOCKET_ADDR_INIT_ADDR_INT:
			{
				int addrValNum = invokeInst.getUse(1);
				int portValNum = invokeInst.getUse(2);
				ConcreteValue addrVal, portVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					addrVal = valSolver.solve(new ParamCaller(node, instIdx, addrValNum), node, instIdx, TypeId.INET_ADDRESS.getTypeReference(), callFlows, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					portVal = valSolver.solve(new ParamCaller(node, instIdx, portValNum), node, instIdx, TypeReference.Int, callFlows, maxDepth, subMonitor);
				}
				addPossibleValue(new InetSocketAddressValue(addrVal, portVal));
				break;
			}
		case INET_SOCKET_ADDR_INIT_STR_INT:
			{
				int hostValNum = invokeInst.getUse(1);
				int portValNum = invokeInst.getUse(2);
				ConcreteValue hostVal, portVal;
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					hostVal = valSolver.solve(new ParamCaller(node, instIdx, hostValNum), node, instIdx, TypeId.STRING.getTypeReference(), callFlows, maxDepth, subMonitor);
				}
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					portVal = valSolver.solve(new ParamCaller(node, instIdx, portValNum), node, instIdx, TypeReference.Int, callFlows, maxDepth, subMonitor);
				}
				addPossibleValue(new InetSocketAddressValue(new InetAddressValue(hostVal), portVal));
				break;
			}
		default:
			break;
		}
	}

	@Override
	protected void onStart()
	{
		getProgressMonitor().beginTask("Solving java.net.InetSocketAddress value", 100);		
	}

	@Override
	protected void onEnd()
	{
		getProgressMonitor().done();		
	}
}
