package org.droidslicer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.analysis.BehaviorGraphBuilder;
import org.droidslicer.graph.BehaviorGraph;
import org.droidslicer.signature.BehaviorSignature;
import org.droidslicer.signature.BehaviorSignaturesTester;
import org.droidslicer.util.BehaviorGraphFocusWriter;
import org.droidslicer.util.GraphUtils;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;

import com.google.common.base.Stopwatch;
import com.ibm.wala.util.CancelException;

public class DroidSlicerNoGUI
{
	private final static Logger mLogger = LoggerFactory.getLogger(DroidSlicerNoGUI.class);

	/**
	 * Usage string of this tool
	 */
	public static final String USAGE = "-t <apk> [-b <behavior_graph] [-s <signature_file>] [-m <matched_sigs>] [-u <units_info>] [-l <log>] [-p <log_pattern>] [-e <log_level>]";
	
	/**
	 *  Command line options
	 */
	private static final String CMD_TARGET_APK = "t";
	private static final String CMD_BEHAVIOR_GRAPH = "b";
	private static final String CMD_SIGS = "s";
	private static final String CMD_MATCHED_SIGS = "m";
	private static final String CMD_UNITS_INFO = "u";
	private static final String CMD_LOG = "l";
	private static final String CMD_LOG_PATTERN = "p";
	private static final String CMD_LOG_LEVEL = "e";
	
