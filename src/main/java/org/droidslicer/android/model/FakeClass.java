package org.droidslicer.android.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;

public abstract class FakeClass extends SyntheticClass
{
	private int mModifiers = ClassConstants.ACC_PUBLIC;
	private Set<IMethod> mMethods = new HashSet<IMethod>();
	private Map<Atom, IField> mFields = new HashMap<Atom, IField>();
	private Set<IClass> mInterfaces = new HashSet<IClass>();
	private IClass mSuperClazz = null;
	private IMethod mClassInitializer = null;
	public FakeClass(TypeReference type, IClassHierarchy cha)
	{
		super(type, cha);
	}

	@Override
	final public boolean isPublic()
	{
		return (mModifiers & ClassConstants.ACC_PUBLIC) != 0;
	}

	@Override
	final public boolean isPrivate()
	{
		return (mModifiers & ClassConstants.ACC_PRIVATE) != 0;
	}

	@Override
	final public int getModifiers()
	{
		return mModifiers;
	}

	@Override
	final public IClass getSuperclass()
	{
		return mSuperClazz;
	}

	@Override
	final public Collection<? extends IClass> getDirectInterfaces()
	{
		return mInterfaces;
	}

	@Override
	final public Collection<IClass> getAllImplementedInterfaces()
	{
		Collection<IClass> result = new ArrayList<IClass>();
		result.addAll(getDirectInterfaces());
		if(mSuperClazz != null)
			result.addAll(mSuperClazz.getAllImplementedInterfaces());
		return result;
	}

	@Override
	final public IMethod getMethod(Selector selector)
	{
		for (IMethod m : mMethods)
		{
			if (m.getSelector().equals(selector))
				return m;
		}
		if(mSuperClazz != null)
			return mSuperClazz.getMethod(selector);
		else
			return null;
	}

	@Override
	final public IField getField(Atom name)
	{
		IField field = mFields.get(name);
		if(field != null)
			return field;
		else if(mSuperClazz != null)
			return mSuperClazz.getField(name);
		else
			return null;
	}

	@Override
	final public IMethod getClassInitializer()
	{
		return mClassInitializer;
	}

	@Override
	final public Collection<IMethod> getDeclaredMethods()
	{
		return Collections.unmodifiableCollection(mMethods);
	}

	@Override
	final public Collection<IField> getAllInstanceFields()
	{
		Collection<IField> result = new ArrayList<IField>();
		if(mSuperClazz != null)
			result.addAll(mSuperClazz.getAllInstanceFields());
		for(IField field : mFields.values())
		{
			if(!field.isStatic())
				result.add(field);
		}
		return result;
	}

	@Override
	final public Collection<IField> getAllStaticFields()
	{
		Collection<IField> result = new ArrayList<IField>();
		if(mSuperClazz != null)
			result.addAll(mSuperClazz.getAllInstanceFields());
		for(IField field : mFields.values())
		{
			if(field.isStatic())
				result.add(field);
		}
		return result;
	}

	@Override
	final public Collection<IField> getAllFields()
	{
		ArrayList<IField> allFields = new ArrayList<IField>();
		if(mSuperClazz != null)
			allFields.addAll(mSuperClazz.getAllFields());
		for(IField field : mFields.values()) 
			allFields.add(field);
		return allFields;
	}

	@Override
	final public Collection<IMethod> getAllMethods()
	{
		ArrayList<IMethod> allMethods = new ArrayList<IMethod>();
		if(mSuperClazz != null)
			allMethods.addAll(mSuperClazz.getAllMethods());
		allMethods.addAll(mMethods);
		return allMethods;
	}

	@Override
	final public Collection<IField> getDeclaredInstanceFields()
	{
		Collection<IField> result = new ArrayList<IField>();
		for(IField field : mFields.values())
		{
			if(!field.isStatic())
				result.add(field);
		}
		return result;
	}

	@Override
	final public Collection<IField> getDeclaredStaticFields()
	{
		Collection<IField> result = new ArrayList<IField>();
		for(IField field : mFields.values())
		{
			if(field.isStatic())
				result.add(field);
		}
		return result;
	}

	@Override
	final public boolean isReferenceType()
	{
		return true;
	}

	public void setModifiers(int modifier)
	{
		mModifiers = modifier;
	}
	
	public void setClassInitializer(IMethod method)
	{
		if(method.getDeclaringClass() != this)
			throw new IllegalArgumentException("The declaring class must be this class");
		mClassInitializer = method;
	}
	
	public void addMethod(IMethod method)
	{
		if(method.getDeclaringClass() != this)
			throw new IllegalArgumentException("This declaring class must be this class");
		mMethods.add(method);
	}
	
