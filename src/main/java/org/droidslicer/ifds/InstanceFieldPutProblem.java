package org.droidslicer.ifds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import com.ibm.wala.dataflow.IFDS.IMergeFunction;
import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationProblem;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.UnorderedDomain;
import com.ibm.wala.ipa.slicer.MethodEntryStatement;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;

public class InstanceFieldPutProblem implements PartiallyBalancedTabulationProblem<Statement, PDG, Object>
{
	private final ISupergraph<Statement, PDG> mSupergraph;
	private final Collection<Statement> mSeeds;
	private final UnorderedDomain<Object, Statement> mDomain = new UnorderedDomain<Object, Statement>();
	private final InstanceFieldPutFunctions mFlowFunct = new InstanceFieldPutFunctions();
	public InstanceFieldPutProblem(SDG sdg, Collection<Statement> seeds)
	{
		mSupergraph = new SDGSupergraph(sdg);
		mSeeds = seeds;
	}
	@Override
	public ISupergraph<Statement, PDG> getSupergraph()
	{
		return mSupergraph;
	}

	@Override
	public TabulationDomain<Object, Statement> getDomain()
	{
		return mDomain;
	}

	@Override
	public Collection<PathEdge<Statement>> initialSeeds()
	{
		Collection<PathEdge<Statement>> result = new ArrayList<PathEdge<Statement>>();
		for (Statement stm : mSeeds) 
		{
			Statement entryStm = getFakeEntry(stm);
			PathEdge<Statement> seed = PathEdge.createPathEdge(
					entryStm, 1, stm, 1);
			result.add(seed);
		}
		return result;
	}

	@Override
	public IMergeFunction getMergeFunction()
	{
		return null;
	}

	@Override
	public IPartiallyBalancedFlowFunctions<Statement> getFunctionMap()
	{
		return mFlowFunct;
	}
	
	@Override
	public Statement getFakeEntry(Statement stm)
	{
		return new MethodEntryStatement(stm.getNode());
	}
	public Set<NormalStatement> getPutStatements()
	{
		return mFlowFunct.getPutStatements();
	}
}
