package org.droidslicer.value.solver;

import java.util.Set;
import java.util.Stack;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.InetSocketAddressValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.SocketValue;
import org.droidslicer.value.UnknownValue;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;
import com.ibm.wala.util.intset.SparseIntSet;

public class SocketCFValueSolver extends ControlFlowValueSolver
{
	protected static class State
	{
		private ConcreteValue mAddr;
		public State()
		{
			mAddr = NullValue.getInstance();
		}
		public void setAddress(ConcreteValue addr)
		{
			if(addr == null)
				throw new IllegalArgumentException();
			mAddr = addr;
		}
		public ConcreteValue getAddress()
		{
			return mAddr;
		}
		public ConcreteValue genValue()
		{
			return new SocketValue(mAddr, false);
		}
	}
	private final static int MAX_NUM_PATH = 100;
	private int mNumPaths;
	private Stack<State> mStates = new Stack<State>();
	private OrValue mValue = null;
	private CGNode mCurrNode = null;
	public SocketCFValueSolver(ConcreteValueSolver valSolver, CGNode startNode, int startInstIdx, int valNum, EndCriterion endCriterion, int maxDepth)
	{
		super(valSolver, startNode, startInstIdx, SparseIntSet.singleton(valNum), endCriterion, maxDepth);
	}

	@Override
	public ConcreteValue getValue()
	{
		if(mValue == null)
			return null;
		else
			return mValue.simplify();
	}
	protected ConcreteValue generateValue()
	{
		State state = mStates.peek();
		return state.genValue();
	}
	protected void addPossibleValue(ConcreteValue val)
	{
		mValue.addValue(val); 
		++mNumPaths;
	}
	@Override
	public void onStartBasicBlock(SSACFG.BasicBlock block, int instIdx)
	{
		State state = mStates.peek();
		State newState = new State();
		newState.setAddress(state.getAddress());
		mStates.push(newState);
	}
	@Override
	public void onEndBasicBlock(SSACFG.BasicBlock block)
	{
		mStates.pop();		
	}
	@Override
	public void onStartNode(CGNode node)
	{
		mCurrNode = node;
	}
	@Override
	public void onEndNode(CGNode node)
	{
		mCurrNode = null;
	}
	@Override
	public void onStart()
	{
		assert mStates.isEmpty();
		ProgressMonitor monitor = getProgressMonitor();
		monitor.beginTask("Solving Socket value", 1000);
		mCurrNode = null;
		mStates.clear();
		mStates.add(new State());
		mValue = new OrValue();
		mNumPaths = 0;
	}
	@Override
	public void onEnd()
	{
		ProgressMonitor monitor = getProgressMonitor();
		monitor.done();
		assert mStates.size() == 1;
		mCurrNode = null;
		mStates.clear();
	}
	@Override
	public void onRevisitBasicBlock(BasicBlock block, int instIdx)
	{
		addPossibleValue(generateValue());
	}
	@Override
	public void onCutPath(BasicBlock block)
	{
		addPossibleValue(generateValue());
	}
	@Override
	public void onEndCondition(BasicBlock block)
	{
		addPossibleValue(generateValue());
	}
	@Override
	public void onTerminate(BasicBlock block)
	{
		addPossibleValue(generateValue());
		addPossibleValue(UnknownValue.getInstance());
	}
	@Override
	public void visitInvoke(SSAInvokeInstruction invokeInst) 
	{
		if(invokeInst.isStatic() || getFirstUseIndex() != 0)
		{
			setCutPath(true);
			return;
		}
		if(mNumPaths >= MAX_NUM_PATH)
		{
			setTerminate(true);
			return;
		}
		ConcreteValueSolver valSolver = getValueSolver();
		CallSiteReference callSite = invokeInst.getCallSite();
		int instIdx = getInstructionIndex();
		Set<CGNode> targets = valSolver.getAnalysisContext().getCallGraph().getPossibleTargets(mCurrNode, callSite);
		
		// TODO Currently, we only handle the case there're only one possible target
		if(targets.size() != 1)
		{
			setCutPath(true);
			return;
		}
		ProgressMonitor monitor = getProgressMonitor();
		try
		{
			setCutPath(false);
			CallRecords callRecords = getCallRecords();
			CGNode target = targets.iterator().next();
			IMethod method = target.getMethod();
			ClassLoaderReference classLoader = method.getDeclaringClass().getClassLoader().getReference();
			if(!classLoader.equals(ClassLoaderReference.Primordial))
			{
				setCutPath(true);
				return;
			}
			int maxDepth = getMaxDepth();
			switch(MethodId.getMethodId(method.getReference()))
			{
			case SOCKET_INIT:
			case SOCKET_INIT_PROXY:
				break;
			case SOCKET_INIT_STR_INT:
			case SOCKET_INIT_STR_INT_ADDR_INT:
			case SOCKET_INIT_STR_INT_BOOL:
				{
					int hostValNum = invokeInst.getUse(1);
					int portValNum = invokeInst.getUse(2);
					ConcreteValue hostVal, portVal;
					
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						hostVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, hostValNum), mCurrNode, instIdx, TypeReference.JavaLangString, callRecords, maxDepth, subMonitor);
					}
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						portVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, portValNum), mCurrNode, instIdx, TypeReference.Int, callRecords, maxDepth, subMonitor);
					}
					
					mStates.peek().setAddress(new InetSocketAddressValue(hostVal, portVal));
					break;
				}
			case SOCKET_INIT_ADDR_INT:
			case SOCKET_INIT_ADDR_INT_ADDR_INT:
			case SOCKET_INIT_ADDR_INT_BOOL:
				{
					int addrValNum = invokeInst.getUse(1);
					int portValNum = invokeInst.getUse(2);
					ConcreteValue addrVal, portVal;
					
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						addrVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, addrValNum), mCurrNode, instIdx, TypeId.INET_ADDRESS.getTypeReference(), callRecords, maxDepth, subMonitor);
					}
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						portVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, portValNum), mCurrNode, instIdx, TypeReference.Int, callRecords, maxDepth, subMonitor);
					}
					
					mStates.peek().setAddress(new InetSocketAddressValue(addrVal, portVal));
					break;
				}
			case SOCKET_CONNECT_TIMEOUT:
			case SOCKET_CONNECT:
				{
					int addrValNum = invokeInst.getUse(1);
					ConcreteValue sockAddrVal;
					
					{
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
						sockAddrVal = valSolver.solve(new ParamCaller(mCurrNode, instIdx, addrValNum), mCurrNode, instIdx, TypeId.SOCKET_ADDRESS.getTypeReference(), callRecords, maxDepth, subMonitor);
					}
					mStates.peek().setAddress(sockAddrVal);
					break;
				}
			default:
				break;
			}
		}
		catch(CancelException ex)
		{
			throw CancelRuntimeException.make("Operation canceled");
		}
	}
}