	public void addField(IField field)
	{
		if(field.getDeclaringClass() != this)
			throw new IllegalArgumentException("The declaring class must be this class");
		mFields.put(field.getName(), field);
	}
	
	public void addImplementedInterfaces(IClass clazz)
	{
		if(clazz == null || !clazz.isInterface())
			throw new IllegalArgumentException("Not interface");
		mInterfaces.add(clazz);
	}
	
	public void setSuperclass(IClass clazz)
	{
		if(clazz != null && (clazz.isArrayClass() || clazz.isInterface()))
			throw new IllegalArgumentException("Invalid super class");
		mSuperClazz = clazz;
	}
	
	public void removeField(Atom name)
	{
		mFields.remove(name);
	}
	
	public void removeMethod(IMethod method)
	{
		mMethods.remove(method);
	}
	
	@Override
	public int hashCode()
	{
		return getReference().hashCode();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		IClass that = (IClass)other;
		return getReference().equals(that.getReference());
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		if(isPublic())
			builder.append("public ");
		else if(isPrivate())
			builder.append("private ");
		builder.append("class ");
		builder.append(StringStuff.jvmToReadableType(getReference().getName().toString()));
		if(mSuperClazz != null)
		{
			builder.append(" extends ");
			builder.append(StringStuff.jvmToReadableType(mSuperClazz.getName().toString()));
		}
		if(!mInterfaces.isEmpty())
		{
			builder.append(" implements ");
			boolean first = true;
			for(IClass clazz : mInterfaces)
			{
				if(first)
					first = false;
				else
					builder.append(", ");
				builder.append(StringStuff.jvmToReadableType(clazz.getName().toString()));
			}
		}
		builder.append("\n{\n");
		for(IField field : mFields.values())
		{
			builder.append('\t');
			if(field.isPublic())
				builder.append("public ");
			else if(field.isPrivate())
				builder.append("private ");
			else if(field.isProtected())
				builder.append("protected ");
			if(field.isFinal())
				builder.append("final ");
			if(field.isStatic())
				builder.append("static ");
			if(field.isVolatile())
				builder.append("volatile ");
			builder.append(StringStuff.jvmToReadableType(field.getFieldTypeReference().getName().toString()));
			builder.append(' ');
			builder.append(field.getName());
			builder.append(";\n");
		}
		if(mClassInitializer != null)
		{
			buildMethodString(mClassInitializer, builder, "\t");
		}
		for(IMethod method : mMethods)
			buildMethodString(method, builder, "\t");
		builder.append("}\n");
		return builder.toString();
	}
	private static void buildMethodString(IMethod method, StringBuilder builder, String indent)
	{
		builder.append(indent);
		if(method.isPublic())
			builder.append("public ");
		else if(method.isPrivate())
			builder.append("private ");
		else if(method.isProtected())
			builder.append("protected ");
		if(method.isFinal())
			builder.append("final ");
		if(method.isStatic())
			builder.append("static ");
		if(method.isAbstract())
			builder.append("abstract ");
		if(method.isNative())
			builder.append("native ");
		if(method.isSynchronized())
			builder.append("synchronized ");
		builder.append(StringStuff.jvmToReadableType(method.getReturnType().getName().toString()));
		builder.append(' ');
		builder.append(method.getName().toString());
		builder.append('(');
		int numParam = method.getNumberOfParameters();
		{
			boolean first = true;
			int i = method.isStatic() ? 0 : 1;
			for(; i < numParam; ++i)
			{
				if(first)
					first = false;
				else
					builder.append(", ");
				builder.append(StringStuff.jvmToReadableType(method.getParameterType(i).getName().toString()));
			}
		}
		builder.append(')');
		if(method instanceof SummarizedMethod)
		{
			builder.append('\n');
			builder.append(indent);
			builder.append("{\n");
			SummarizedMethod m = (SummarizedMethod)method;
			SSAInstruction[] insts = m.getStatements();
			for(int i = 0; i < insts.length; ++i)
			{
				builder.append(indent);
				builder.append(indent);
				builder.append(String.format("%04d ", i));
				if(insts[i] instanceof SSAGotoInstruction)
				{
					SSAGotoInstruction gotoInst = (SSAGotoInstruction)insts[i];
					builder.append("goto ");
					builder.append(gotoInst.getLabel());
				}
				else
					builder.append(insts[i].toString());
				builder.append('\n');
			}
			builder.append(indent);
			builder.append("}\n");
		}
		else
		{
			builder.append(";\n");
		}
	}
}
