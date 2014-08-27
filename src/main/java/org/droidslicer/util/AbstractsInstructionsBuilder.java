package org.droidslicer.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.Selector;

public abstract class AbstractsInstructionsBuilder
{
	private int mNextPC = -1;
	private int mNextLocal = -1;
	private final List<SSAInstruction> mInsts = new ArrayList<SSAInstruction>();
	private final Map<Integer, ConstantValue> mConstants;
	public AbstractsInstructionsBuilder(Map<Integer, ConstantValue> constants, int nextPC, int nextLocal)
	{
		if(nextLocal < 1 || nextPC < 0)
			throw new IllegalArgumentException();
		mNextPC = nextPC;
		mNextLocal = nextLocal;
		if(constants == null)
			mConstants = new HashMap<Integer, ConstantValue>();
		else
			mConstants = constants;
	}
	public static IMethod getDeclaredMethod(IClass type, Selector selector)
	{
		for(IMethod method : type.getDeclaredMethods())
		{
			if(method.getSelector().equals(selector))
				return method;
		}
		return null;
	}
	public int getNextLocal()
	{
		return mNextLocal;
	}
	public int addLocal()
	{
		return mNextLocal++;
	}
	public int getNextPC()
	{
		return mNextPC;
	}
	public void addInstruction(SSAInstruction inst)
	{
		mInsts.add(inst);
		++mNextPC;
	}
	public List<SSAInstruction> getInstructions()
	{
		return mInsts;
	}
	public Map<Integer, ConstantValue> getConstants()
	{
		return mConstants;
	}
    /**
     * Get the value number of an integer constant {@code c}. If no such value number exists 
     * for the constant, then create a new one.
     * @param c
     * @return the value number for the constant value
     */
    public int findOrCreateConstant(int c)
    {
        ConstantValue v = new ConstantValue(c);
        for(Map.Entry<Integer, ConstantValue> entry : mConstants.entrySet())
        {
        	if(entry.getValue().equals(v))
        		return entry.getKey();
        }
        int result = addLocal();
        assert !mConstants.containsKey(result);
        mConstants.put(result, v);
        return result;
    }
}
