package org.droidslicer.signature;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.droidslicer.graph.BehaviorSupergraph;

public class SemanticSignatureContext
{
	private BehaviorSupergraph mSupergraph = null;
	private Collection<Signature> mPendingSigs = new HashSet<Signature>();
	private Map<Signature, Boolean> mSigsVal = new HashMap<Signature, Boolean>();
	public SemanticSignatureContext()
	{
		
	}
	public Collection<Signature> getPendingSignature()
	{
		return mPendingSigs;
	}
	public void setSignatureValue(Signature sig, boolean val)
	{
		mPendingSigs.remove(sig);
		mSigsVal.put(sig, val);
	}
	public void setSupergraph(BehaviorSupergraph supergraph)
	{
		if(mSupergraph != null && mSupergraph == supergraph)
			return;
		mSupergraph = supergraph;
	}
	public boolean evaluate(Signature sig)
		throws UnevaluatedSignatureException
	{
		Boolean val = mSigsVal.get(sig);
		if(val == null)
		{
			mPendingSigs.add(sig);
			throw new UnevaluatedSignatureException();
		}
		else
			return val;
	}
}
