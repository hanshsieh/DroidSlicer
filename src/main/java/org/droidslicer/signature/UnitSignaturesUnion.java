package org.droidslicer.signature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.droidslicer.graph.BehaviorNode;
import org.droidslicer.graph.BehaviorSupergraph;
import org.droidslicer.graph.entity.UnitEntity;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

public class UnitSignaturesUnion extends Signature
{
	private int mHash = 0;
	private final Set<UnitSignature> mSigs = new HashSet<UnitSignature>();
	public void addSigature(UnitSignature sig)
	{
		mHash = 0;
		mSigs.add(sig);
	}
	public Set<UnitSignature> getSignatures()
	{
		return mSigs;
	}
	public Set<BehaviorNode> evaluate(BehaviorSupergraph supergraph, UnitEntity unit)
	{
		final Collection<UnitSignature> possibleMatched = new ArrayList<UnitSignature>();
		boolean matched = false;
		{
			for(UnitSignature sig : mSigs)
			{
				if(matched)
					break;
				switch(sig.isBasicMatched(unit))
				{
				case NOT_MATCHED:
					break;
				case MATCHED:
					matched = true;
					break;
				default:
					possibleMatched.add(sig);
					break;
				}
			}
			if(!matched && possibleMatched.isEmpty())
				return Collections.emptySet();
		}
		Set<BehaviorNode> nodes = supergraph.getNodesForUnit(unit);
		if(matched)
			return nodes;
		return Sets.filter(nodes, new Predicate<BehaviorNode>()
		{
			@Override
			public boolean apply(BehaviorNode node)
			{
				boolean accept = false;
				for(UnitSignature sig : possibleMatched)
				{
					if(sig.isMatched(node))
					{
						accept = true;
						break;
					}
				}
				return accept;
			}			
		});
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("sigs: ");
		boolean first = true;
		for(UnitSignature sig : mSigs)
		{
			if(first)
				first = false;
			else
				builder.append(", ");
			builder.append('[');
			builder.append(sig);
			builder.append(']');
		}
		return builder.toString();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof UnitSignaturesUnion))
			return false;
		UnitSignaturesUnion that = (UnitSignaturesUnion)other;
		return mSigs.equals(that.mSigs);
	}
	@Override
	public int hashCode()
	{
		if(mHash == 0)
		{
			mHash = mSigs.hashCode();
			if(mHash == 0)
				mHash = 1;
		}
		return mHash;
	}
}
