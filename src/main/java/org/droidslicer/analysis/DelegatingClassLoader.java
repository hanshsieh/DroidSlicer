package org.droidslicer.analysis;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.strings.Atom;

public class DelegatingClassLoader implements IClassLoader
{
	private final IClassLoader mDelegate;
	private final Map<TypeName, Set<FieldSpec>> mExtraFields = new HashMap<TypeName, Set<FieldSpec>>();
	
	// Notice that we shouldn't use a cache to store the IClass. It seems that in somewhere in the WALA, 
	// it compares that the IClass instance with '==' operator. Thus, once a IClass is created, we should
	// always return the same instance every time afterward.
	private final Map<TypeName, IClass> mClassMap = new HashMap<TypeName, IClass>();
	public DelegatingClassLoader(IClassLoader delegate)
	{
		mDelegate = delegate;
	}
	public void addExtraField(FieldSpec field)
	{
		FieldReference ref = field.getFieldReference();
		TypeName className = ref.getDeclaringClass().getName();
		Set<FieldSpec> original = mExtraFields.get(className);
		if(original == null)
		{
			original = new HashSet<FieldSpec>();
			mExtraFields.put(className, original);
		}
		original.add(field);
	}
	
	@Override
	public IClass lookupClass(TypeName className)
	{
		return lookupClassInternal(className, null);
	}
	protected IClass lookupClassInternal(TypeName className, IClass original)
	{
		IClass result = mClassMap.get(className);
		if(result != null)
			return result;
		if(original == null)
			original = mDelegate.lookupClass(className);
		if(original == null)
			return null;
		if(original instanceof ArrayClass)
		{
			mClassMap.put(className, original);
			return original;
		}
		Set<FieldSpec> extraFields = mExtraFields.get(className);
		
		// Event if no extra fields is to be added, we still need to wrap the original class
		// so that other classes' superclass will point to the wrapped version instead of the original 
		// version.
		DelegatingClass delegate = new DelegatingClass(DelegatingClassLoader.this, original);
		if(extraFields != null)
		{
			for(FieldSpec field : extraFields)
			{
				FieldReference fieldRef = field.getFieldReference();
				delegate.addField(fieldRef.getName(), fieldRef.getFieldType(), field.getAccessFlags(), null);
			}
		}
		mClassMap.put(className, delegate);
		return delegate;
	}

	@Override
	public ClassLoaderReference getReference()
	{
		return mDelegate.getReference();
	}

	@Override
	public Iterator<IClass> iterateAllClasses()
	{
		return Iterators.transform(mDelegate.iterateAllClasses(), new Function<IClass, IClass>()
		{
			@Override
			public IClass apply(IClass original)
			{
				return lookupClassInternal(original.getName(), original);
			}			
		});
	}

	@Override
	public int getNumberOfClasses()
	{
		return mDelegate.getNumberOfClasses();
	}

	@Override
	public Atom getName()
	{
		return mDelegate.getName();
	}

	@Override
	public Language getLanguage()
	{
		return mDelegate.getLanguage();
	}

	@Override
	public SSAInstructionFactory getInstructionFactory()
	{
		return mDelegate.getInstructionFactory();
	}

	@Override
	public int getNumberOfMethods()
	{
		return mDelegate.getNumberOfMethods();
	}

	@Override
	public String getSourceFileName(IMethod method, int offset)
	{
		return mDelegate.getSourceFileName(method, offset);
	}

	@Override
	public InputStream getSource(IMethod method, int offset)
	{
		return mDelegate.getSource(method, offset);
	}

	@Override
	public String getSourceFileName(IClass klass) throws NoSuchElementException
	{
		return mDelegate.getSourceFileName(klass);
	}

	@Override
	public InputStream getSource(IClass klass) throws NoSuchElementException
	{
		return mDelegate.getSource(klass);
	}

	@Override
	public IClassLoader getParent()
	{
		return mDelegate.getParent();
	}

	@Override
	public void init(List<Module> modules) throws IOException
	{
		mDelegate.init(modules);
	}

	@Override
	public void removeAll(Collection<IClass> toRemove)
	{
		mDelegate.removeAll(toRemove);
		for(IClass clazz : toRemove)
			mClassMap.remove(clazz.getName());
	}

}
