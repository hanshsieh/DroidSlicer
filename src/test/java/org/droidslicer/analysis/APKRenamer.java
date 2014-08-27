package org.droidslicer.analysis;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.droidslicer.analysis.AndroidAppInfo;
import org.droidslicer.android.AndroidAPKFormatException;
import org.droidslicer.android.manifest.AndroidManifest;
import org.droidslicer.android.manifest.AndroidManifestParser;
import org.droidslicer.android.manifest.AndroidManifestSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.v3.Dex2jar;

public class APKRenamer
{
	private final static Logger mLogger = LoggerFactory.getLogger(APKRenamer.class);
	private final static String USAGE = "-t <target>";
	private final static String ANDROID_MANIFEST = "AndroidManifest.xml";
	private final static String CMD_TARGET = "t";
	private File[] mTargets;
	public APKRenamer(String[] args)
	{
		Options opts = buildOptions();
		if(!parseArguments(opts, args))
		{
			printHelp(opts);
			System.exit(1);
			return;
		}
		for(File target : mTargets)
		{
			try
			{
				rename(target);
			}
			catch(Exception ex)
			{
				mLogger.error("Exception occurred when processing file \"" + target.getPath() + "\": ", ex);
			}
		}
	}
	public void rename(File target)
		throws AndroidManifestSyntaxException, IOException
	{
		FileInputStream fileInput = null;
		ZipInputStream apkStream = null;
		AndroidManifest manifest = null;
		try
		{
			fileInput = new FileInputStream(target);
			apkStream = new ZipInputStream(fileInput);
			ZipEntry entry;
			while((entry = apkStream.getNextEntry()) != null)
			{
				if(ANDROID_MANIFEST.equals(entry.getName()))
				{
					manifest = AndroidManifestParser.parseFromCompressed(apkStream, false);
				}
			}
		}
		finally
		{
			if(fileInput != null)
			{
				try
				{
					fileInput.close();
				}
				catch(Exception ex)
				{}
			}
			if(apkStream != null)
			{
				try
				{
					apkStream.close();
				}
				catch(Exception ex)
				{}
			}
		}
		if(manifest == null)
			throw new IOException("Fail to find " + ANDROID_MANIFEST + " in the file \"" + target.getAbsolutePath() + "\"");
		int verCode = manifest.getVersionCode();
		String verName = manifest.getVersionName();
		String pkgName = manifest.getPackageName();
		boolean isOfficial = target.getName().contains("(official)");
		File newFile;
		int renameCount = 0;
		do
		{
			String newName = pkgName + "_" + verCode + "_" + (verName == null ? "" : verName);
			if(isOfficial)
				newName += "(official)";
			if(renameCount > 0)
				newName += "(" + renameCount + ")";
			newName += ".apk";
			newFile = new File(target.getParent(), newName);
			if(newFile.equals(target))
				return;
			renameCount++;
		}while(newFile.exists());
		if(!target.renameTo(newFile))
			throw new IOException("Fail to rename file \"" + target.getPath() + "\" to \"" + newFile.getPath() + "\"");
	}
	private static Options buildOptions()
	{
		Options options = new Options();
		options.addOption(CMD_TARGET, true, "The target directory containing .apk files or a single file that needs to be remained");
		return options;
	}
	protected static void printHelp(Options options)
	{
		HelpFormatter helpFormatter = new HelpFormatter();  
		helpFormatter.printHelp(USAGE, options);
	}
	private boolean parseArguments(Options opts, String[] args)
	{
		CommandLineParser cmdLineParser = new PosixParser();
		CommandLine cmdLine;
		try
		{
			cmdLine = cmdLineParser.parse(opts, args, true);
		}
		catch(ParseException ex)
		{
			return false;
		}
		
		if(!cmdLine.hasOption(CMD_TARGET))
			return false;
		File target = new File(cmdLine.getOptionValue(CMD_TARGET));
		if(target.isDirectory())
		{
			mTargets = target.listFiles(new FileFilter()
			{
				@Override
				public boolean accept(File file)
				{
					if(!file.isFile())
						return false;
					String name = file.getName();
					return name.endsWith(".apk");
				}
			});
			Arrays.sort(mTargets, new Comparator<File>()
			{
				@Override
				public int compare(File file1, File file2)
				{
					return file1.getName().compareTo(file2.getName());
				}				
			});
		}
		else if(target.isFile())
			mTargets = new File[]{target};
		else
		{
			mLogger.error("Target \"" + target.getPath() + " doesn't exist");
			return false;
		}
		return true;
	}
	public static void main(String[] args)
	{
		new APKRenamer(args);
	}
}
