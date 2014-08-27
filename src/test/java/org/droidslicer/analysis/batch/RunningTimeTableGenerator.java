package org.droidslicer.analysis.batch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

public class RunningTimeTableGenerator 
{
	private class SampleRecord
	{
		private final File mSampleDir;
		private long mCallGraphGenTimeMilli = -1;
		private long mEntitiesSolveTimeMilli = -1;
		private long mDependSolveTimeMilli = -1;
		private long mEdgeFindTimeMilli = -1;
		private long mImplicitEdgeFindTimeMilli = -1;
		private long mSigMatchTimeMilli = -1;
		private Map<RunningTimeType, Long> mRunningType = new HashMap<RunningTimeType, Long>();
		
		public SampleRecord(File sampleDir)
		{
			mSampleDir = sampleDir;
		}
		public String getSampleName()
		{
			String name = mSampleDir.getName();
			int idx = name.indexOf(' ');
			if(idx < 0)
				return name;
			return name.substring(idx + 1);
		}
		public File getSampleDir()
		{
			return mSampleDir;
		}
		public File getLogFile()
		{
			return new File(new File(mSampleDir, mTag), LOG_FILE_NAME);
		}
		public void setRunningTime(RunningTimeType type, long time)
		{
			mRunningType.put(type, time);
		}
		public long getRunningTime(RunningTimeType type)
		{
			Long time = mRunningType.get(type);
			return time == null ? -1 : time;
		}
		public long getCallGraphGenerationTime()
		{
			return mCallGraphGenTimeMilli;
		}
		public long getEntitiesSolveTime()
		{
			return mEntitiesSolveTimeMilli;
		}
		public long getDependSolveTime()
		{
			return mDependSolveTimeMilli;
		}
		public long getEdgeFindTime()
		{
			return mEdgeFindTimeMilli;
		}
		public long getImplicitEdgeFindTime()
		{
			return mImplicitEdgeFindTimeMilli;
		}
		public long getSignatureMatchTime()
		{
			return mSigMatchTimeMilli;
		}
		public void setCallGraphGenerationTime(long time)
		{
			mCallGraphGenTimeMilli = time;
		}
		public void getEntitiesSolveTime(long time)
		{
			mEntitiesSolveTimeMilli = time;
		}
		public void getDependSolveTime(long time)
		{
			mDependSolveTimeMilli = time;
		}
		public void getEdgeFindTime(long time)
		{
			mEdgeFindTimeMilli = time;
		}
		public void getImplicitEdgeFindTime(long time)
		{
			mImplicitEdgeFindTimeMilli = time;
		}
		public void getSignatureMatchTime(long time)
		{
			mSigMatchTimeMilli = time;
		}
	}
	private static class RunningTimeType
	{
		private final String mName;
		private final Pattern mPat;
		public RunningTimeType(String name, Pattern pat)
		{
			mName = name;
			mPat = pat;
		}
		public String getName()
		{
			return mName;
		}
		public Pattern getPattern()
		{
			return mPat;
		}
	}
	private final static Logger mLogger = LoggerFactory.getLogger(RunningTimeTableGenerator.class);
	private final static String USAGE = "-d <samples_dir> -t <analysis_tag> -o <output>";
	private final static String CMD_SAMPLES_DIR = "d";
	private final static String CMD_TAG = "t";
	private final static String CMD_OUTPUT = "o";
	private final static String LOG_FILE_NAME = "log";
	private final static String IMPLETE_TOKEN = "analyzing";
	private final static Pattern PAT_CALL_GRAPH_GEN = Pattern.compile("Call graph generation finished. Elapsed time:\\s*(\\S+)\\s+(\\S+)$");
	private final static Pattern PAT_ENTITIES_SOLVE = Pattern.compile("Finished solving entities. Elapsed time:\\s*(\\S+)\\s+(\\S+)$");
	private final static Pattern PAT_DEPEND_SOLVE = Pattern.compile("Finished solving dependencies. Elapsed time:\\s*(\\S+)\\s+(\\S+)$");
	private final static Pattern PAT_EDGE_FIND = Pattern.compile("Elapsed time for finding edges for data dependencies:\\s*(\\S+)\\s+(\\S+)$");
	private final static Pattern PAT_IMPLICIT_EDGE_FIND = Pattern.compile("Elapsed time for finding edges for implicit relationship:\\s*(\\S+)\\s+(\\S+)$");
	private final static Pattern PAT_SIG_MATCH = Pattern.compile("Elapsed time for matching semantic signatures:\\s*(\\S+)\\s+(\\S+)$");
	private final static RunningTimeType[] RUNNING_TIME_TYPES = new RunningTimeType[]
	{
		new RunningTimeType("Call graph generation", PAT_CALL_GRAPH_GEN),
		new RunningTimeType("Solving entities", PAT_ENTITIES_SOLVE),
		new RunningTimeType("Solving dependency", PAT_DEPEND_SOLVE),
		new RunningTimeType("Finding normal edges", PAT_EDGE_FIND),
		new RunningTimeType("Finding implicit edges", PAT_IMPLICIT_EDGE_FIND),
		new RunningTimeType("Matching signatures", PAT_SIG_MATCH)
	};
	private File mSamplesRootDir;
	private SampleRecord[] mSamples;
	private String mTag;
	private File mOutputFile;
	public RunningTimeTableGenerator(String[] args)
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
		for(SampleRecord sample : mSamples)
		{
			collectRunningTime(sample);
		}
		generateOutput();
	}
	protected static long parseTimeString(String timeStr, String unitStr)
	{
		TimeUnit unit;
		if(unitStr.equals("ns"))
			unit = TimeUnit.NANOSECONDS;
		else if(unitStr.equals("\u03bcs"))
			unit = TimeUnit.MICROSECONDS;
		else if(unitStr.equals("ms"))
			unit = TimeUnit.MILLISECONDS;
		else if(unitStr.equals("s"))
			unit = TimeUnit.SECONDS;
		else if(unitStr.equals("min"))
			unit = TimeUnit.MINUTES;
		else if(unitStr.equals("h"))
			unit = TimeUnit.HOURS;
		else if(unitStr.equals("d"))
			unit = TimeUnit.DAYS;
		else
			throw new IllegalArgumentException("Illegal time unit string: \"" + unitStr + "\"");
		double time = Double.parseDouble(timeStr);
		return TimeUnit.MILLISECONDS.convert(Math.round(time * 1000000.0), unit) / 1000000;
	}
	protected void generateOutput()
		throws IOException
	{
		Writer output = null;
		try
		{
			output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mOutputFile)));
			// Print table header
			{
				for(int i = 0; i < RUNNING_TIME_TYPES.length; ++i)
				{
					output.write('\t');
					output.write(RUNNING_TIME_TYPES[i].getName());
				}
				output.write('\n');
			}
			
			// Print table content
			{
				for(SampleRecord sample : mSamples)
				{
					output.write(sample.getSampleName());
					for(RunningTimeType type : RUNNING_TIME_TYPES)
					{
						output.write('\t');
						long time = sample.getRunningTime(type);
						if(time < 0)
							throw new IllegalArgumentException("Fail to find running of type \"" + type.getName() + "\" for sample \"" + sample.getSampleName() + "\"");
						output.write(Long.toString(time));
					}
					output.write('\n');
				}
			}
			mLogger.info("Output has been written to \"" + mOutputFile.getPath() + "\"");
		}
		finally
		{
			if(output != null)
			{
				try
				{
					output.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	protected void collectRunningTime(SampleRecord sample)
		throws IOException
	{
		File logFile = sample.getLogFile();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
			String line;
			while((line = reader.readLine()) != null)
			{
				line = line.trim();
				while((line = reader.readLine()) != null)
				{
					for(RunningTimeType type : RUNNING_TIME_TYPES)
					{
						Pattern pat = type.getPattern();
						Matcher matcher = pat.matcher(line);
						if(!matcher.find())
							continue;
						String timeStr = matcher.group(1);
						String unitStr = matcher.group(2);
						long time = parseTimeString(timeStr, unitStr);
						sample.setRunningTime(type, time);
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
		
		if(!cmdLine.hasOption(CMD_SAMPLES_DIR) ||
			!cmdLine.hasOption(CMD_TAG) || 
			!cmdLine.hasOption(CMD_OUTPUT))
		{
			return false;
		}
		mSamplesRootDir = new File(cmdLine.getOptionValue(CMD_SAMPLES_DIR));
		mTag= cmdLine.getOptionValue(CMD_TAG);
		mOutputFile = new File(cmdLine.getOptionValue(CMD_OUTPUT));
		if(!mSamplesRootDir.isDirectory())
			return false;
		return true;
	}
	private static Options buildOptions()
	{
		Options options = new Options();
		options.addOption(CMD_SAMPLES_DIR, true, "Directory of sample directory")
			.addOption(CMD_TAG, true, "Tag name of the analysis")
			.addOption(CMD_OUTPUT, true, "Output file");
		return options;
	}
	public static void main(String[] args)
			throws Exception
	{
		new RunningTimeTableGenerator(args);
	}
}
