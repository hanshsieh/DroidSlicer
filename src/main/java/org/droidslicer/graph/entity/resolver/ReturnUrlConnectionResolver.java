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
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

/**
 * It is for methods that return java.net.URLConnection, e.g. {@link java.net.URL#openConnection()}
 *
 */
public class ReturnUrlConnectionResolver extends ReturnTypeResolver
{
	public ReturnUrlConnectionResolver()
	{
		super(TypeId.URL_CONNECTION.getTypeReference());
	}
	@Override
	public void resolve(UnitsResolverContext ctx, 
			SSAInvokeInstruction invokeInst, int instIdx,
			ProgressMonitor monitor) throws CancelException
	{
		try
		{
			monitor.beginTask("Resolve entity for the returned java.net.URLConnection", 100);
			CGNode node = ctx.getCurrentNode();
			TypeReference declaredRetType = invokeInst.getDeclaredResultType();
			
			// Resolve the information of the file
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			ConcreteValue urlConnVal;
			NormalReturnCaller retStm = new NormalReturnCaller(node, instIdx);
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				int resolveDepth = getResolveDepth();
				urlConnVal = valSolver.solve(retStm, node, instIdx, declaredRetType, resolveDepth, subMonitor);
			}
		
			Pair<UrlConnectionInputUnit, UrlConnectionOutputUnit> pair;
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				pair = UrlConnectionUnit.fromUrlConnectionValue(ctx.getAnalysisContext(), retStm, urlConnVal, subMonitor);
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
