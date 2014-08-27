package org.droidslicer.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ProviderPermission
{
	private final Set<String> mAuths = new HashSet<String>();
	private final Set<PathPermission> mPathPerms = new HashSet<PathPermission>();
	private String mReadPerm = null;
	private String mWritePerm = null;
	public void addAuthority(String auth)
	{
		mAuths.add(auth);
	}
	public Collection<String> getAuthories()
	{
		return mAuths;
	}
	public void setReadPermission(String perm)
	{
		mReadPerm = perm;
	}
	public String getReadPermission()
	{
		return mReadPerm;
	}
	public void setWritePermission(String perm)
	{
		mWritePerm = perm;
	}
	public String getWritePermission()
	{
		return mWritePerm;
	}
	public boolean addPathPermission(PathPermission pathPerm)
	{
		return mPathPerms.add(pathPerm);
	}
	public boolean removePathPermission(PathPermission pathPerm)
	{
		return mPathPerms.remove(pathPerm);
	}
	public Collection<PathPermission> getPathPermissions()
	{
		return mPathPerms;
	}
}