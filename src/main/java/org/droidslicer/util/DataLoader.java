package org.droidslicer.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.ipa.callgraph.AnalysisScope;

public class DataLoader
{
	private static final Logger mLogger = LoggerFactory.getLogger(DataLoader.class);
	private final static String API_DIR_PREFIX = "api";
	private final int mAndroidApiVersion;
	private final File mAndroidMethodSummary;
	private final File mAndroidModelConfig;
	private final File mAndroidDefaultModelConfig;
	private final File mAndroidExclusions;
	private final File mAndroidMethodSummaryHelper;
	private final File mPScoutAPIPermMapping;
	private final File mPScoutCPPermMapping;
	private final File mEntryPoints;
	private final File mApiPermissions;
	private final File mIntentPermissions;
	private final File mProviderPermissions;
	private final File mListeners;
	private final File mLibsDir;
	public DataLoader(int androidApiVersion) throws IOException
	{
		if(androidApiVersion <= 0)
			throw new IllegalArgumentException("Invalid Android API version: " + androidApiVersion);
		mAndroidApiVersion = androidApiVersion;
		try
		{
			File androidBase = getConfigBaseForVersion(mAndroidApiVersion);
			if(!androidBase.isDirectory())
				throw new IOException("Unsupported Android SDK version: " + mAndroidApiVersion);
			mAndroidMethodSummary = new File(androidBase, "method_summary.xml");
			mAndroidModelConfig = new File(androidBase, "model.config");
			mAndroidDefaultModelConfig = new File(androidBase, "model_default.config");
			mAndroidExclusions = new File(androidBase, "regression_exclusions.txt");
			mAndroidMethodSummaryHelper = new File(androidBase, "method_summary_helper.jar");
			mPScoutAPIPermMapping = new File(androidBase, "pscout/allmappings.txt");
			mPScoutCPPermMapping = new File(androidBase, "pscout/contentproviderpermission.txt");
			mEntryPoints = new File(androidBase, "entry_points.xml");
			mApiPermissions = new File(androidBase, "api_permissions.xml");
			mIntentPermissions = new File(androidBase, "intent_permissions.xml");
			mProviderPermissions = new File(androidBase, "provider_permissions.xml");
			mListeners = new File(androidBase, "listeners.txt");
			mLibsDir = new File(androidBase, "libs");
		}
		catch(URISyntaxException ex)
		{
			throw new IOException(ex);
		}
	}
	private static File getConfigBase() throws URISyntaxException, IOException
	{
		File file = new File(getCodeUrl());
		file = new File(file.getParent(), "config");
		return file;
	}
	private static File getConfigBaseForVersion(int ver) throws URISyntaxException, IOException
	{
		File configBase = getConfigBase();
		return new File(configBase, API_DIR_PREFIX + ver + "/");
	}
	public static Collection<Integer> getSupportedAndroidAPIVers() throws IOException
	{
		try
		{
			Collection<Integer> result = new ArrayList<Integer>();
			File base = getConfigBase();
			File[] files = base.listFiles();
			if(files == null)
				return new ArrayList<Integer>();
			for(File file : files)
			{
				if(!file.isDirectory())
					continue;
				String name = file.getName();
				if(!name.startsWith(API_DIR_PREFIX))
					continue;
				try
				{
					int ver = Integer.parseInt(name.substring(API_DIR_PREFIX.length()));
					result.add(ver);
				}
				catch(NumberFormatException ex)
				{}				
			}
			return result;
		}
		catch(URISyntaxException ex)
		{
			throw new IOException(ex);
		}
	}
	private static URI getCodeUrl() throws URISyntaxException, IOException
	{
		URL url = DataLoader.class.getProtectionDomain().getCodeSource().getLocation();
		if(url == null)
			throw new IOException("Fail to find code URL");
		return url.toURI();
	}
	
	public File getAndroidMethodSummary()
	{
		return mAndroidMethodSummary;
	}
	
	public File getAndroidExlucsions()
	{
		return mAndroidExclusions;
	}
	public File getAndroidModelConfig()
	{
		return mAndroidModelConfig;
	}
	public File getAndroidDefaultModelConfig()
	{
		return mAndroidDefaultModelConfig;
	}
	public File getAndroidMethodSummaryHelper()
	{
		return mAndroidMethodSummaryHelper;
	}
	public File getPScoutAPIPermMapping()
	{
		return mPScoutAPIPermMapping;
	}
	public File getPScoutCPPermMapping()
	{
		return mPScoutCPPermMapping;
	}
	public File getEntryPoints()
	{
		return mEntryPoints;
	}
	public File getApiPermissions()
	{
		return mApiPermissions;
	}
	public File getIntentPermissions()
	{
		return mIntentPermissions;
	}
	public File getProviderPermissions()
	{
		return mProviderPermissions;
	}
	public File getListeners()
	{
		return mListeners;
	}
	public File getLibsDir()
	{
		return mLibsDir;
	}
	public void loadSystemLibraries(AnalysisScope scope) throws IOException
	{
		{
			File libsDir = getLibsDir();
			if(!libsDir.isDirectory())
				throw new IllegalArgumentException(libsDir.getAbsolutePath() + " isn't a directory");
			List<File> files = new ArrayList<File>();
			for(File file: libsDir.listFiles())
			{
				if(file.isFile() && file.getName().endsWith(".jar"))
					files.add(file);
			}
			Collections.sort(files, new Comparator<File>()
			{
				@Override
				public int compare(File file1, File file2)
				{
					return file1.getName().compareTo(file2.getName());
				}
			});
			for(File file : files)
			{
				scope.addToScope(scope.getPrimordialLoader(), new JarFile(file));
				mLogger.info("System library {} is loaded", file.getAbsoluteFile());				
			}
		}
	}
	public static void main(String[] args) throws Exception
	{
		new DataLoader(16);
	}
	
}
