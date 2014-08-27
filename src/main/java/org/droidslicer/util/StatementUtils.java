package org.droidslicer.util;

import org.droidslicer.value.BoolValue;
import org.droidslicer.value.CharValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;
import org.droidslicer.value.DoubleValue;
import org.droidslicer.value.FloatValue;
import org.droidslicer.value.IntValue;
import org.droidslicer.value.LongValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.UnknownValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ExceptionalReturnCaller;
import com.ibm.wala.ipa.slicer.GetCaughtExceptionStatement;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.PhiStatement;
import com.ibm.wala.ipa.slicer.PiStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SymbolTable;

public class StatementUtils
{
	public static class ValueDef
	{
		private final boolean mIsConstant;
		private final Object mDef;
		private ValueDef(boolean isConstant, Object def)
		{
			mIsConstant = isConstant;
			mDef = def;
		}
		public static ValueDef fromConstant(Object val)
		{
			return new ValueDef(true, val);
		}
		public static ValueDef fromStatement(Statement stm)
		{
			return new ValueDef(false, stm);
		}
		public boolean isConstant()
		{
			return mIsConstant;
		}
		public Statement getDefiningStatement()
		{
			return (Statement)mDef;
		}
		public Object getConstantValue()
		{
			return mDef;
		}
	}
	public static ConcreteValue getPossibleConstantConcreteValue(CGNode node, int valNum)
	{
		SymbolTable symbolTable = node.getIR().getSymbolTable();
		if(symbolTable.isConstant(valNum))
		{
			Object constVal = symbolTable.getConstantValue(valNum);
			if(constVal == null)
				return NullValue.getInstance();
			else if(constVal instanceof String)
				return new ConstantStringValue((String)constVal);
			else if(constVal instanceof Integer)
				return new IntValue((Integer)constVal);
			else if(constVal instanceof Float)
			{
				return new FloatValue((Float)constVal);
			}
			else if(constVal instanceof Double)
			{
				return new DoubleValue((Float)constVal);
			}
			else if(constVal instanceof Long)
			{
				return new LongValue((Long)constVal);
			}
			else if(constVal instanceof Character)
			{
				return new CharValue((Character)constVal);
			}
			else if(constVal instanceof Boolean)
			{
				return new BoolValue((Boolean)constVal);
			}
			else
				return UnknownValue.getInstance();
		}
		return null;
	}
	public static ValueDef getValNumDefStatement(CGNode node, int valNum)
	{
		IR ir = node.getIR();
		if(ir == null)
			return null;
		SymbolTable symbolTable = node.getIR().getSymbolTable();
		if(symbolTable.isConstant(valNum))
			return ValueDef.fromConstant(symbolTable.getConstantValue(valNum));
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
			return ValueDef.fromStatement(new ParamCallee(node, valNum));
		}
		else
		{
			// Is local variable
			SSAInstruction defInst = node.getDU().getDef(valNum);
			if(defInst instanceof SSAPhiInstruction)
			{
				return ValueDef.fromStatement(new PhiStatement(node, (SSAPhiInstruction)defInst));
			}
			else if(defInst instanceof SSAPiInstruction)
			{
				return ValueDef.fromStatement(new PiStatement(node, (SSAPiInstruction)defInst));
			}
			else if(defInst instanceof SSAGetCaughtExceptionInstruction)
			{
				return ValueDef.fromStatement(new GetCaughtExceptionStatement(node, (SSAGetCaughtExceptionInstruction)defInst));
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
							if(invokeInst.getException() == valNum)
								return ValueDef.fromStatement(new ExceptionalReturnCaller(node, idx));
							else
								return ValueDef.fromStatement(new NormalReturnCaller(node, idx));
						}
						else
							return ValueDef.fromStatement(new NormalStatement(node, idx));
					}
				}
				
				// Normally, it should only happen in synthetic method
				return null;
			}
		}
	}
	public static boolean isDefiningInstruction(SSAInstruction inst, int valNum)
	{
		int nDef = inst.getNumberOfDefs();
		int defIdx;
		for(defIdx = 0; defIdx < nDef; ++defIdx)
		{
			if(inst.getDef(defIdx) == valNum)
				break;
		}
		if(defIdx >= nDef)
			return false;
		else
			return true;
	}
	public static boolean isDefiningStatement(Statement stm, int valNum)
	{
		switch(stm.getKind())
		{
		case PARAM_CALLEE:
			{
				ParamCallee paramCallee = (ParamCallee)stm;
				int srcValNum = paramCallee.getValueNumber();
				return srcValNum == valNum;
			}
		case NORMAL_RET_CALLER:
			{
				NormalReturnCaller retCaller = (NormalReturnCaller)stm;
				return valNum == retCaller.getValueNumber();
			}
		case PHI:
			{
				PhiStatement phiStm = (PhiStatement)stm;
				SSAPhiInstruction phiInst = phiStm.getPhi();
				return isDefiningInstruction(phiInst, valNum);
			}
		case NORMAL:
			{
				NormalStatement normalStm = (NormalStatement)stm;
				SSAInstruction srcInst = normalStm.getInstruction();
				return isDefiningInstruction(srcInst, valNum);
			}
		default:
			return false;
		}
	}
}
