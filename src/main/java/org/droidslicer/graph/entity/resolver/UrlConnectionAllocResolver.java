package org.droidslicer.graph.entity.resolver;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.graph.entity.UrlConnectionInputUnit;
import org.droidslicer.graph.entity.UrlConnectionOutputUnit;
import org.droidslicer.graph.entity.UrlConnectionUnit;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.solver.ConcreteValueSolver;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.util.CancelException;

public class UrlConnectionAllocResolver extends AllocationEntityResolver
{
	public UrlConnectionAllocResolver()
	{
		super(TypeId.URL_CONNECTION.getTypeReference());
	}

	@Override
	public void resolve(UnitsResolverContext ctx, 
			SSANewInstruction newInst, int instIdx, ProgressMonitor monitor)
			throws CancelException
	{
		try
		{
			CGNode node = ctx.getCurrentNode();
			monitor.beginTask("Solving allocation site of java.net.URLConnection", 100);
			// Resolve the information of the file
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			ConcreteValue urlConnVal;
			NormalStatement newStm = new NormalStatement(node, instIdx);
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				urlConnVal = valSolver.solve(newStm, node, instIdx, newInst.getConcreteType(), getResolveDepth(), subMonitor);
			}
			
			Pair<UrlConnectionInputUnit, UrlConnectionOutputUnit> pair;
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				pair = UrlConnectionUnit.fromUrlConnectionValue(ctx.getAnalysisContext(), newStm, urlConnVal, subMonitor);
			}
			
			ctx.addUnit(pair.getLeft());
			ctx.addUnit(pair.getRight());
		}
		finally
		{
			monitor.done();
		}
	}

}
