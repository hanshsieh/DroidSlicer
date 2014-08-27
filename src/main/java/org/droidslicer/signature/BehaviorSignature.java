package org.droidslicer.signature;

import com.google.common.base.Predicate;

public class BehaviorSignature
{
	private final String mName;
	private final Predicate<SemanticSignatureContext> mSig;
	public BehaviorSignature(String name, Predicate<SemanticSignatureContext> sig)
	{
		if(name == null || sig == null)
			throw new IllegalArgumentException();
		mName = name;
		mSig = sig;
	}
	public Predicate<SemanticSignatureContext> getSignature()
	{
		return mSig;
	}
	public String getName()
	{
		return mName;
	}
	@Override
	public int hashCode()
	{
		return super.hashCode();
	}
	@Override
	public boolean equals(Object other)
	{
		return this == other;
	}
}
