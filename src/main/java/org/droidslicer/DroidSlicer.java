package org.droidslicer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.analysis.BehaviorGraphBuilder;
import org.droidslicer.android.AndroidAPKFormatException;
import org.droidslicer.android.manifest.AndroidManifestSyntaxException;
import org.droidslicer.graph.BehaviorGraph;
import org.droidslicer.graph.BehaviorGraphComponent;
import org.droidslicer.graph.BehaviorSupergraph;
import org.droidslicer.graph.VisualBehaviorGraph;
import org.droidslicer.graph.entity.Entity;
import org.droidslicer.signature.BehaviorSignaturesTester;
import org.droidslicer.util.BehaviorGraphFocusWriter;
import org.droidslicer.util.GraphUtils;
import org.droidslicer.util.LevelRangeFilter;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CGrid;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.SingleCDockable;
import bibliothek.gui.dock.common.mode.ExtendedMode;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.common.base.Stopwatch;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.io.CommandLine;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraphSelectionModel;

public class DroidSlicer extends JFrame
{
	private static final long serialVersionUID = 650728529535967479L;
	private static final String ID_LOGGING_PANE = "Log";
	private static final String ID_GRAPH_PANE = "Behavior graph";
	private static final String ID_PROPS_PANE = "Properties";
	private static final Logger mLogger = LoggerFactory.getLogger(DroidSlicer.class);
	private static final String USAGE = "Usage: -app <apk>";
	private static final String APP_PATH = "app";
	protected final JMenuBar mMenuBar = new JMenuBar();
	protected final LoggingPane mLoggingPane = new LoggingPane();
	protected final GraphPane mGraphPane = new GraphPane();
	protected final PropertiesPane mPropertiesPane;
	private AndroidAnalysisContext mAnalysisCtx = null;
	private final JMenuItem mOpenApkMenuItem = new JMenuItem();
	private final CControl mDockableControl;
	public DroidSlicer()
		throws IOException
	{
		Utils.configureLogging();
		buildMenu();
		configureLoggingPane();
		configureGraphPane();
		mPropertiesPane = new PropertiesPane();
		mDockableControl = new CControl(this);
		SingleCDockable graphDockable = wrapInSingleDockable(ID_GRAPH_PANE, ID_GRAPH_PANE, mGraphPane);
		SingleCDockable loggerDockable = wrapInSingleDockable(ID_LOGGING_PANE, ID_LOGGING_PANE, mLoggingPane);
		SingleCDockable propsDockable = wrapInSingleDockable(ID_PROPS_PANE, ID_PROPS_PANE, mPropertiesPane);
		registerLogger();
		getContentPane().add(mDockableControl.getContentArea());
		CGrid grid = new CGrid( mDockableControl );
		grid.add( 0, 0, 800, 450, graphDockable);
		grid.add( 0, 500, 800, 150, loggerDockable);
		grid.add( 0, 500, 800, 150, propsDockable);
		loggerDockable.setExtendedMode(ExtendedMode.MINIMIZED);
		mDockableControl.getContentArea().deploy( grid );
	}
	private static SingleCDockable wrapInSingleDockable( String id, String title, JComponent comp )
	{
		DefaultSingleCDockable dockable = new DefaultSingleCDockable( id, title );
		dockable.setTitleText( title );
		dockable.setCloseable( false );
		dockable.getContentPane().add(comp);
		return dockable;
	}
	@Override
	protected void finalize()
		throws Throwable
	{
		dispose();
	}
	@Override
	public void dispose()
	{
		mDockableControl.destroy();
		unregisterLogger();
		changeAnalysisContext(null);
		super.dispose();
	}
	private void unregisterLogger()
	{
		ch.qos.logback.classic.Logger rootLogger = 
				(ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLogger.detachAppender(mLoggingPane.getAppender());
	}
	private void registerLogger()
	{
		ch.qos.logback.classic.Logger rootLogger = 
				(ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		Appender<ILoggingEvent> appender = mLoggingPane.getAppender();
		rootLogger.addAppender(appender);
	}
	protected void configureGraphPane()
	{
		//mGraphPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		//mGraphPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		mGraphPane.setLayout(new BorderLayout());
	}
	protected void configureLoggingPane()
	{
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		PatternLayout layout = new PatternLayout();
		layout.setPattern("%d{HH:mm:ss.SSS} [%-5level] %msg%n");
		layout.setContext(lc);
		layout.start();
		mLoggingPane.setLayout(layout);
		Appender<ILoggingEvent> appender = mLoggingPane.getAppender();
		LevelRangeFilter filter = new LevelRangeFilter();
		filter.setLevel(Level.INFO);
		filter.setOnMatch(FilterReply.ACCEPT);
		filter.setOnMismatch(FilterReply.DENY);
		filter.start();
		appender.addFilter(filter);
	}
	protected void buildMenu()
	{
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		mMenuBar.add(fileMenu);
		mOpenApkMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(new File("."));
				FileFilter fileFilter = new FileNameExtensionFilter("APK file", "apk");
				fileChooser.addChoosableFileFilter(fileFilter);
				fileChooser.setFileFilter(fileFilter);
				int state = fileChooser.showOpenDialog(DroidSlicer.this);
				if(state == JFileChooser.APPROVE_OPTION)
				{
					final File file = fileChooser.getSelectedFile();
					new Thread()
					{
						@Override
						public void run()
						{
							try
							{
								openApk(file, new ProgressMonitor());
							}
							catch(IOException ex)
							{
								mLogger.error("Exception occurred: ", ex);
								JOptionPane.showMessageDialog(DroidSlicer.this,
									"Fail to open " + file.getAbsolutePath() + ":\n" + ex.getMessage(),
									"Open file error",
									JOptionPane.ERROR_MESSAGE);
							}
							catch(AndroidAPKFormatException ex)
							{
								mLogger.error("Exception occurred: ", ex);
								JOptionPane.showMessageDialog(DroidSlicer.this,
										"Invalid APK: " + ex.getMessage(),
										"Open file error",
										JOptionPane.ERROR_MESSAGE);
							}
							catch(AndroidManifestSyntaxException ex)
							{
								mLogger.error("Exception occurred: ", ex);
								JOptionPane.showMessageDialog(DroidSlicer.this,
										"Invalid Android manifest: " + ex.getMessage(),
										"Open file error",
										JOptionPane.ERROR_MESSAGE);
							}
							catch(ClassHierarchyException ex)
							{
								mLogger.error("Exception occurred: ", ex);
								JOptionPane.showMessageDialog(DroidSlicer.this,
										"Fail to parse the class hierarchy: " + ex.getMessage(),
										"Open file error",
										JOptionPane.ERROR_MESSAGE);
							}
							catch(Exception ex)
							{
								mLogger.error("Exception occurred: ", ex);
								JOptionPane.showMessageDialog(DroidSlicer.this,
										"Fail to parse the class hierarchy: " + ex.getMessage(),
										"Open file error",
										JOptionPane.ERROR_MESSAGE);
							}
							catch(Error ex)
							{
								mLogger.error("Fatal error occurred: ", ex);
								JOptionPane.showMessageDialog(DroidSlicer.this,
										"Fatal error: " + ex.getMessage(),
										"Open file error",
										JOptionPane.ERROR_MESSAGE);
							}
						}
					}.start();
				}
			}
		});
		mOpenApkMenuItem.setText("Open an APK...");
		fileMenu.add(mOpenApkMenuItem);
		setJMenuBar(mMenuBar);
	}
	public void openApk(File appFile, ProgressMonitor monitor)
		throws IOException, AndroidAPKFormatException, AndroidManifestSyntaxException, ClassHierarchyException, CancelException
	{
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					mOpenApkMenuItem.setEnabled(false);
					mGraphPane.setGraphComponent(null);
					mGraphPane.repaint();
				}
			});
		}
		catch(Exception ex)
		{
			mLogger.warn("{}", ex);
		}
		try
		{
			monitor.beginTask("Opening APK", 1000);
			changeAnalysisContext(null);
			changeAnalysisContext(AndroidAnalysisContext.makeDefault(appFile, new SubProgressMonitor(monitor, 100)));
			
			// Build the behavior graph
			BehaviorGraph graph = null;
			try
			{
				Stopwatch watch = Stopwatch.createStarted();
				graph = analyze(new SubProgressMonitor(monitor, 800));
				mLogger.info("Analysis finished, elapsed time: {}", watch);
			}
			catch(CancelException ex)
			{
				mLogger.info("Operation canceled: {}", ex.getMessage());
			}
			GraphUtils.writeDotFile(graph, new File("behavior_graph.dot"));
			final BehaviorGraphComponent comp = new BehaviorGraphComponent(new VisualBehaviorGraph(graph));
			mPropertiesPane.setBehaviorGraph(graph);
			comp.doLayout(false);
			mxGraphSelectionModel selModel = comp.getGraph().getSelectionModel();
			selModel.addListener(mxEvent.CHANGE, new mxIEventListener()
			{
				@Override
				public void invoke(Object sender, mxEventObject evt)
				{
					Entity entity = null;
					mxGraphSelectionModel selModel = (mxGraphSelectionModel)sender;
					Object[] cells = selModel.getCells();
					if(cells.length == 1)
					{
						Object cell = cells[0];
						BehaviorGraphComponent graphComp = mGraphPane.getGraphComponent();
						if(graphComp != null)
						{
							VisualBehaviorGraph graph = (VisualBehaviorGraph)graphComp.getGraph();
							entity = graph.getEntityForCell(cell);						
						}
					}
					mPropertiesPane.setClassHierarchy(mAnalysisCtx.getClassHierarchy());
					mPropertiesPane.setEntity(entity);
				}
			});
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					@Override
					public void run()
					{
						mGraphPane.setGraphComponent(comp);
						//mGraphPane.setViewportView(comp);
						//mGraphPane.getViewport().add(comp);
						mGraphPane.validate();
					}
				});
			}
			catch(Exception ex)
			{
				mLogger.warn("{}", ex);
			}
			{
				BehaviorGraphFocusWriter writer = new BehaviorGraphFocusWriter(graph);
				writer.write(new FileOutputStream("units_info.txt"));
			}
			{
				BehaviorSignaturesTester sigTester = new BehaviorSignaturesTester(graph, new File("config/sensitive_behavior_signatures.xml"));
				sigTester.test(new SubProgressMonitor(monitor, 100));
				BehaviorSupergraph supergraph = sigTester.getSupergraph();
				if(mLogger.isDebugEnabled())
					GraphUtils.writeDotFile(GraphUtils.convertICFG2JGraphT(supergraph), new File("behavior_supergraph.dot"));
			}			
			
			Utils.releaseMemory(mAnalysisCtx);
		}
		finally
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					@Override
					public void run()
					{
						mOpenApkMenuItem.setEnabled(true);
					}
				});
			}
			catch(Exception ex)
			{
				mLogger.warn("{}", ex);
			}
			monitor.done();
		}
	}
	private void changeAnalysisContext(AndroidAnalysisContext ctx)
	{
		if(mAnalysisCtx != null && mAnalysisCtx != ctx)
		{
			try
			{
				mAnalysisCtx.close();
			}
			catch(Exception ex)
			{
				mLogger.error("Exception occurred when releasing the analysis context: {}", ex);
			}
		}
		mAnalysisCtx = ctx;
	}
	private BehaviorGraph analyze(ProgressMonitor monitor)
			throws CancelException
	{
		try
		{
			monitor.beginTask("Analyzing Android code", 100);
	    	BehaviorGraphBuilder graphBuilder = 
	    			new BehaviorGraphBuilder(mAnalysisCtx);
	    	return graphBuilder.build(new SubProgressMonitor(monitor, 100));
		}
		finally
		{
			monitor.done();
		}
	}
	public static void main(String[] args)
		throws Exception
	{
		try
		{
			UIManager.setLookAndFeel(
			        UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception ex)
		{
			mLogger.warn("Fail to set the look-and-feel, use the default one");
		}
		final DroidSlicer frame;
		frame = new DroidSlicer();
		frame.pack();
		frame.setSize(800, 600);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setVisible(false);
		frame.setVisible(true);
		if(args.length > 0)
		{
			Properties props = CommandLine.parse(args);
			String appPath = props.getProperty(APP_PATH);
			if(appPath == null)
			{
				System.err.println(USAGE);
				System.exit(1);
				return;
			}
			try
			{
				frame.openApk(new File(appPath), new ProgressMonitor());
			}
			catch(Exception ex)
			{
				mLogger.error("Exception occurred when opening APK \"{}\": {}", appPath, ExceptionUtils.getStackTrace(ex));
			}
			catch(Error err)
			{
				mLogger.error("Error occurred when opening APK \"{}\": {}", appPath, ExceptionUtils.getStackTrace(err));
			}
		}
	}
}
