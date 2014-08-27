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
import java.util.HashSet;
import java.util.Set;
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

public class MatchedSignatureTableGenerator
{
	private final static Logger mLogger = LoggerFactory.getLogger(MatchedSignatureTableGenerator.class);
	private final static String USAGE = "-d <samples_dir> -t <analysis_tag> -o <output>";
	private final static String CMD_SAMPLES_DIR = "d";
	private final static String CMD_TAG = "t";
	private final static String CMD_OUTPUT = "o";
	private static final String LOG_FILE_NAME = "log";
	private static final String IMPLETE_TOKEN = "analyzing";
	private static class Source
	{
		private final Pattern mPat;
		private final String mName;
		public Source(Pattern pat, String name)
		{
			mPat = pat;
			mName = name;
		}
		public boolean isMatched(String sig)
		{
			Matcher matcher = mPat.matcher(sig);
			return matcher.find();
		}
		public String getName()
		{
			return mName;
		}
	}
	private static class Sink
	{
		private final Pattern mPat;
		private final String mName;
		public Sink(Pattern pat, String name)
		{
			mPat = pat;
			mName = name;
		}
		public boolean isMatched(String sig)
		{
			Matcher matcher = mPat.matcher(sig);
			return matcher.find();
		}
		public String getName()
		{
			return mName;
		}
	}
	public static Source[] mSources = new Source[]{
		new Source(Pattern.compile("_SMS_TO_|_MMS_TO"), "SMS"),
		new Source(Pattern.compile("INTERNET_TO_"), "Internet"),
		new Source(Pattern.compile("LOCATION_TO_"), "location"),
		new Source(Pattern.compile("CALENDAR_TO_"), "calendar"),
		new Source(Pattern.compile("CALL_LOG_TO_"), "call log"),
		new Source(Pattern.compile("READ_CONTACTS_TO_"), "contacts"),
		new Source(Pattern.compile("PHONE_STATE_TO_"), "phone state"),
		new Source(Pattern.compile("READ_HISTORY_BOOKMARKS_TO_"), "history bookmarks"),
		new Source(Pattern.compile("(READ_EXTERNAL_STORAGE|_FILE|_DATABASE|_SHARED_PREFERENCES).*_TO_"), "file"),
		//new Source(Pattern.compile("CAMERA_TO_"), "camera"),
		//new Source(Pattern.compile("RECORD_AUDIO_TO_"), "audio"),
	};
	public static Sink[] mSinks = new Sink[]{
		//new Sink(Pattern.compile("_TO_.*_FILE|_TO_.*_DATABASE|_TO_.*_SHARED_PREFERENCES"), "file"),
		new Sink(Pattern.compile("_TO_.*(WRITE_EXTERNAL_STORAGE|_FILE|_DATABASE|_SHARED_PREFERENCES)"), "file"),
		new Sink(Pattern.compile("_TO_.*_INTERNET"), "Internet"),
		new Sink(Pattern.compile("_TO_.*_SEND_SMS"), "SMS"),
	};
	private class SampleRecord
	{
		private final File mSampleDir;
		private Set<String> mSigs = new HashSet<String>();
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
		public Set<String> getSignatures()
		{
			return mSigs;
		}
		public void addSignature(String sig)
		{
			mSigs.add(sig);
		}
	}
	
	private File mSamplesRootDir;
	private SampleRecord[] mSamples;
	private String mTag;
	private File mOutputFile;
	private static Options buildOptions()
	{
		Options options = new Options();
		options.addOption(CMD_SAMPLES_DIR, true, "Directory of sample directory")
			.addOption(CMD_TAG, true, "Tag name of the analysis")
			.addOption(CMD_OUTPUT, true, "Output file");
		return options;
	}
	private void collectMatchedSignatures(SampleRecord sample)
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
				if(!line.endsWith("Matched signatures:"))
					continue;
				while((line = reader.readLine()) != null)
				{
					line = line.trim();
					if(!line.startsWith("SIG_"))
						break;
					sample.addSignature(line);
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
	private void genSampleRow(Writer output, SampleRecord sample)
		throws IOException
	{
		output.write(sample.getSampleName());
		for(Source source : mSources)
		{
			for(Sink sink : mSinks)
			{
				boolean matched = false;
				for(String sig : sample.getSignatures())
				{
					if(source.isMatched(sig) && sink.isMatched(sig))
					{
						matched = true;
						break;
					}
				}
				output.write('\t');
				if(matched)
					output.write("V");
				else
					output.write(" ");
			}
		}
		output.write('\n');
	}
	private void generateSummaryTable(Writer output)
		throws IOException
	{
		for(Sink sink : mSinks)
		{
			output.write('\t');
			output.write(sink.getName());
		}
		output.write('\n');
		for(Source source : mSources)
		{
			output.write(source.getName());
			for(Sink sink : mSinks)
			{
				output.write('\t');
				int nMatched = 0;
				for(SampleRecord sample : mSamples)
				{
					for(String sig : sample.getSignatures())
					{
						if(source.isMatched(sig) && sink.isMatched(sig))
						{
							++nMatched;
							break;
						}
					}
				}
				output.write(String.valueOf(nMatched));
			}
			output.write('\n');
		}
	}
	private void generateOutput()
		throws IOException
	{
		Writer output = null;
		try
		{
			output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mOutputFile)));
			{
				int sigIdx = 0;
				for(int i = 0; i < mSources.length * mSinks.length; ++i)
				{
					output.write('\t');
					output.write("sig" + (sigIdx + 1));
					++sigIdx;
				}
			}
			output.write('\n');
			for(SampleRecord sample : mSamples)
			{
				genSampleRow(output, sample);
			}
			output.write('\n');
			{
				int sigIdx = 0;
				for(Source source : mSources)
				{
					for(Sink sink : mSinks)
					{
						output.write("sig" + (sigIdx + 1));
						output.write('\t');
						output.write(source.getName() + " to " + sink.getName());
						output.write('\n');
						++sigIdx;
					}
				}
			}
			output.write('\n');
			generateSummaryTable(output);
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
	public MatchedSignatureTableGenerator(String[] args)
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
			collectMatchedSignatures(sample);
		}
		generateOutput();
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
	public static void main(String[] args)
		throws Exception
	{
		new MatchedSignatureTableGenerator(args);
	}
}
