package org.droidslicer.android.model;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.strings.Atom;

public class FakeField implements IField
{
	private final IClass declaringClass;
	private final FieldReference fieldRef;
	private final int accessFlags;

	public FakeField(IClass declaringClass, FieldReference canonicalRef, int accessFlags)
	{
		this.declaringClass = declaringClass;
		this.fieldRef = canonicalRef;
		this.accessFlags = accessFlags;
		if (declaringClass == null)
			throw new IllegalArgumentException("Null declaringClass");
		if (fieldRef == null)
			throw new IllegalArgumentException("Null canonicalRef");
		if(!declaringClass.getReference().equals(canonicalRef.getDeclaringClass()))
			throw new IllegalArgumentException("Declaring class mismatch");
	}

	/*
	* @see com.ibm.wala.classLoader.IMember#getDeclaringClass()
	*/
	@Override
	public IClass getDeclaringClass() {
		return declaringClass;
	}

	/*
	* @see java.lang.Object#equals(java.lang.Object)
	*/
	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		else if(!(obj instanceof FakeField))
			return false;
		else
		{
			FakeField other = (FakeField) obj;
			return fieldRef.equals(other.fieldRef) && declaringClass.equals(other.declaringClass) && accessFlags == other.accessFlags;
		}
	}

	@Override
	public int hashCode()
	{
		return declaringClass.hashCode() * (31 * 31) + fieldRef.hashCode() * 31 + accessFlags;
	}

	@Override
	public String toString()
	{
		return getReference().toString();
	}

	@Override
	public FieldReference getReference()
	{
		return fieldRef;
	}

	/*
	* @see com.ibm.wala.classLoader.IMember#getName()
	*/
	@Override
	public Atom getName()
	{
		return fieldRef.getName();
	}

	/*
	* @see com.ibm.wala.classLoader.IField#getFieldTypeReference()
	*/
	@Override
	public TypeReference getFieldTypeReference()
	{
		return fieldRef.getFieldType();
	}

	@Override
	public boolean isStatic()
	{
		return ((accessFlags & ClassConstants.ACC_STATIC) != 0);
	}

	@Override
	public boolean isFinal()
	{
		return ((accessFlags & ClassConstants.ACC_FINAL) != 0);
	}

	@Override
	public boolean isPrivate()
	{
		return ((accessFlags & ClassConstants.ACC_PRIVATE) != 0);
	}

	@Override
	public boolean isProtected()
	{
		return ((accessFlags & ClassConstants.ACC_PROTECTED) != 0);
	}

	@Override
	public boolean isPublic()
	{
		return ((accessFlags & ClassConstants.ACC_PUBLIC) != 0);
	}
	
	@Override
	public boolean isVolatile()
	{
		return ((accessFlags & ClassConstants.ACC_VOLATILE) != 0);
	}

	@Override
	public IClassHierarchy getClassHierarchy()
	{
		return declaringClass.getClassHierarchy();
	}

	@Override
	public Collection<Annotation> getAnnotations()
	{
		return Collections.emptySet();
	}
}
