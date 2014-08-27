package org.droidslicer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.analysis.BehaviorGraphBuilder;
import org.droidslicer.graph.BehaviorGraph;
import org.droidslicer.signature.BehaviorSignature;
import org.droidslicer.signature.BehaviorSignaturesTester;
import org.droidslicer.util.BehaviorGraphFocusWriter;
import org.droidslicer.util.GraphUtils;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.ibm.wala.util.CancelException;

public class BatchAnalysisHelper
{
	private final static Logger mLogger = LoggerFactory.getLogger(BatchAnalysisHelper.class);
	private final static String USAGE = "-t <tag> -d <dir> [-s <apk_file>]";
	private final static String CMD_TAG = "t";
	private final static String CMD_DIR = "d";
	private final static String CMD_APK = "s";
	private final static String FILE_TOKEN = "analyzing";
	private File[] mSamples;
	private String mTag;
	private File mSamplesDir;
	private final Predicate<String> mPermOfInterestPred = new Predicate<String>()
		{
			@Override
			public boolean apply(String perm)
			{
				if(perm.contains("READ_SMS") || 
						perm.contains("RECEIVE_SMS") ||
						perm.contains("CALENDAR") ||
						perm.contains("INTERNET") ||
						perm.contains("LOCATION") ||
						perm.contains("CALL_LOG") ||
						perm.contains("READ_CONTACTS") ||
						perm.contains("PHONE_STATE") ||
						perm.contains("READ_HISTORY_BOOKMARKS") ||
						perm.contains("SEND_SMS") ||
						perm.contains("CAMERA") ||
						perm.contains("RECORD_AUDIO"))
					return true;
				else
					return false;
			}
			
		};
	private static Options buildOptions()
	{
		Options options = new Options();
		options.addOption(CMD_TAG, true, "Tag of the run")
			.addOption(CMD_DIR, true, "Directory of the samples")
			.addOption(CMD_APK, true, 
					"Only analyze a single APK file. Only exactly one of -" + CMD_APK + 
					" and -" + CMD_DIR + " should be specified");
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
		
		if(!cmdLine.hasOption(CMD_TAG) ||
			!(cmdLine.hasOption(CMD_DIR) || cmdLine.hasOption(CMD_APK)) || 
			(cmdLine.hasOption(CMD_DIR) && cmdLine.hasOption(CMD_APK)))
		{
			return false;
		}
		mTag = cmdLine.getOptionValue(CMD_TAG).trim();
		if(mTag.isEmpty())
			return false;

		if(cmdLine.hasOption(CMD_DIR))
		{
			mSamplesDir = new File(cmdLine.getOptionValue(CMD_DIR));
			if(!mSamplesDir.isDirectory())
				return false;
		}
		else
		{
			mSamplesDir = null;
			mSamples = new File[]{new File(cmdLine.getOptionValue(CMD_APK))};
		}
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
		File[] dirs = mSamplesDir.listFiles(new FileFilter()
		{
			@Override
			public boolean accept(File pathname)
			{
				return pathname.isDirectory();
			}			
		});
		mSamples = new File[dirs.length];
		for(int i = 0; i < dirs.length; ++i)
		{
			File dir = dirs[i];
			File[] apks = dir.listFiles(new FileFilter()
			{
				@Override	
				public boolean accept(File pathname)
				{
					return pathname.isFile() && pathname.getName().endsWith(".apk");
				}
			});
			if(apks.length != 1)
			{
				if(apks.length == 0)
					throw new IOException("Doesn't find any *.apk in directory " + dir.getPath());
				else
					throw new IOException("Find more than one *.apk in directory " + dir.getPath());
			}		
			mSamples[i] = apks[0];
		}
		Arrays.sort(mSamples, new Comparator<File>()
		{
			@Override
			public int compare(File file1, File file2)
			{
				File p1 = file1.getParentFile();
				File p2 = file2.getParentFile();
				return -p1.getName().compareTo(p2.getName());
				/*long size1 = file1.length();
				long size2 = file2.length();
				if(size1 < size2)
					return -1;
				else if(size1 > size2)
					return 1;
				else
					return 0;*/
			}});
	}
	protected void analyzeSample(File sample)
		throws IOException
	{
		mLogger.info("Analyzing sample " + sample.getAbsolutePath());
		File sampleDir = sample.getParentFile();
		File outputDir = new File(sampleDir, mTag);
		if(outputDir.exists())
		{
			if(!outputDir.isDirectory())
			{
				mLogger.error(outputDir.getAbsolutePath() + " already exists, but isn't a directory, skip it");
				return;
			}
			File[] files = outputDir.listFiles();
			boolean broken = false;
			for(File file : files)
			{
				if(file.getName().equals(FILE_TOKEN))
				{
					mLogger.info("It seems that previous analysis of the sample fails, I'll delete the previous result");
					broken = true;
					break;
				}
			}
			if(!broken)
			{
				mLogger.info("It seems that the sample already has analysis result from previous runs, skip it");
				return;
			}
			FileUtils.deleteDirectory(outputDir);	
		}
		boolean outputDirCreated = false;
		for(int i = 0; i < 5; ++i)
		{
			if(outputDir.mkdir())
			{
				outputDirCreated = true;
				break;
			}
			try
			{
				Thread.sleep(1000 * 3);
			}
			catch(InterruptedException ex)
			{}
		}
		if(!outputDirCreated)
		{
			throw new IOException("Fail to create output directory \"" + outputDir.getAbsolutePath() + "\"");
		}
		
		new FileOutputStream(new File(outputDir, FILE_TOKEN)).close();
		
		ProgressMonitor monitor = new ProgressMonitor();
		ch.qos.logback.classic.Logger rootLogger = 
				(ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		FileAppender<ILoggingEvent> logAppender = null;
		try
		{
			monitor.beginTask("Analyzing sample", 1000);
			{
				logAppender = new FileAppender<ILoggingEvent>();
				logAppender.setFile(new File(outputDir, "log").getAbsolutePath());
				PatternLayoutEncoder encoder = new PatternLayoutEncoder();
				LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			    encoder.setContext(loggerContext);
			    encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36}:%L - %msg%n");
			    encoder.start();
			    logAppender.setEncoder(encoder);
			    ThresholdFilter filter = new ThresholdFilter();
			    filter.setLevel(Level.DEBUG.levelStr);
			    filter.setContext(loggerContext);
			    filter.start();
			    logAppender.addFilter(filter);
			    logAppender.setContext(loggerContext);
				logAppender.start();
				rootLogger.addAppender(logAppender);
			}
			AndroidAnalysisContext analysisCtx = 
					AndroidAnalysisContext.makeDefault(sample, mPermOfInterestPred, new SubProgressMonitor(monitor, 100));
			BehaviorGraph graph = null;
			{
				Stopwatch watch = Stopwatch.createStarted();
				BehaviorGraphBuilder graphBuilder = 
		    			new BehaviorGraphBuilder(analysisCtx);
		    	graph = graphBuilder.build(new SubProgressMonitor(monitor, 800));
				mLogger.info("Analysis finished, elapsed time: {}", watch);
			}
			{
				mLogger.info("Exporting DOT file of behavior supergraph");
				GraphUtils.writeDotFile(graph, new File(outputDir, "behavior_graph.dot"));
			}
			{
				mLogger.info("Exporting units info");
				BehaviorGraphFocusWriter writer = new BehaviorGraphFocusWriter(graph);
				writer.write(new FileOutputStream(new File(outputDir, "units_info.txt")));
			}
			{
				mLogger.info("Matching semantic signatures");
				BehaviorSignaturesTester sigTester = new BehaviorSignaturesTester(graph, new File("semantic_signatures.xml"));
				Stopwatch watch = Stopwatch.createStarted();
				sigTester.test(new SubProgressMonitor(monitor, 100));
				watch.stop();
				//BehaviorSupergraph supergraph = sigTester.getSupergraph();
				if(mLogger.isInfoEnabled())
				{
					StringBuilder builder = new StringBuilder("Matched signatures:\n");
					Collection<BehaviorSignature> sigs = sigTester.getMatchedSignatures();
					for(BehaviorSignature sig : sigs)
					{
						builder.append('\t');
						builder.append(sig.getName());
						builder.append('\n');
					}
					mLogger.info("# matched signatures: {}\n{}", sigs.size(), builder);
				}
				mLogger.info("Elapsed time for matching semantic signatures: {}", watch);
				//mLogger.info("Exporting behavior supergraph...");
				//GraphUtils.writeDotFile(GraphUtils.convertICFG2JGraphT(supergraph), new File(outputDir, "behavior_supergraph.dot"));
			}
			if(!new File(outputDir, FILE_TOKEN).delete())
			{
				throw new IOException("Fail to remove token file " + new File(outputDir, FILE_TOKEN).getAbsolutePath());
			}
		}
		catch(CancelException ex)
		{
			throw new IOException(ex);
		}
		finally
		{
			if(logAppender != null)
			{
				rootLogger.detachAppender(logAppender);
			}
			monitor.done();
		}		
	}
	public BatchAnalysisHelper(String[] args)
		throws IOException
	{
		Options opts = buildOptions();
		if(!parseArguments(opts, args))
		{
			printHelp(opts);
			System.exit(1);
			return;
		}
		if(mSamplesDir != null)
			findSamples();
		for(File sample : mSamples)
		{
			analyzeSample(sample);
		}
	}
	public static void main(String[] args)
	{
		try
		{
			new BatchAnalysisHelper(args);
		}
		catch(Exception ex)
		{
			mLogger.error("Exception occurred: ", ex);
		}
		catch(Error err)
		{
			mLogger.error("Error occurred: ", err);
		}
	}
}
