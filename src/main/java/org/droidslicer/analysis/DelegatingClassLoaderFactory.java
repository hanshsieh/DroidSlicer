package org.droidslicer.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;

public class DelegatingClassLoaderFactory extends ClassLoaderFactoryImpl
{
	private final Map<ClassLoaderReference, Set<FieldSpec>> mExtraFields = new HashMap<ClassLoaderReference, Set<FieldSpec>>();
	public DelegatingClassLoaderFactory(SetOfClasses exclusions)
	{
		super(exclusions);
	}
	
	public void addExtraField(FieldSpec field)
	{
		FieldReference ref = field.getFieldReference();
		ClassLoaderReference loaderRef = ref.getDeclaringClass().getClassLoader();
		Set<FieldSpec> original = mExtraFields.get(loaderRef);
		if(original == null)
		{
			original = new HashSet<FieldSpec>();
			mExtraFields.put(loaderRef, original);
		}
		original.add(field);
	}
	
	@Override
	protected IClassLoader makeNewClassLoader(
			ClassLoaderReference classLoaderReference, IClassHierarchy cha, IClassLoader parent,
			AnalysisScope scope) throws IOException
	{
		IClassLoader original = super.makeNewClassLoader(classLoaderReference, cha, parent, scope);
		Set<FieldSpec> fields = mExtraFields.get(classLoaderReference);
		if(fields == null || fields.isEmpty())
			return original;
		DelegatingClassLoader result = new DelegatingClassLoader(original);
		for(FieldSpec field : fields)
		{
			if(field.getFieldReference().getDeclaringClass().getClassLoader().equals(classLoaderReference))
				result.addExtraField(field);
		}
		return result;
	}
}
