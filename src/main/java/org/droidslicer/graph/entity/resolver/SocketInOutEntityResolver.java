package org.droidslicer.graph.entity.resolver;

import java.util.Collection;

import org.droidslicer.graph.entity.SocketInputUnit;
import org.droidslicer.graph.entity.SocketOutputUnit;
import org.droidslicer.graph.entity.SocketUnit;
import org.droidslicer.util.EntityUtils;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.solver.ConcreteValueSolver;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public class SocketInOutEntityResolver extends InvocationEntityResolver
{
	public SocketInOutEntityResolver(MethodReference methodRef)
	{
		super(methodRef, false);
		TypeReference classRef = methodRef.getDeclaringClass();
		if(!classRef.equals(TypeId.SOCKET.getTypeReference()))
			throw new IllegalArgumentException("The method must be an instance method of java.net.Socket");
		TypeReference retType = methodRef.getReturnType();
		if(!Utils.equalIgnoreLoader(retType, TypeId.OUTPUT_STREAM.getTypeReference()) && 
			!Utils.equalIgnoreLoader(retType, TypeId.INPUT_STREAM.getTypeReference()))
		{
			throw new IllegalArgumentException("Return type must be either InputStream or OutputStream");	
		}
	}

	@Override
	public void resolve(UnitsResolverContext ctx, 
			SSAInvokeInstruction invokeInst, int instIdx,
			ProgressMonitor monitor) throws CancelException
	{
		if(invokeInst.isStatic())
			return;
		MethodReference declaredTarget = invokeInst.getDeclaredTarget();
		TypeReference retType = declaredTarget.getReturnType();
		boolean isInput;
		if(Utils.equalIgnoreLoader(retType, TypeId.INPUT_STREAM.getTypeReference()))
			isInput = true;
		else if(Utils.equalIgnoreLoader(retType, TypeId.OUTPUT_STREAM.getTypeReference()))
			isInput = false;
		else
			return;
		try
		{
			CGNode node = ctx.getCurrentNode();
			monitor.beginTask("Solving Socket entity for " + (isInput ? "Socket#getInputStream()" : "Socket#getStream()"), 100);
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			ConcreteValue sockVal;
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				int resolveDepth = getResolveDepth();
				int sockValNum = invokeInst.getUse(0);
				sockVal = valSolver.solve(new ParamCaller(node, instIdx, sockValNum), node, instIdx, TypeId.SOCKET.getTypeReference(), resolveDepth, subMonitor);
			}
			SocketUnit unit;
			if(isInput)
				unit = new SocketInputUnit(node, instIdx);
			else
				unit = new SocketOutputUnit(node, instIdx);
			unit.setSocketValue(sockVal);
			NormalReturnCaller retStm = new NormalReturnCaller(node, instIdx);
			
			Collection<Statement> flowStms;
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 50);
				flowStms = EntityUtils.computeInOutStreamFlowStatements(ctx.getAnalysisContext(), retStm, isInput, subMonitor);
			}
			for(Statement flowStm : flowStms)
			{
				if(unit instanceof SocketInputUnit)
					((SocketInputUnit)unit).addOutflowStatement(flowStm);
				else
					((SocketOutputUnit)unit).addInflowStatement(flowStm);
			}		
			ctx.addUnit(unit);
		}
		finally
		{
			monitor.done();
		}
	}
}
