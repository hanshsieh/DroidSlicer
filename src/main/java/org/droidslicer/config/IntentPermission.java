package org.droidslicer.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class IntentPermission
{
	private final String mAction;
	private final Set<String> mSenderPerms = new HashSet<String>();
	private final Set<String> mReceiverPerms = new HashSet<String>();
	public IntentPermission(String action)
	{
		mAction = action;
	}
	public String getAction()
	{
		return mAction;
	}
	public void addSenderPermission(String perm)
	{
		mSenderPerms.add(perm);
	}
	public Collection<String> getSenderPermissions()
	{
		return mSenderPerms;
	}
	public void addReceiverPermission(String perm)
	{
		mReceiverPerms.add(perm);
	}
	public Collection<String> getReceiverPermissions()
	{
		return mReceiverPerms;
	}
	public void removeSenderPermission(String perm)
	{
		mSenderPerms.remove(perm);
	}
	public void removeReceiverPermission(String perm)
	{
		mReceiverPerms.remove(perm);
	}
}
