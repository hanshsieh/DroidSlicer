package org.droidslicer.signature;


public class FlowSignature extends Signature
{
	private final UnitSignaturesUnion mFrom;
	private final UnitSignaturesUnion mTo;
	
	/**
	 * If {@code} is null, then the is signature is matched if {@code from} 
	 * signature can match
	 * @param from
	 * @param to
	 */
	public FlowSignature(UnitSignaturesUnion from, UnitSignaturesUnion to)
	{
		if(from == null || to == null)
			throw new IllegalArgumentException();
		mFrom = from;
		mTo = to;
	}
	public UnitSignaturesUnion getFrom()
	{
		return mFrom;
	}
	public UnitSignaturesUnion getTo()
	{
		return mTo;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("from=[");
		builder.append(mFrom);
		builder.append("], to=[");
		builder.append(mTo);
		builder.append(']');
		return builder.toString();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof FlowSignature))
			return false;
		FlowSignature that = (FlowSignature)other;
		return mFrom.equals(that.mFrom) && mTo.equals(that.mTo);
	}
	@Override
	public int hashCode()
	{
		return mFrom.hashCode() + mTo.hashCode();
	}
}
