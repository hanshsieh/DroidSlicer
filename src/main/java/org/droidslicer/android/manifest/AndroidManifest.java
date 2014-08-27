package org.droidslicer.android.manifest;

import java.util.ArrayList;
import java.util.Collection;

public class AndroidManifest
{
	public static class Permission
	{
		private final String mName;
		private final int mMaxSdk;
		public Permission(String name, int maxSdk)
		{
			if(name == null)
				throw new IllegalArgumentException();
			mName = name;
			mMaxSdk = maxSdk;
		}
		public Permission(String name)
		{
			this(name, -1);
		}
		public int getMaxSdkVersion()
		{
			return mMaxSdk;
		}
		public String getName()
		{
			return mName;
		}
	}
	private String mPkgName = null;
	private String mVerName = null;
	private int mVerCode = -1;
	private ArrayList<AndroidActivity> mActivities = new ArrayList<AndroidActivity>();
	private ArrayList<AndroidReceiver> mReceivers = new ArrayList<AndroidReceiver>();
	private ArrayList<AndroidService> mServices = new ArrayList<AndroidService>();
	private ArrayList<AndroidProvider> mProviders = new ArrayList<AndroidProvider>();
	private ArrayList<Permission> mPerms = new ArrayList<Permission>();
	private AndroidApplication mApp = null;
	private int mMinSDKVer = -1, mMaxSDKVer = -1, mTargetSDKVer = -1;
	public void addActivity(AndroidActivity activity)
	{
		mActivities.add(activity);
	}
	public void addPermission(Permission perm)
	{
		mPerms.add(perm);
	}
	public Collection<Permission> getPermissions()
	{
		return mPerms;
	}
	public Collection<AndroidActivity> getActivities()
	{
		return mActivities;
	}
	public void addReceiver(AndroidReceiver receiver)
	{
		mReceivers.add(receiver);
	}
	public Collection<AndroidReceiver> getReceivers()
	{
		return mReceivers;
	}
	public void addProvider(AndroidProvider provider)
	{
		mProviders.add(provider);
	}
	public Collection<AndroidProvider> getProviders()
	{
		return mProviders;
	}
	public void addService(AndroidService service)
	{
		mServices.add(service);
	}
	public Collection<AndroidService> getServices()
	{
		return mServices;
	}
	public AndroidApplication getApplication()
	{
		return mApp;
	}
	public void setApplication(AndroidApplication app)
	{
		mApp = app;
	}
	public void setPackageName(String pkgName)
	{
		mPkgName = pkgName;
	}
	public String getPackageName()
	{
		return mPkgName;
	}
	public int getMinSDKVersion()
	{
		return mMinSDKVer;
	}
	public void setMinSDKVersion(int version)
	{
		mMinSDKVer = version;
	}
	public boolean hasMinSDKVersion()
	{
		return mMinSDKVer > 0;
	}
	public int getMaxSDKVersion()
	{
		return mMaxSDKVer;
	}
	public void setMaxSDKVersion(int version)
	{
		mMaxSDKVer = version;
	}
	public boolean hasMaxSDKVersion()
	{
		return mMaxSDKVer > 0;
	}
	public int getTargetSDKVersion()
	{
		return mTargetSDKVer;
	}
	public void setTargetSDKVersion(int version)
	{
		mTargetSDKVer = version;
	}
	public boolean hasTargetSDKVersion()	
	{
		return mTargetSDKVer > 0;
	}
	public int getVersionCode()
	{
		return mVerCode;
	}
	public void setVersionCode(int verCode)
	{
		mVerCode = verCode;
	}
	public void setVersionName(String verName)
	{
		mVerName = verName;
	}
	public String getVersionName()
	{
		return mVerName;
	}
	
}

