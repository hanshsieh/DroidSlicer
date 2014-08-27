package org.droidslicer.value.solver;

import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.UnknownValue;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.ExceptionalReturnCaller;
import com.ibm.wala.ipa.slicer.GetCaughtExceptionStatement;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.PhiStatement;
import com.ibm.wala.ipa.slicer.PiStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public abstract class ConcreteValueSolver
{
	private final IClass mAndroidCtxClass;
	private final AndroidAnalysisContext mAnalysisCtx;
	public ConcreteValueSolver(
			AndroidAnalysisContext analysisCtx)
	{
		if(analysisCtx == null)
			throw new IllegalArgumentException();
		mAnalysisCtx = analysisCtx;
		IClassHierarchy cha = analysisCtx.getClassHierarchy();
		mAndroidCtxClass = cha.lookupClass(TypeId.ANDROID_CONTEXT.getTypeReference());
		if(mAndroidCtxClass == null)
			throw new IllegalArgumentException("Fail to find android.content.Context in class hierarchy");
	}
	public void cleanUp()
	{}
	public IClass getAndroidContextClass()
	{
		return mAndroidCtxClass;
	}
	public AndroidAnalysisContext getAnalysisContext()
	{
		return mAnalysisCtx;
	}

	public abstract ConcreteValue solve(
			Statement defStm, 
			CGNode endNode, 
			int endInstIdx, 
			TypeReference typeRef, 
			CallRecords oldCallRecords,
			int maxDepth,
			ProgressMonitor monitor) throws CancelException;
	public ConcreteValue solve(Statement defStm, CGNode endNode, int endInstIdx, TypeReference typeRef, int maxDepth, ProgressMonitor monitor) throws CancelException
	{
		CallRecords oldCallFlows = new CallRecords(mAnalysisCtx.getCallGraph());
		return solve(defStm, endNode, endInstIdx, typeRef, oldCallFlows, maxDepth, monitor);
	}
	public ConcreteValue solve(LocalPointerKey pointer, CGNode endNode, int endInstIdx, TypeReference type, int maxDepth, ProgressMonitor monitor) throws CancelException
	{
		if(pointer == null)
			throw new IllegalArgumentException();
		if(type != null && type.isPrimitiveType())
			return UnknownValue.getInstance();
		CGNode node = pointer.getNode();
		int valNum = pointer.getValueNumber();
		SymbolTable symbolTable = node.getIR().getSymbolTable();
		int[] paramValNums = symbolTable.getParameterValueNumbers();
		boolean isParam = false;
		for(int paramValNum : paramValNums)
		{
			if(paramValNum == valNum)
			{
				isParam = true;
				break;
			}
		}
		if(isParam)
		{
			// The value number is a parameter
			return solve(new ParamCallee(node, valNum), endNode, endInstIdx, type, maxDepth, monitor);
		}
		else
		{
			if(symbolTable.isConstant(valNum))
			{
				// Is constant
				Object constVal = symbolTable.getConstantValue(valNum);
				if(constVal instanceof String)
					return new ConstantStringValue((String)constVal);
				else if(constVal == null)
					return NullValue.getInstance();
				else
					return UnknownValue.getInstance();
			}
			else
			{
				// Is local variable
				SSAInstruction defInst = node.getDU().getDef(valNum);
				if(defInst instanceof SSAPhiInstruction)
				{
					return solve(new PhiStatement(node, (SSAPhiInstruction)defInst), endNode, endInstIdx, type, maxDepth, monitor);
				}
				else if(defInst instanceof SSAPiInstruction)
				{
					return solve(new PiStatement(node, (SSAPiInstruction)defInst), endNode, endInstIdx, type, maxDepth, monitor);
				}
				else if(defInst instanceof SSAGetCaughtExceptionInstruction)
				{
					return solve(new GetCaughtExceptionStatement(node, (SSAGetCaughtExceptionInstruction)defInst), endNode, endInstIdx, type, maxDepth, monitor);
				}
				else
				{
					SSAInstruction[] insts = node.getIR().getInstructions();
					for(int idx = 0; idx < insts.length; ++idx)
					{
						SSAInstruction inst = insts[idx];
						if(defInst == inst)
						{
							if(inst instanceof SSAInvokeInstruction)
							{
								SSAInvokeInstruction invokeInst = (SSAInvokeInstruction)inst;
								if(invokeInst.getNumberOfReturnValues() > 0 && invokeInst.getReturnValue(0) == valNum)
									return solve(new NormalReturnCaller(node, idx), endNode, endInstIdx, type, maxDepth, monitor);
								else if(invokeInst.getException() == valNum)
									return solve(new ExceptionalReturnCaller(node, idx), endNode, endInstIdx, type, maxDepth, monitor);
								else
									throw new RuntimeException();
							}
							else
								return solve(new NormalStatement(node, idx), endNode, endInstIdx, type, maxDepth, monitor);
						}
					}
					
					// Normally, it should only happen in synthetic method
					return UnknownValue.getInstance();
				}
			}
		}
	}
}
