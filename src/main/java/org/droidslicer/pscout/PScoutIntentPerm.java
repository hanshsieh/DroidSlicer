package org.droidslicer.pscout;

public class PScoutIntentPerm
{
	private final String mAction;
	private final String mPerm;
	
	// Whether the permission is enforced on the sender of the Intent or 
	// the receiver of the intent
	private final boolean mIsSender;
	public PScoutIntentPerm(String action, String perm, boolean isSender)
	{
		if(action == null || perm == null)
			throw new IllegalArgumentException();
		mAction = action;
		mPerm = perm;
		mIsSender = isSender;
	}
	public String getPermission()
	{
		return mPerm;
	}
	public String getAction()
	{
		return mAction;
	}
	public boolean isSender()
	{
		return mIsSender;
	}
	@Override
	public int hashCode() 
	{
		return mAction.hashCode() * 31 + mPerm.hashCode() * 2 + (mIsSender ? 1 : 0);
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof PScoutIntentPerm))
			return false;
		PScoutIntentPerm that = (PScoutIntentPerm)other;
		return mAction.equals(that.mAction) && mPerm.equals(that.mPerm) && mIsSender == that.mIsSender;
	}
}
