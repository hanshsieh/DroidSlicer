package org.droidslicer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.SyntheticMethod;

public class DelegatingSyntheticMethod extends SyntheticMethod
{
	public DelegatingSyntheticMethod(IClass declaringClass, SyntheticMethod delegate)
	{
		super(delegate.getReference(), 
				declaringClass, 
				delegate.isStatic(), 
				delegate.isFactoryMethod());	
	}
}
