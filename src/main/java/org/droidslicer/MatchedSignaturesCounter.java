package org.droidslicer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatchedSignaturesCounter
{
	private static final String LOG_FILE_NAME = "log";
	private static final String IMPLETE_TOKEN = "analyzing";
	private class SampleRecord
	{
		private final File mSampleDir;
		private boolean mMatched = false;
		public SampleRecord(File sampleDir)
		{
			mSampleDir = sampleDir;
		}
		public File getSampleDir()
		{
			return mSampleDir;
		}
		public File getLogDir()
		{
			return new File(new File(mSampleDir, mTag), LOG_FILE_NAME);
		}
		public void setSignatureMatched(boolean matched)
		{
			mMatched = matched;
		}
		public boolean isSignatureMatched()
		{
			return mMatched;
		}
	}
	private final static Logger mLogger = LoggerFactory.getLogger(MatchedSignaturesCounter.class);
	private final static String USAGE = "-c <sig_name> -d <samples_dir>";
	private static final String CMD_COUNT_SIG = "c";
	private static final String CMD_TAG = "t";
	private static final String CMD_SAMPLES_DIR = "d";
	private final Set<String> mSigNames = new HashSet<String>();
	private File mSamplesRootDir;
	private SampleRecord[] mSamples;
	private String mTag;
	private static Options buildOptions()
	{
		Options options = new Options();
		options.addOption(CMD_COUNT_SIG, true, "Count the number of apps having the given signatures. (Seperated by ',')")
			.addOption(CMD_SAMPLES_DIR, true, "Directory of sample directory")
			.addOption(CMD_TAG, true, "Tag name of the analysis");
		return options;
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
		
		if(!cmdLine.hasOption(CMD_COUNT_SIG) ||
			!cmdLine.hasOption(CMD_SAMPLES_DIR) ||
			!cmdLine.hasOption(CMD_TAG))
		{
			return false;
		}
		{
			mSigNames.clear();
			String[] sigsStr = cmdLine.getOptionValue(CMD_COUNT_SIG).trim().split(",");
			for(String sigStr : sigsStr)
			{
				sigStr = sigStr.trim();
				if(sigStr.isEmpty())
					continue;
				mSigNames.add(sigStr);
			}
		}
		mSamplesRootDir = new File(cmdLine.getOptionValue(CMD_SAMPLES_DIR));
		mTag= cmdLine.getOptionValue(CMD_TAG);
		if(!mSamplesRootDir.isDirectory())
			return false;
		return true;
	}
	protected static void printHelp(Options options)
	{
		HelpFormatter helpFormatter = new HelpFormatter();  
		helpFormatter.printHelp(USAGE, options);
	}
	protected void findSamples()
			throws IOException
	{
		File[] samples = mSamplesRootDir.listFiles(new FileFilter()
		{
			@Override
			public boolean accept(File dir)
			{
				if(!dir.isDirectory())
					return false;
				boolean result = false;
				do
				{					
					File tagDir = new File(dir, mTag);
					if(!tagDir.isDirectory())
						break;
					File tokenFile = new File(tagDir, IMPLETE_TOKEN);
					if(tokenFile.exists())
						break;
					File logFile = new File(tagDir, LOG_FILE_NAME);
					if(!logFile.isFile())
						break;
					result = true;
				}while(false);
				if(!result)
					mLogger.info("Sample \"" + dir.getPath() + "\" is ignored");
				return result;
			}			
		});
		Arrays.sort(samples, new Comparator<File>()
			{
				@Override
				public int compare(File file1, File file2)
				{
					return file1.getName().compareTo(file2.getName());
				}
			});
		mSamples = new SampleRecord[samples.length];
		for(int i = 0; i < samples.length; ++i)
		{
			mSamples[i] = new SampleRecord(samples[i]);
		}
	}
	private void analyzeSample(SampleRecord sample)
		throws IOException
	{
		File logFile = sample.getLogDir();
		BufferedReader reader = null;
		sample.setSignatureMatched(false);
		try
		{
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
			String line;
			while((line = reader.readLine()) != null)
			{
				line = line.trim();
				if(!line.endsWith("Matched signatures:"))
					continue;
				while((line = reader.readLine()) != null)
				{
					line = line.trim();
					if(!line.startsWith("SIG_"))
						break;
					if(mSigNames.contains(line))
					{
						sample.setSignatureMatched(true);
						break;
					}
				}
				break;
			}
		}
		finally
		{
			if(reader != null)
			{
				try
				{
					reader.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	public MatchedSignaturesCounter(String[] args)
		throws IOException
	{
		Options opts = buildOptions();
		if(!parseArguments(opts, args))
		{
			printHelp(opts);
			System.exit(1);
			return;
		}
		findSamples();
		if(mLogger.isInfoEnabled())
		{
			StringBuilder builder = new StringBuilder();
			for(SampleRecord sample : mSamples)
			{
				builder.append('\t');
				builder.append(sample.getSampleDir().getName());
				builder.append('\n');
			}
			mLogger.info("Samples to be analyzed:\n{}", builder);
			mLogger.info("Total number of samples: {}", mSamples.length);
		}
		for(SampleRecord sample : mSamples)
		{
			analyzeSample(sample);
		}
		int nMatched = 0;
		for(SampleRecord sample : mSamples)
		{
			if(sample.isSignatureMatched())
			{
				++nMatched;
				mLogger.info("Sample \"{}\" is matched", sample.getSampleDir().getName());
			}
		}
		mLogger.info("# matched samples: {}", nMatched);
	}
	public static void main(String[] args)
		throws Exception
	{
		new MatchedSignaturesCounter(args);
	}
}
