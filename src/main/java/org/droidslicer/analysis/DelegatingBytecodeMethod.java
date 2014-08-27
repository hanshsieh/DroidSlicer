package org.droidslicer.analysis;

import java.util.Collection;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.IndirectionData;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.strings.Atom;

public class DelegatingBytecodeMethod implements IBytecodeMethod
{
	private final IBytecodeMethod mDelegate;
	private final IClass mDeclaringClass;
	public DelegatingBytecodeMethod(IClass declaringClass, IBytecodeMethod delegate)
	{
		if(delegate == null)
			throw new IllegalArgumentException();
		mDelegate = delegate;
		mDeclaringClass = declaringClass;
	}
	@Override
	public Atom getName()
	{
		return mDelegate.getName();
	}

	@Override
	public boolean isStatic()
	{
		return mDelegate.isStatic();
	}

	@Override
	public Collection<Annotation> getAnnotations()
	{
		return mDelegate.getAnnotations();
	}

	@Override
	public IClassHierarchy getClassHierarchy()
	{
		return mDelegate.getClassHierarchy();
	}

	@Override
	public boolean isSynchronized()
	{
		return mDelegate.isSynchronized();
	}

	@Override
	public boolean isClinit()
	{
		return mDelegate.isClinit();
	}

	@Override
	public boolean isInit()
	{
		return mDelegate.isInit();
	}

	@Override
	public boolean isNative()
	{
		return mDelegate.isNative();
	}

	@Override
	public boolean isSynthetic() 
	{
		return mDelegate.isSynthetic();
	}

	@Override
	public boolean isAbstract()
	{
		return mDelegate.isAbstract();
	}

	@Override
	public boolean isPrivate()
	{
		return mDelegate.isPrivate();
	}

	@Override
	public boolean isProtected() 
	{
		return mDelegate.isProtected();
	}

	@Override
	public boolean isPublic() 
	{
		return mDelegate.isPublic();
	}

	@Override
	public boolean isFinal()
	{
		return mDelegate.isFinal();
	}

	@Override
	public boolean isBridge() 
	{
		return mDelegate.isBridge();
	}

	@Override
	public MethodReference getReference()
	{
		return mDelegate.getReference();
	}

	@Override
	public boolean hasExceptionHandler() 
	{
		return mDelegate.hasExceptionHandler();
	}

	@Override
	public TypeReference getParameterType(int i)
	{
		return mDelegate.getParameterType(i);
	}

	@Override
	public TypeReference getReturnType() 
	{
		return mDelegate.getReturnType();
	}

	@Override
	public int getNumberOfParameters()
	{
		return mDelegate.getNumberOfParameters();
	}

	@Override
	public TypeReference[] getDeclaredExceptions()
			throws InvalidClassFileException, UnsupportedOperationException 
	{
		return mDelegate.getDeclaredExceptions();
	}

	@Override
	public int getLineNumber(int bcIndex)
	{
		return mDelegate.getLineNumber(bcIndex);
	}

	@Override
	public String getLocalVariableName(int bcIndex, int localNumber) 
	{
		return mDelegate.getLocalVariableName(bcIndex, localNumber);
	}

	@Override
	public String getSignature() 
	{
		return mDelegate.getSignature();
	}

	@Override
	public Selector getSelector() 
	{
		return mDelegate.getSelector();
	}

	@Override
	public Descriptor getDescriptor()
	{
		return mDelegate.getDescriptor();
	}

	@Override
	public boolean hasLocalVariableTable() 
	{
		return mDelegate.hasLocalVariableTable();
	}
	@Override
	public IClass getDeclaringClass()
	{
		return mDeclaringClass;
	}
	@Override
	public int getBytecodeIndex(int i) throws InvalidClassFileException
	{
		return mDelegate.getBytecodeIndex(i);
	}
	@Override
	public ExceptionHandler[][] getHandlers() throws InvalidClassFileException
	{
		return mDelegate.getHandlers();
	}
	@Override
	public IInstruction[] getInstructions() throws InvalidClassFileException
	{
		return mDelegate.getInstructions();
	}
	@Override
	public Collection<CallSiteReference> getCallSites()
			throws InvalidClassFileException
	{
		return mDelegate.getCallSites();
	}
	@Override
	public IndirectionData getIndirectionData()
	{
		return mDelegate.getIndirectionData();
	}
	@Override
	public String toString()
	{
		return mDelegate.toString();
	}
	@Override
	public int hashCode()
	{
		return mDelegate.hashCode();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof IMethod))
			return false;
		IMethod that = (IMethod)other;
		return getDeclaringClass().equals(that.getDeclaringClass()) && 
				getReference().equals(that.getReference());
	}

}

