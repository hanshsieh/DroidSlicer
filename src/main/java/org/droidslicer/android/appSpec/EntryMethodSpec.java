package org.droidslicer.android.appSpec;

import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.MutableIntSet;

public class EntryMethodSpec
{
	private final MethodReference mMethod;
	private final boolean mIsStatic;
	private MutableIntSet mParamTrack = new BitVectorIntSet();
	public EntryMethodSpec(MethodReference method, boolean isStatic)
	{
		if(method == null)
			throw new IllegalArgumentException();
		mMethod = method;
		mIsStatic = isStatic;
	}
	public int getNumberOfParameters()
	{
		if(mIsStatic)
			return mMethod.getNumberOfParameters();
		else
			return mMethod.getNumberOfParameters() + 1;
	}
	public boolean isStatic()
	{
		return mIsStatic;
	}
	public MethodReference getMethod()
	{
		return mMethod;
	}
	private void checkParamIndex(int paramIdx)
	{
		int nParam = getNumberOfParameters();
		if(paramIdx < 0 || paramIdx >= nParam)
			throw new IllegalArgumentException("Illegal parameter index: " + paramIdx);
	}
	
	/**
	 * Set whether a parameter should be track. Notice that parameter at index 0
	 * would be the implicit 'this' parameter for non-static methods.
	 * @param paramIdx
	 * @param track
	 */
	public void setParamTrack(int paramIdx, boolean track)
	{
		checkParamIndex(paramIdx);
		if(track)
			mParamTrack.add(paramIdx);
		else
			mParamTrack.remove(paramIdx);
	}
	/**
	 * Returns whether a parameter should be tracked. Notice that parameter at index 0
	 * would be the implicit 'this' parameter for non-static methods.
	 * @param paramIdx
	 * @return whether the parameter should be tracked
	 */
	public boolean isParamTrack(int paramIdx)
	{
		checkParamIndex(paramIdx);
		return mParamTrack.contains(paramIdx);
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof EntryMethodSpec))
			return false;
		EntryMethodSpec that = (EntryMethodSpec)other;
		return mMethod.equals(that.mMethod) && mIsStatic == that.mIsStatic;
	}
	@Override
	public int hashCode()
	{
		return (mMethod.hashCode() << 1) | (mIsStatic ? 1 : 0);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("method=[");
		builder.append(mMethod);
		builder.append("], static=");
		builder.append(mIsStatic);
		return builder.toString();
	}
}
