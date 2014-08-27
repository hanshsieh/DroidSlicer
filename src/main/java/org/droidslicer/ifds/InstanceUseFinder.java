package org.droidslicer.ifds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.SparseIntSet;

public class InstanceUseFinder
{
	private static class IsReceiverPredicate implements Predicate<ParamCaller>
	{
		@Override
		public boolean apply(ParamCaller param)
		{
			SSAAbstractInvokeInstruction invokeInst = param.getInstruction();
			if(invokeInst.isStatic() || invokeInst.getNumberOfUses() <= 0)
				return false;
			int receiver = invokeInst.getReceiver();
			int valNum = param.getValueNumber();
			return receiver == valNum;
		}
	}
	private final Collection<Statement> mRootStms;
	private final AndroidAnalysisContext mAnalysisCtx;
	private final Collection<ParamCaller> mParamStms = new ArrayList<ParamCaller>();
	public InstanceUseFinder(AndroidAnalysisContext analysisCtx, Collection<Statement> rootStms)
	{
		if(analysisCtx == null || rootStms == null)
			throw new IllegalArgumentException();
		mRootStms = rootStms;
		mAnalysisCtx = analysisCtx;
	}
	public void solve(CallRecords callRecords, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Finding the usage of an instance", 100);
			mParamStms.clear();
			Map<Statement, IntSet> seeds = new HashMap<Statement, IntSet>();
			for(Statement rootStm : mRootStms)
			{
				seeds.put(rootStm, SparseIntSet.singleton(1));
			}
			Collection<Statement> reachedStms;
			{
				InstanceUseFlowFunctions functs = new InstanceUseFlowFunctions();
				DependencySolver dependSolver = new DependencySolver(mAnalysisCtx, functs);
				dependSolver.solve(seeds, callRecords, new SubProgressMonitor(monitor, 98));
				reachedStms = dependSolver.getReachedStatements().keySet();
			}
			for(Statement reachedStm : reachedStms)
			{
				if(reachedStm.getKind().equals(Statement.Kind.PARAM_CALLER))
				{
					mParamStms.add((ParamCaller)reachedStm);
				}
			}
		}
		finally
		{
			monitor.done();
		}
	}
	public Iterator<ParamCaller> getInvokeParameters()
	{
		return mParamStms.iterator();
	}
	public Iterator<ParamCaller> getInvokeReceivers()
	{
		return Iterators.filter(mParamStms.iterator(), new IsReceiverPredicate());
	}

	public Iterator<ParamCaller> getInvokeNonReceiverParameters()
	{
		return Iterators.filter(mParamStms.iterator(), Predicates.not(new IsReceiverPredicate()));	
	}	
}
