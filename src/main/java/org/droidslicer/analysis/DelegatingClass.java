package org.droidslicer.analysis;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.ibm.wala.classLoader.BytecodeClass;
import com.ibm.wala.classLoader.FieldImpl;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.strings.Atom;

/**
 * Notice that this class isn't implemented very efficiently. We assume that the added fields are few. 
 * @author someone
 *
 */
public class DelegatingClass extends BytecodeClass<IClassLoader>
{
	private final IClass mDelegate;
	private HashSet<IField> mFields = new HashSet<IField>(); 
	private final IClassLoader mClassLoader;
	public DelegatingClass(IClassLoader classLoader, IClass delegate)
	{
		super(classLoader, delegate.getClassHierarchy());
		if(classLoader == null || delegate == null)
			throw new NullPointerException();
		mDelegate = delegate;
		mClassLoader = classLoader;
	}
	public void addField(Atom fieldName, TypeReference fieldType, int accessFlags, Collection<Annotation> annotations)
	{
		FieldImpl field = new FieldImpl(this, FieldReference.findOrCreate(getReference(), fieldName, fieldType), accessFlags, annotations);
		mFields.add(field);
	}

	@Override
	public boolean isInterface()
	{
		return mDelegate.isInterface();
	}

	@Override
	public boolean isAbstract()
	{
		return mDelegate.isAbstract();
	}

	@Override
	public boolean isPublic()
	{
		return mDelegate.isPublic();
	}

	@Override
	public boolean isPrivate()
	{
		return mDelegate.isPrivate();
	}

	@Override
	public int getModifiers() throws UnsupportedOperationException
	{
		return mDelegate.getModifiers();
	}

	@Override
	public IClass getSuperclass()
	{
		IClass original = mDelegate.getSuperclass();
		if(original == null)
			return original;
		TypeName superClassName = original.getName();
		return mClassLoader.lookupClass(superClassName);
	}

	@Override
	public Collection<? extends IClass> getDirectInterfaces()
	{
		return Collections2.transform(mDelegate.getDirectInterfaces(), new Function<IClass, IClass>()
		{
			@Override
			public IClass apply(IClass clazz)
			{
				return mClassLoader.lookupClass(clazz.getName());
			}
		});
	}

	@Override
	public Collection<IClass> getAllImplementedInterfaces()
	{
		return Collections2.transform(mDelegate.getAllImplementedInterfaces(), new Function<IClass, IClass>()
		{
			@Override
			public IClass apply(IClass clazz)
			{
				return mClassLoader.lookupClass(clazz.getName());
			}
		});
	}

	public boolean isAddedField(IField field)
	{
		return mFields.contains(field);
	}
	@Override
	public IField getField(Atom name)
	{
		for(IField field : mFields)
		{
			if(field.getName().equals(name))
				return field;
		}
		return mDelegate.getField(name);
	}

	@Override
	public IField getField(Atom name, TypeName type)
	{
		for(IField field : mFields)
		{
			if(field.getName().equals(name) && field.getFieldTypeReference().getName().equals(type))
				return field;
		}
		return mDelegate.getField(name, type);
	}

	@Override
	public TypeReference getReference()
	{
		return mDelegate.getReference();
	}

	@Override
	public String getSourceFileName() throws NoSuchElementException
	{
		return mDelegate.getSourceFileName();
	}

	@Override
	public InputStream getSource() throws NoSuchElementException
	{
		return mDelegate.getSource();
	}

	@Override
	public IMethod getClassInitializer()
	{
		return mDelegate.getClassInitializer();
	}

	@Override
	public boolean isArrayClass()
	{
		return mDelegate.isArrayClass();
	}

	@Override
	public Collection<IMethod> getDeclaredMethods()
	{
		return mDelegate.getDeclaredMethods();
	}

	@Override
	public Collection<IField> getAllInstanceFields()
	{
		Collection<IField> original = mDelegate.getAllInstanceFields();
		ArrayList<IField> result = new ArrayList<IField>();
		for(IField field : mFields)
		{
			if(!field.isStatic())
				result.add(field);
		}
		result.addAll(original);
		return result;
	}

	@Override
	public Collection<IField> getAllStaticFields()
	{
		Collection<IField> original = mDelegate.getAllStaticFields();
		ArrayList<IField> result = new ArrayList<IField>();
		for(IField field : mFields)
		{
			if(field.isStatic())
				result.add(field);
		}
		result.addAll(original);
		return result;
	}

	@Override
	public Collection<IField> getAllFields()
	{
		Collection<IField> original = mDelegate.getAllFields();
		ArrayList<IField> result = new ArrayList<IField>();
		for(IField field : mFields)
		{
			result.add(field);
		}
		result.addAll(original);
		return result;
	}

	@Override
	public Collection<IField> getDeclaredInstanceFields()
	{
		Collection<IField> original = mDelegate.getDeclaredInstanceFields();
		ArrayList<IField> result = new ArrayList<IField>();
		for(IField field : mFields)
		{
			if(!field.isStatic())
				result.add(field);
		}
		result.addAll(original);
		return result;
	}

	@Override
	public Collection<IField> getDeclaredStaticFields()
	{
		Collection<IField> original = mDelegate.getDeclaredStaticFields();
		ArrayList<IField> result = new ArrayList<IField>();
		for(IField field : mFields)
		{
			if(field.isStatic())
				result.add(field);
		}
		result.addAll(original);
		return result;
	}

	@Override
	public TypeName getName()
	{
		return mDelegate.getName();
	}

	@Override
	public boolean isReferenceType()
	{
		return mDelegate.isReferenceType();
	}

	@Override
	public Collection<Annotation> getAnnotations()
	{
		return mDelegate.getAnnotations();
	}
	@Override
	public String toString()
	{
		return mDelegate.toString();
	}
	@Override
	public int hashCode()
	{
		return mDelegate.hashCode() * 4919;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof DelegatingClass))
			return false;
		DelegatingClass that = (DelegatingClass)other;
		
		// We ignore the extra fields when comparing two class
		return mDelegate.equals(that.mDelegate);
	}

	@Override
	protected IMethod[] computeDeclaredMethods()
			throws InvalidClassFileException
	{
		Collection<IMethod> methods = mDelegate.getDeclaredMethods();
		IMethod[] result = new IMethod[methods.size()];
		int idx = 0;
		for(IMethod method : methods)
		{
			Selector selector = method.getSelector();
			IMethod original = mDelegate.getMethod(selector);
			if(original instanceof SyntheticMethod)
			{
				result[idx] = new DelegatingSyntheticMethod(this, (SyntheticMethod)original);
			}
			else if(original instanceof IBytecodeMethod)
			{
				result[idx] = new DelegatingBytecodeMethod(this, (IBytecodeMethod)original);
			}
			else
			{
				throw new IllegalArgumentException("Unsupported type of method");
			}
			++idx;
		}
		return result;
	}
}