	public static final String DEFAULT_LOG_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36}:%L - %msg%n";
	public static final String DEFAULT_LOG_LEVEL = Level.INFO.levelStr;
	
	private File mTargetAPK = null;
	private File mSBGDotFile = null;
	private File mSigsFile = null;
	private File mMatchedSigsFile = null;
	private File mUnitsInfoFile = null;
	private File mLogFile = null;
	private String mLogPattern = DEFAULT_LOG_PATTERN;
	private String mLogLevel = DEFAULT_LOG_LEVEL;
	public static void main(String[] args)
	{
		try
		{
			Utils.configureLogging();
			DroidSlicerNoGUI analyzer = new DroidSlicerNoGUI();
			if(!analyzer.parseArguments(args))
			{
				DroidSlicerNoGUI.printHelp();
				System.exit(1);
				return;
			}
			analyzer.start();
		}
		catch(Exception ex)
		{
			mLogger.error("Exception occurred: ", ex);
		}
	}
	public DroidSlicerNoGUI()
	{}
	public void setTargetAPK(File file)
	{
		if(file == null)
			throw new IllegalArgumentException("The target APK cannot be null");
		mTargetAPK = file;
	}
	public File getTargetAPK()
	{
		return mTargetAPK;
	}
	public void setOutputSBGFile(File file)
	{
		mSBGDotFile = file;
	}
	public File getOutputSBGFile(File file)
	{
		return mSBGDotFile;
	}
	public void setSignaturesFile(File file)
	{
		mSigsFile = file;
	}
	public File getSignaturesFile()
	{
		return mSigsFile;
	}
	public void setOutputMatchedSignaturesFile(File file)
	{
		mMatchedSigsFile = file;
	}
	public File getOutputMatchedSignaturesFile()
	{
		return mMatchedSigsFile;
	}
	public void setOutputUnitsInfoFile(File file)
	{
		mUnitsInfoFile = file;
	}
	public File getOutputUnitsInfoFile()
	{
		return mUnitsInfoFile;
	}
	public void setExtraLogFile(File file)
	{
		mLogFile = file;
	}
	public File getExtraLogFile()
	{
		return mLogFile;
	}
	public void setLogPattern(String pattern)
	{
		mLogPattern = pattern;
	}
	public void setLogLevel(String level)
	{
		mLogLevel = level;
	}
	public String getLogLevel()
	{
		return mLogLevel;
	}	
	private Appender<ILoggingEvent> setLogAppender(ch.qos.logback.classic.Logger rootLogger)
	{
		FileAppender<ILoggingEvent> logAppender = null;
		if(mLogFile != null)
		{
			logAppender = new FileAppender<ILoggingEvent>();
			logAppender.setFile(mLogFile.getAbsolutePath());
			PatternLayoutEncoder encoder = new PatternLayoutEncoder();
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		    encoder.setContext(loggerContext);
		    encoder.setPattern(mLogPattern == null ? DEFAULT_LOG_PATTERN : mLogPattern);
		    encoder.start();
		    logAppender.setEncoder(encoder);
		    ThresholdFilter filter = new ThresholdFilter();
		    filter.setLevel(mLogLevel == null ? DEFAULT_LOG_LEVEL : mLogLevel);
		    filter.setContext(loggerContext);
		    filter.start();
		    logAppender.addFilter(filter);
		    logAppender.setContext(loggerContext);
			logAppender.start();
			rootLogger.addAppender(logAppender);
		}
		return logAppender;
	}
	private void writeSignatures(Collection<BehaviorSignature> sigs, File file)
		throws IOException
	{
		BufferedWriter writer = null;
		FileWriter fileWriter = null;
		try
		{
			fileWriter = new FileWriter(file);
			writer = new BufferedWriter(fileWriter);
			for(BehaviorSignature sig : sigs)
			{
				writer.write(sig.getName());
				writer.write('\n');
			}
			writer.flush();
		}
		finally
		{
			if(fileWriter != null)
			{
				try
				{
					fileWriter.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	public void start()
		throws IOException
	{
		if(mTargetAPK == null)
			throw new IllegalArgumentException("The target APK hasn't been specified");
		ProgressMonitor monitor = new ProgressMonitor();
		ch.qos.logback.classic.Logger rootLogger = 
				(ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		Appender<ILoggingEvent> logAppender = null;
		try
		{
			monitor.beginTask("Analyzing sample", 1000);
			logAppender = setLogAppender(rootLogger);
			AndroidAnalysisContext analysisCtx = 
					AndroidAnalysisContext.makeDefault(mTargetAPK, new SubProgressMonitor(monitor, 100));
			
			// Build the behavior graph
			BehaviorGraph graph = null;
			{
				mLogger.info("Building the sensitive behavior graph");
				Stopwatch watch = Stopwatch.createStarted();
				BehaviorGraphBuilder graphBuilder = 
		    			new BehaviorGraphBuilder(analysisCtx);
		    	graph = graphBuilder.build(new SubProgressMonitor(monitor, 800));
				mLogger.info("Behavior graph is built. Elapsed time: {}", watch);
			}
			if(mSBGDotFile != null)
			{
				mLogger.info("Exporting DOT file of behavior supergraph");
				GraphUtils.writeDotFile(graph, mSBGDotFile);
			}
			if(mUnitsInfoFile != null)
			{
				mLogger.info("Exporting units info");
				BehaviorGraphFocusWriter writer = new BehaviorGraphFocusWriter(graph);
				writer.write(new FileOutputStream(mUnitsInfoFile));
			}
			if(mSigsFile != null)
			{
				mLogger.info("Matching sensitive behavior signatures");
				BehaviorSignaturesTester sigTester = new BehaviorSignaturesTester(graph, mSigsFile);
				Stopwatch watch = Stopwatch.createStarted();
				sigTester.test(new SubProgressMonitor(monitor, 100));
				mLogger.info("Elapsed time for matching sensitive behavior signatures: {}", watch);
				Collection<BehaviorSignature> sigs = sigTester.getMatchedSignatures();
				if(mLogger.isInfoEnabled())
				{
					StringBuilder builder = new StringBuilder("Matched signatures:\n");
					for(BehaviorSignature sig : sigs)
					{
						builder.append('\t');
						builder.append(sig.getName());
						builder.append('\n');
					}
					mLogger.info("# matched signatures: {}\n{}", sigs.size(), builder);
				}
				if(mMatchedSigsFile != null)
					writeSignatures(sigs, mMatchedSigsFile);
			}
		}
		catch(CancelException ex)
		{
			throw new IOException(ex);
		}
		finally
		{
			if(logAppender != null)
				rootLogger.detachAppender(logAppender);
			monitor.done();
		}	
	}
	protected static Options buildOptions()
	{
		Options options = new Options();
		options.addOption(CMD_TARGET_APK, true, 
				"The target APK to be analyzed. (required)");
		options.addOption(CMD_BEHAVIOR_GRAPH, true, 
				"The file to store the behavior graph (in DOT format). " + 
				"If not specified, the behaivor graph won't be stored. (optional)");
		options.addOption(CMD_SIGS, true,
				"The file specifiying the sensitive behavior signatures to be matched. " + 
				"If this option isn't specified, then no signatures will be matched. (optional)");
		options.addOption(CMD_MATCHED_SIGS, true, 
				"The file to store the matched signatures. " + 
				"If this option isn't specified, then the matched signatures won't be stored to a file. (optional)");
		options.addOption(CMD_UNITS_INFO, true, 
				"The file to store the units information. " + 
				"If not specified, then it won't be stored to a file. (optional)");
		options.addOption(CMD_LOG, true, 
				"This option is used to specify an additional file to store the log in addition to the " + 
				"ones specified in the logging configuration file. (optional)");
		options.addOption(CMD_LOG_PATTERN, true,
				"Set the log pattern. This option is valid only if -" + CMD_LOG + " option is specified. " + 
				"If not specified, then the default log pattern \"" + DEFAULT_LOG_PATTERN + "\" will be used.");
		options.addOption(CMD_LOG_LEVEL, true,
				"Set the log level. This option is valid only if -" + CMD_LOG + " option is specified. " + 
				"If not specified, then the default log level \"" + DEFAULT_LOG_LEVEL + "\" is used.");
		return options;
	}
	public static void printHelp()
	{
		Options opts = buildOptions();
		HelpFormatter helpFormatter = new HelpFormatter();  
		helpFormatter.printHelp(USAGE, opts);
	}
	private boolean parseArguments(String[] args)
	{
		Options opts = buildOptions();
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
		
		if(!cmdLine.hasOption(CMD_TARGET_APK))
			return false;
		mTargetAPK = new File(cmdLine.getOptionValue(CMD_TARGET_APK));
		if(!mTargetAPK.isFile())
		{
			mLogger.error("File not found: \"{}\"", mTargetAPK.getPath());
			return false;
		}
		if(cmdLine.hasOption(CMD_BEHAVIOR_GRAPH))
			mSBGDotFile = new File(cmdLine.getOptionValue(CMD_BEHAVIOR_GRAPH));
		else
			mSBGDotFile = null;
		if(cmdLine.hasOption(CMD_SIGS))
		{
			mSigsFile = new File(cmdLine.getOptionValue(CMD_SIGS));
			if(cmdLine.hasOption(CMD_MATCHED_SIGS))
				mMatchedSigsFile = new File(cmdLine.getOptionValue(CMD_MATCHED_SIGS));
			else
				mMatchedSigsFile = null;
		}
		else
			mSigsFile = null;
		if(cmdLine.hasOption(CMD_UNITS_INFO))
			mUnitsInfoFile = new File(cmdLine.getOptionValue(CMD_UNITS_INFO));
		else
			mUnitsInfoFile = null;
		if(cmdLine.hasOption(CMD_LOG))
		{
			mLogFile = new File(cmdLine.getOptionValue(CMD_LOG));
			if(cmdLine.hasOption(CMD_LOG_PATTERN))
				mLogPattern = cmdLine.getOptionValue(CMD_LOG_PATTERN);
			else
				mLogPattern = DEFAULT_LOG_PATTERN;
			if(cmdLine.hasOption(CMD_LOG_LEVEL))
				mLogLevel = cmdLine.getOptionValue(CMD_LOG_LEVEL);
			else
				mLogLevel = DEFAULT_LOG_LEVEL;
		}
		else
			mLogFile = null;
		return true;
	}
}
