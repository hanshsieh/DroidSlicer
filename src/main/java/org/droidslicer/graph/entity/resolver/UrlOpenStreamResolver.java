package org.droidslicer.graph.entity.resolver;

import java.util.Collection;

import org.droidslicer.graph.entity.UrlConnectionInputUnit;
import org.droidslicer.util.EntityUtils;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.solver.ConcreteValueSolver;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.CancelException;

public class UrlOpenStreamResolver extends InvocationEntityResolver
{
	
	public UrlOpenStreamResolver()
	{
		super(MethodId.URL_OPEN_STREAM.getMethodReference(), false);
	}

	@Override
	public void resolve(UnitsResolverContext ctx, 
			SSAInvokeInstruction invokeInst, int instIdx,
			ProgressMonitor monitor) throws CancelException
	{
		monitor.beginTask("Solving UrlConnectionEntity for URL.openStream", 100);
		try
		{
			CGNode node = ctx.getCurrentNode();
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			ConcreteValue urlVal;
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				int resolveDepth = getResolveDepth();
				int urlValNum = invokeInst.getUse(0);
				urlVal = valSolver.solve(new ParamCaller(node, instIdx, urlValNum), node, instIdx, TypeId.URL.getTypeReference(), resolveDepth, subMonitor);
			}
			UrlConnectionInputUnit inputUnit = new UrlConnectionInputUnit(node, instIdx);
			inputUnit.setUrlValue(urlVal);
			NormalReturnCaller retStm = new NormalReturnCaller(node, instIdx);
			
			Collection<Statement> flowStms;
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				flowStms = EntityUtils.computeInOutStreamFlowStatements(ctx.getAnalysisContext(), retStm, true, subMonitor);
			}
			for(Statement flowStm : flowStms)
				inputUnit.addOutflowStatement(flowStm);
			ctx.addUnit(inputUnit);
		}
		finally
		{
			monitor.done();
		}
	}

}

