package org.droidslicer.analysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.droidslicer.util.TypeId;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.slicer.HeapExclusions;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;

public class AndroidHeapExclusions extends HeapExclusions
{
	private final SetOfClasses mSet;
	public AndroidHeapExclusions(final IClass appModelClass)
	{
		super(null);
		mSet = new SetOfClasses()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean contains(String klassName)
			{
				throw new RuntimeException("Unimplemented");
			}

			@Override
			public boolean contains(TypeReference clazz)
			{
				ClassLoaderReference classLoaderRef = clazz.getClassLoader();
				if(appModelClass.getReference().equals(clazz))
					return true;
				if(classLoaderRef.equals(ClassLoaderReference.Primordial))
				{
					switch(TypeId.getTypeId(clazz.getName()))
					{
					case ANDROID_ACTIVITY:
					//case ANDROID_CONTEXT:
					//case ANDROID_CONTEXT_WRAPPER:
						return false;
					default:
						return true;
					}
				}
				else
					return false;
			}

			@Override
			public void add(IClass klass)
			{
				throw new RuntimeException("Unimplemented");
			}
		};
	}
	@Override
	public Set<PointerKey> filter(Collection<PointerKey> s)
	{
		if (s == null)
			throw new IllegalArgumentException("s is null");
		HashSet<PointerKey> result = new HashSet<PointerKey>();
		for (PointerKey p : s)
		{
			if(!excludes(p))
				result.add(p);
		}
		return result;
	}
	@Override
	public boolean excludes(PointerKey pk)
	{
		TypeReference t = getType(pk);
		return (t == null) ? false : mSet.contains(t);
  	}
	public static TypeReference getType(PointerKey pk)
	{
		if(pk instanceof InstanceFieldKey)
		{
			InstanceFieldKey f = (InstanceFieldKey)pk;
			IClass clazz = f.getField().getDeclaringClass();
			if(clazz instanceof DelegatingClass && clazz.getClassLoader().getReference().equals(ClassLoaderReference.Primordial))
			{
				DelegatingClass delegateClass = (DelegatingClass)clazz;
				if(delegateClass.isAddedField(f.getField()))
					return null;
			}
			return clazz.getReference();
		}
		else if (pk instanceof AbstractFieldPointerKey)
		{
			AbstractFieldPointerKey f = (AbstractFieldPointerKey) pk;
			if (f.getInstanceKey().getConcreteType() != null)
			{
				return f.getInstanceKey().getConcreteType().getReference();
			}
		}
		else if (pk instanceof StaticFieldKey)
		{
			StaticFieldKey sf = (StaticFieldKey) pk;
			IClass clazz = sf.getField().getDeclaringClass();
			
			// Don't ignore heap dependencies of static fields
			if(clazz.getClassLoader().getReference().equals(ClassLoaderReference.Primordial))
				return null;
			return clazz.getReference();
		}
		return null;
	}
}
