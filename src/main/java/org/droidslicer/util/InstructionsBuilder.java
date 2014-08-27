package org.droidslicer.util;

import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

public class InstructionsBuilder extends AbstractsInstructionsBuilder
{
	private static final Logger mLogger = LoggerFactory.getLogger(InstructionsBuilder.class);
	private final IClassHierarchy mCha;
	private final SSAInstructionFactory mInstFactory;
	private List<InstructionException> mSupressedExceptions = new ArrayList<InstructionException>();
	public InstructionsBuilder(IClassHierarchy cha, SSAInstructionFactory instFactory, Map<Integer, ConstantValue> constants, int nextPC, int nextLocal)
	{
		super(constants, nextPC, nextLocal);
		if(cha == null || instFactory == null)
			throw new IllegalArgumentException();
		mCha = cha;
		mInstFactory = instFactory;
	}
	public int addInstsArrayAllocation(TypeReference tr, Map<TypeReference, Integer> typeToId, int maxDepth, boolean supressExceptions)
		throws InstructionException
    {
    	if(!tr.isArrayType())
    		throw new IllegalArgumentException("The type isn't array type");
    	final IClass klass = mCha.lookupClass(tr);
        if (klass == null)
        {
        	InstructionException ex = new InstructionException("Type reference " + tr.getName() + " doesn't exist in the class hierarchy");
        	if(supressExceptions)
        	{
        		mSupressedExceptions.add(ex);
        		return addLocal();
        	}
        	else
        		throw ex;
        }
        
        int innerTypeValNum = -1;
        final int arrValNum = addLocal();

       	ArrayClass arrClazz = (ArrayClass)klass;
    	int dim = arrClazz.getDimensionality();

    	// Prepare the size of each dimension of the array
    	int[] sizes = new int[dim];
    	
    	// Set each dimension to be of size 1
    	// That is, we will create an array like int[1][1][1]
    	Arrays.fill(sizes, findOrCreateConstant(1));
    	
    	// Create an instruction for instantiating an array with type "ref".
    	// The size of each dimension is specified by "sizes".
    	// Notice that multiple dimension array in Java is implemented by array of arrays.
    	// Thus, jagged array is allowed. 
    	// To instantiate an array with i dimensions, we can only specify the size of 
    	// first j dimensions where j <= i. This will correspond to the Java code like below:
    	// int[][][][] a = new int[2][2][][];
    	// In this example, a[0][0] is of type int[][], but its value is null.
        final NewSiteReference newSiteRef = NewSiteReference.make(getNextPC(), tr);
    	SSANewInstruction newInst = mInstFactory.NewInstruction(arrValNum, newSiteRef, sizes);
    	
    	// Add the statement to the method
        addInstruction(newInst);

        // If the inner-most element type of the array is a class type, then we 
        // should add instruction for allocation of the class type and store it into the 
        // array.
        if(tr.getInnermostElementType().isClassType())
        {
            int nowArrRef = arrValNum;
            TypeReference nowArrType = tr;
            
            // Get the array reference of the one before the last
            // E.g. for String[1][1][1] --> String[0][0]
            for(int i = 0; i < dim - 1; ++i)
        	{
            	int subArrRef = addLocal();
            	
            	// <subArrRef> := nowArrRef[0]
            	SSAInstruction inst = mInstFactory.ArrayLoadInstruction(subArrRef, nowArrRef, 0, nowArrType.getArrayElementType());
        		addInstruction(inst);
        		nowArrRef = subArrRef;
        		nowArrType = nowArrType.getArrayElementType();
        	}

            assert nowArrType.isArrayType() && nowArrType.getArrayElementType().isClassType();
            TypeReference eleType = nowArrType.getArrayElementType();
            innerTypeValNum = addLocal();
            NewSiteReference eleNewSiteRef = NewSiteReference.make(getNextPC(), eleType);
            
            // Create a new instance of the inner-most element of the array
            SSAInstruction newEleInst = mInstFactory.NewInstruction(innerTypeValNum, eleNewSiteRef);
            addInstruction(newEleInst);
            
            // Store the element into the array
            SSAInstruction aastoreInst = mInstFactory.ArrayStoreInstruction(nowArrRef, 0, innerTypeValNum, eleType);
            addInstruction(aastoreInst);
            addInstsInit(eleType, innerTypeValNum, typeToId, maxDepth, supressExceptions);
        }
        return arrValNum;
    }
    public void addInstsInit(TypeReference tr, int valueNum, Map<TypeReference, Integer> typeToId, int maxDepth, boolean supressExceptions)
    	throws InstructionException
    {
    	try
    	{
	    	if(!tr.isClassType())
	    		throw new InstructionException("The type isn't class type");
	    	IClass clazz = mCha.lookupClass(tr);
	    	if(clazz == null)
	    		throw new InstructionException("Fail to find the type in class hierarchy");
	        IMethod bestCtor = null;
	        if(!clazz.isInterface())
	        {
		        int minCtorNArg = Integer.MAX_VALUE;
		        for(IMethod method : clazz.getDeclaredMethods())
		        {
		        	int numParams = method.getNumberOfParameters();
		        	if(!method.isStatic() && 
		        			method.isInit() && 
		        			minCtorNArg > numParams && 
		        			numParams > 0 && 
		        			method.getParameterType(0).equals(clazz.getReference()))
		        	{
		        		bestCtor = method;
		        		minCtorNArg = method.getNumberOfParameters();
		        	}
		        }
		        if(bestCtor == null)
		        	throw new InstructionException("No constructor found for type " + tr.getName());
	        }
	        else
	        	throw new InstructionException("Interface " + clazz + " cannot be instantiated");
	
	        assert bestCtor.getNumberOfParameters() > 0 && bestCtor.getParameterType(0).equals(clazz.getReference());
	        int ctorNumParams = bestCtor.getNumberOfParameters();
	        int[] paramsValNums = new int[ctorNumParams];
	        paramsValNums[0] = valueNum;
	        TypeReference dependClType = null;
	        if(typeToId != null)
	        {
	        	// Find the dependent type (It may be a enclosing class of a static inner class or a enclosing class of a nested class)
		        try
		        {
		        	String clName = tr.getName().getClassName().toUnicodeString();
			        for(int i = clName.length() - 1; i >= 0; --i)
			        {
			        	if(clName.charAt(i) == '$')
			        	{
			        		dependClType = TypeReference.findOrCreate(ClassLoaderReference.Primordial, 
			        				"L" + tr.getName().getPackage() + "/" + clName.substring(0, i));
			        		break;
			        	}
			        }
		        }
		        catch(UTFDataFormatException ex)
		        {
		        	throw new InstructionException("Exception occurred when decoding class name from UTF8 encoding");
		        }
	        }
	        int startParamIdx;
	        
	        // Check if the dependent type is possibly an enclosing class of a inner class
	        // We cannot be very sure, since it is impossible to distinguish static nested class and inner class precisely 
	        // since this information is lost when the Java source code is compiled into byte-code.
	        if(dependClType != null && ctorNumParams >= 1 + 1 && bestCtor.getParameterType(1).equals(dependClType))
	        {
	        	Integer paramValNum = typeToId.get(dependClType);
	    		if(paramValNum == null)
    				paramsValNums[1] = addAllocation(dependClType, typeToId, maxDepth - 1, supressExceptions);
	    		else
	    			paramsValNums[1] = paramValNum;
	    		startParamIdx = 2;
	        }
	        else
	        	startParamIdx = 1;
	        for(int i = startParamIdx; i < ctorNumParams; ++i)
	        {
	        	TypeReference paramType = bestCtor.getParameterType(i);
        		paramsValNums[i] = addAllocation(paramType, typeToId, maxDepth - 1, supressExceptions);
	        }
	        addInstsInvocation(bestCtor.getReference(), paramsValNums, IInvokeInstruction.Dispatch.SPECIAL);
    	}
    	catch(InstructionException ex)
    	{
    		if(supressExceptions)
    			mSupressedExceptions.add(ex);
    		else
    			throw ex;
    	}
    }
    public int addInstsInvocation(MethodReference method, int[] params, IInvokeInstruction.Dispatch dispatchCode)
    {
    	CallSiteReference newSite = CallSiteReference.make(getNextPC(), method, dispatchCode);
    	SSAInvokeInstruction s = null;
    	int exValNum = addLocal();
    	int retValNum = -1;
    	if (newSite.getDeclaredTarget().getReturnType().equals(TypeReference.Void))
    	{
    		s = mInstFactory.InvokeInstruction(params, exValNum, newSite);
    	}
    	else
    	{
    		retValNum = addLocal();
    		s = mInstFactory.InvokeInstruction(retValNum, params, exValNum, newSite);
    	}
   		addInstruction(s);
   		return retValNum;
    }
	public int addAllocation(TypeReference tr, Map<TypeReference, Integer> typeToId, int maxDepth, boolean supressExceptions)
		throws InstructionException
	{
    	if(tr.isPrimitiveType() || maxDepth <= 0)
    		return addLocal();
    	else if(tr.isArrayType())
    		return addInstsArrayAllocation(tr, typeToId, maxDepth, supressExceptions);
    	else
    	{
			IClass clazz = mCha.lookupClass(tr);
			if(clazz == null)
			{
				InstructionException ex = new InstructionException("No class definition found for the type " + tr.getName()); 
				if(supressExceptions)
				{
					mSupressedExceptions.add(ex);
					return addLocal();
				}
				else
				{
					throw ex;
				}				
			}
			
    		if(typeToId != null && typeToId.containsKey(tr))
    			return typeToId.get(tr);
    		assert tr.isClassType();
    		NewSiteReference newSiteRef = NewSiteReference.make(getNextPC(), tr);

    		// Create an instruction for instantiating an object of type denoted by "ref", and 
        	// store the reference in register i
        	final int valueNum = addLocal();
        	SSANewInstruction newInst = mInstFactory.NewInstruction(valueNum, newSiteRef);
            addInstruction(newInst);
           	addInstsInit(tr, valueNum, typeToId, maxDepth, supressExceptions);
            if(typeToId != null)
            	typeToId.put(tr, valueNum);
            return valueNum;
    	}
	}
	public List<InstructionException> getSuppressedExceptions() 
	{
		return mSupressedExceptions;
	}
	public static IInvokeInstruction.Dispatch getInvokeDispatch(IMethod method)
	{
		if(method.isStatic())
			return IInvokeInstruction.Dispatch.STATIC;
		else if (method.isInit())
			return IInvokeInstruction.Dispatch.SPECIAL;
		else if (method.getDeclaringClass().isInterface())
			return IInvokeInstruction.Dispatch.INTERFACE;
		else
			return IInvokeInstruction.Dispatch.VIRTUAL;
	}
}
