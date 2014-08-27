package org.droidslicer.value.solver;

import org.droidslicer.util.DemandInstanceUseVisitor;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.OrValue;

import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.demandpa.alg.InstanceKeyAndState;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;

public abstract class ImmutablePAValueSolver
{
	protected abstract class InstanceInitVisitor extends DemandInstanceUseVisitor
	{
		private OrValue mValue = new OrValue();
		public InstanceInitVisitor(InstanceKeyAndState instance)
		{
			super(instance, DemandInstanceUseVisitor.Usage.INVOCATION_RECEIVER);
		}
		@Override
		public void visitInvoke(SSAInvokeInstruction invokeInst)
		{
			if(invokeInst.isStatic() || 
				!invokeInst.isSpecial() || 
				!invokeInst.getDeclaredTarget().getName().equals(MethodReference.initAtom))
			{
				return;
			}
			// The instruction is "invokespecial"; thus the declared the target is the real target
			onInit(invokeInst);
		}
		protected void addPossibleValue(ConcreteValue val)
		{
			mValue.addValue(val);
		}
		protected abstract void onInit(SSAInvokeInstruction invokeInst);
		public ConcreteValue getValue()
		{
			return mValue.simplify();
		}
	}
	private final PointerKey mPointer;
	private final ConcreteValueSolver mValSolver;
	public ImmutablePAValueSolver(ConcreteValueSolver valSolver, PointerKey pointer)
	{
		mValSolver = valSolver;
		mPointer = pointer;
	}
	protected abstract InstanceInitVisitor getInstanceInitVisitor(InstanceKeyAndState instance);
	public ConcreteValueSolver getValueSolver()
	{
		return mValSolver;
	}
	public ConcreteValue solve(DemandRefinementPointsTo pointsTo)
	{
		OrValue result = new OrValue();
		for(InstanceKeyAndState instance : pointsTo.getPointsToWithStates(mPointer))
		{
			InstanceInitVisitor solver = getInstanceInitVisitor(instance);
			if(solver == null)
				continue;
			solver.run(pointsTo);
			result.addValue(solver.getValue());
		}
		return result.simplify();
	}
}
