package org.droidslicer.android.appSpec;

import java.util.Collection;
import java.util.Iterator;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;

public class AndroidListenerSpec extends EntryCompSpec
{
	public AndroidListenerSpec(TypeReference listenerClass)
	{
		super(listenerClass);
	}
	public static Iterator<IMethod> getDefaultListenerMethods(TypeReference listenerTypeRef, IClassHierarchy cha) 
	{
		IClass listenerType = cha.lookupClass(listenerTypeRef);
		if(listenerType == null)
			throw new IllegalArgumentException("Fail to find listener type " + listenerTypeRef + " in class hierarchy");
		Collection<IMethod> methods = listenerType.getDeclaredMethods();
		return Iterators.filter(methods.iterator(), new Predicate<IMethod>()
		{
			@Override
			public boolean apply(IMethod method)
			{
				return !method.isStatic();
			}
		});
	}
}
