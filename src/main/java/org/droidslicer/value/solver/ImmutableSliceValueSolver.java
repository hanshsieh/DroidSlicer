package org.droidslicer.value.solver;

import java.util.Collections;

import org.droidslicer.analysis.RecordCallTabulationSolver;
import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.OrValue;

import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;

public abstract class ImmutableSliceValueSolver
{
	private final Statement mStartStm;
	private final ConcreteValueSolver mValSolver;
	private OrValue mValue;
	private RecordCallTabulationSolver<Object> mInitSolver;
	private ProgressMonitor mMonitor;
	private int mMaxDepth = -1;
	public ImmutableSliceValueSolver(ConcreteValueSolver valSolver, Statement startStm)
	{
		mValSolver = valSolver;
		mStartStm = startStm;
	}
	public ConcreteValueSolver getValueSolver()
	{
		return mValSolver;
	}
	protected CallRecords getCallRecords()
	{
		return mInitSolver.getCallRecords();
	}
	public void addPossibleValue(ConcreteValue val)
	{
		mValue.addValue(val);
	}
	protected ProgressMonitor getProgressMonitor()
	{
		return mMonitor;
	}
	protected int getMaxDepth()
	{
		return mMaxDepth;
	}
	protected abstract void onInit(CGNode node, int instIdx, SSAAbstractInvokeInstruction invokeInst) throws CancelException;
	protected abstract void onStart();
	protected abstract void onEnd();
	
	/**
	 * Notice that this method isn't reentrant.
	 * @param oldCallRecords
	 * @param monitor
	 * @throws CancelException
	 */
	public void solve(CallRecords oldCallRecords, int maxDepth, ProgressMonitor monitor)
		throws CancelException
	{
		mMonitor = monitor;
		mMaxDepth = maxDepth;
		onStart();
		try
		{
			mValue = new OrValue();
			InstanceInitProblem problem = new InstanceInitProblem(mValSolver.getAnalysisContext().getSDG(), Collections.singleton(mStartStm));
			mInitSolver = RecordCallTabulationSolver.create(problem, oldCallRecords, monitor);
			mInitSolver.setRecordCalls(true);
			TabulationResult<Statement, PDG, Object> result;
			try
			{
				result = mInitSolver.solve();
			}
			catch (com.ibm.wala.util.CancelException ex)
			{
				throw new CancelException(ex);
			}
			for(Statement reachedStm : result.getSupergraphNodesReached())
			{
				if(!reachedStm.getKind().equals(Statement.Kind.PARAM_CALLER))
					continue;
				ParamCaller callerStm = (ParamCaller)reachedStm;
				SSAAbstractInvokeInstruction invokeInst = callerStm.getInstruction();
				if(invokeInst.isStatic() || 
						!invokeInst.isSpecial() || 
						invokeInst.getReceiver() != callerStm.getValueNumber() || 
						!invokeInst.getDeclaredTarget().getName().equals(MethodReference.initAtom))
				{
					continue;
				}
				onInit(callerStm.getNode(), callerStm.getInstructionIndex(), invokeInst);
			}
		}
		finally
		{
			onEnd();
			mMonitor = null;
		}
	}
	public ConcreteValue getValue()
	{
		return mValue.simplify();
	}
}
