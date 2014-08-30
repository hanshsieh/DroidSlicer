package org.droidslicer.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

import javax.swing.JFrame;

import org.apache.commons.io.FileUtils;
import org.droidslicer.DroidSlicer;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.graph.BehaviorGraphComponent;
import org.droidslicer.graph.VisualBehaviorGraph;
import org.droidslicer.graph.entity.DataDependencyRelation;
import org.droidslicer.graph.entity.RelationEntity;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.common.base.Predicate;
import com.ibm.wala.analysis.reflection.InstanceKeyWithNode;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.HeapStatement.HeapParamCaller;
import com.ibm.wala.ipa.slicer.HeapStatement.HeapReturnCaller;
import com.ibm.wala.ipa.slicer.ISDG;
import com.ibm.wala.ipa.slicer.NormalReturnCallee;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.viz.DotUtil;
import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.util.mxMorphing;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;

public class Utils
{
	public static final int PATH_PAT_REP_TO_AST = 0x1;
	
	// The value number of the first argument of a method
	public static final int FIRST_ARG_VAL_NUM = 1;
	private final static Logger mLogger = LoggerFactory.getLogger(Utils.class);
	public final static String LOGGING_CONFIG_FILE = "config/logback.xml"; 
	private static Set<File> mFilesRm = new HashSet<File>();
	static
	{
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				synchronized(mFilesRm)
				{
					for(File file : mFilesRm)
					{
						try
						{
							if(file.isDirectory())
								FileUtils.deleteDirectory(file);
							else
							{
								boolean deleted = file.delete();
								if(!deleted)
									mLogger.error("Fail to delete file {}", file.getAbsolutePath());
							}
						}
						catch(Exception ex)
						{
							mLogger.error("Exception occurred: ", ex);
						}
					}
				}
			}
		});
	}
	private Utils()
	{}
	/*
	public static <V, E> void visualizeGraph(edu.uci.ics.jung.graph.Graph<V, E> graph)
	{
		HierarchicalLayout<V, E> layout = new HierarchicalLayout<V, E>(graph);
		layout.setBottomUp(false);
		layout.setSize(new Dimension(800, 600));
		VisualizationViewer<V, E> viewer = new VisualizationViewer<V, E>(layout);
		DefaultModalGraphMouse<V, E> graphMouse = new DefaultModalGraphMouse<V, E>();
		viewer.setGraphMouse(graphMouse);
        graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
    	JFrame frame = new JFrame();
    	frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    	frame.getContentPane().add(viewer);
    	frame.setSize(800, 600);
    	frame.setVisible(true);
	}*/
	public static void configureLogging()
			throws IOException
	{
		// See http://logback.qos.ch/manual/configuration.html#joranDirectly
		URL url = Utils.class.getProtectionDomain().getCodeSource().getLocation();
		if(url == null)
			throw new IOException("Fail to find the code URL");
		File configFile;
		try
		{
			configFile = new File(url.toURI());
		}
		catch(URISyntaxException ex)
		{
			throw new IOException(ex);
		}
		configFile = new File(configFile.getParent(), LOGGING_CONFIG_FILE);
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
	    try
	    {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);

			// Call context.reset() to clear any previous configuration, e.g. default 
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset(); 
			configurator.doConfigure(configFile);
	    }
	    catch(JoranException je)
	    {
	    	throw new IOException(je);
	    }
	    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}
	public static <V, E> void visualizeGraph(org.jgrapht.ListenableGraph<V, E> graph)
	{
		final mxGraph visualGraph = new JGraphXAdapter<V, E>( graph );
		
		visualGraph.setCellsEditable(false);
		visualGraph.setCellsResizable(false);
		visualGraph.setAllowDanglingEdges(false);
		visualGraph.setEdgeLabelsMovable(false);
		visualGraph.setCellsCloneable(false);
    	visualGraph.setCellsBendable(false);
    	
    	JFrame frame = new JFrame();
    	frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    	mxGraphComponent graphComp = new mxGraphComponent(visualGraph);
    	frame.getContentPane().add(graphComp);
    	frame.setSize(800, 600);
    	mxFastOrganicLayout layout = new mxFastOrganicLayout(visualGraph);
		visualGraph.getModel().beginUpdate();
		try
		{
			layout.execute(visualGraph.getDefaultParent());
		}
		finally
		{
			mxMorphing morph = new mxMorphing(graphComp, 20, 1.2, 20);

            morph.addListener(mxEvent.DONE, new mxIEventListener() {

                @Override
                public void invoke(Object arg0, mxEventObject arg1) {
                	visualGraph.getModel().endUpdate();
                    // fitViewport();
                }

            });

            morph.startAnimation();
            //visualGraph.getModel().endUpdate();
		}
    	frame.setVisible(true);
	}
	public static void visualizeGraph(VisualBehaviorGraph graph)
	{
		JFrame frame = new JFrame();
    	frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    	BehaviorGraphComponent comp = new BehaviorGraphComponent(graph);
    	comp.doLayout(false);
    	frame.getContentPane().add(comp);
    	frame.setSize(800, 600);
    	frame.setVisible(true);
	}
	/*public static <T> edu.uci.ics.jung.graph.Graph <T, BasicEdge> convertWalaGraph2JUNG(com.ibm.wala.util.graph.Graph<T> graph)
	{
		SparseGraph<T, BasicEdge> result = new SparseGraph<T, BasicEdge>();
		Iterator<T> nodesItr = graph.iterator();
		while(nodesItr.hasNext())
		{
			T node = nodesItr.next();
			result.addVertex(node);
			Iterator<T> succItr = graph.getSuccNodes(node);
			result.addVertex(node);
			while(succItr.hasNext())
			{
				T succNode = succItr.next();
				result.addVertex(succNode);
				result.addEdge(new BasicEdge(), node, succNode, EdgeType.DIRECTED);
			}
		}
		return result;
	}*/
	
	public static <T> org.jgrapht.ListenableGraph<T, RelationEntity> convertWalaGraph2JGraphT(com.ibm.wala.util.graph.Graph<T> graph)
	{
		org.jgrapht.ListenableGraph<T, RelationEntity> result = new ListenableDirectedGraph<T, RelationEntity>(RelationEntity.class);
		Iterator<T> nodesItr = graph.iterator();
		while(nodesItr.hasNext())
		{
			T node = nodesItr.next();
			result.addVertex(node);
			Iterator<T> succItr = graph.getSuccNodes(node);
			result.addVertex(node);
			while(succItr.hasNext())
			{
				T succNode = succItr.next();
				result.addVertex(succNode);
				result.addEdge(node, succNode, new DataDependencyRelation());
			}
		}
		return result;
	}
	public static String deploymentMethodString(MethodReference methodRef)
	{
		StringBuilder builder = new StringBuilder();
		builder.append(StringStuff.jvmToBinaryName(methodRef.getReturnType().getName().toString()));
		builder.append(' ');
		builder.append(methodRef.getName().toString());
		builder.append('(');
		int nParam = methodRef.getNumberOfParameters();
		for(int i = 0; i < nParam; ++i)
		{
			if(i > 0)
				builder.append(", ");
			TypeReference paramType = methodRef.getParameterType(i);
			builder.append(StringStuff.jvmToBinaryName(paramType.getName().toString()));
		}
		builder.append(')');
		return builder.toString();
	}
	/**
	 * The types should be represented in deployment format. E.g. java.lang.String.
	 * It will return something like "(ILjava/lang/String;)Ljava/lang/String"
	 * @param retType
	 * @param argTypes
	 * @return the canonical descriptor string
	 */
	public static String canonicalDescriptorString(String retType, String... argTypes)
	{
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		for(int i = 0; i < argTypes.length; ++i)
		{
			builder.append(StringStuff.deployment2CanonicalDescriptorTypeString(argTypes[i]));
		}
		builder.append(')');
		builder.append(StringStuff.deployment2CanonicalDescriptorTypeString(retType));
		return builder.toString();
	}
	public static void registerFileOrDirDelete(File dir)
	{
		synchronized(mFilesRm)
		{
			mFilesRm.add(dir);
		}
	}
	public static void unregisterFileOrDirDelete(File dir)
	{
		synchronized(mFilesRm)
		{
			mFilesRm.remove(dir);
		}
	}
	public static int getInstructionIdx(Statement stm)
	{
		switch(stm.getKind())
		{
		case HEAP_PARAM_CALLER:
			{
				HeapParamCaller caller = (HeapParamCaller)stm;
				return caller.getCallIndex();
			}
		case PARAM_CALLER:
			{
				ParamCaller caller = (ParamCaller)stm;
				return caller.getInstructionIndex();
			}
		case NORMAL:
			{
				NormalStatement normalStm = (NormalStatement)stm;
				return normalStm.getInstructionIndex();
			}
		case HEAP_RET_CALLER:
			{
				HeapReturnCaller heapRetStm = (HeapReturnCaller)stm;
				return heapRetStm.getCallIndex();
			}
		default:
			return -1;
		}
	}
	public static <T> void writeDotFile(Graph<T> graph, String path)
	{
		File dotFile = new File(path);
		try
		{
			DotUtil.writeDotFile(graph, null, "Graph", dotFile.getAbsolutePath());
			mLogger.info("Dot file has been stored at {}", dotFile.getAbsolutePath());
		}
		catch(Exception ex)
		{
			mLogger.error("Exception occurred: ", ex);
		}
	}
	public static Graph<CGNode> pruneCallGraphForApp(final CallGraph cg)
	{
	    return GraphSlicer.prune(cg, new com.ibm.wala.util.Predicate<CGNode>()
    		{
				@Override
				public boolean test(CGNode node)
				{
					// If the call node is a method in the APK
					if (node.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application))
					{
						return true;
					}
					else
					{
						
						// For each predecessor node of this node
						Iterator<CGNode> n = cg.getPredNodes(node);
						while (n.hasNext()) 
						{
							// If this node (non-application node) is invoked
							// by an application node
							CGNode preNode = n.next();
							if (preNode.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application))
								return true;
						}
						
						// For each successor node of this node
						n = cg.getSuccNodes(node);
						while (n.hasNext()) {
							// If this node (non-application node) invokes 
							// an application node
							CGNode nextNode = n.next();
							if (nextNode.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application))
								return true;
						}
						// This node is a primordial node with no direct successors or predecessors
						// being application node
						return false;
					}
				}
    		});
	}
	public static boolean equalIgnoreLoader(MethodReference ref1, MethodReference ref2)
	{
		return ref1.getDeclaringClass().getName().equals(ref2.getDeclaringClass().getName()) &&
				ref1.getDescriptor().equals(ref2.getDescriptor());
	}
	public static boolean equalIgnoreLoader(TypeReference ref1, TypeReference ref2)
	{
		return ref1.getName().equals(ref2.getName());
	}
	public static boolean isDemandPointsToSupported(InstanceKey instance)
	{
		return instance instanceof InstanceKeyWithNode;
	}
	
	public static boolean isReturnThis(CGNode node)
	{
		IMethod method = node.getMethod();
		if(method.isStatic() || method.getNumberOfParameters() <= 0)
			return false;
		TypeReference retTypeRef = method.getReturnType();
		if(!retTypeRef.isReferenceType())
			return false;
		IR ir = node.getIR();
		int thisValNum = ir.getParameterValueNumbers()[0];
		BasicBlock exitBlock = ir.getExitBlock();
		SSACFG cfg = ir.getControlFlowGraph();
		SSAInstruction[] insts = ir.getInstructions();
		Iterator<ISSABasicBlock> itr = cfg.getPredNodes(exitBlock);
		while(itr.hasNext())
		{
			ISSABasicBlock block = itr.next();
			int lastInst = block.getLastInstructionIndex();
			for(int instIdx = block.getFirstInstructionIndex(); 
					instIdx <= lastInst;
					++instIdx)
			{
				SSAInstruction inst = insts[instIdx];
				if(inst instanceof SSAReturnInstruction)
				{
					SSAReturnInstruction retInst = (SSAReturnInstruction)inst;
					if(retInst.returnsVoid() || retInst.getResult() != thisValNum)
						return false;
				}
			}
		}
		return true;
	}
	
	
	/**
	 * Whether the specified call node will directly return 'this'.
	 * It uses simple heuristic to decide whether the method of the call node 
	 * will return 'this'.
	 * It can only work correctly for the simple case, e.g. "return this".
	 * Some methods, like StringBuilder#append(Object) have some internal logic that 
	 * prevent us using these simple heuristic to decide whether the method will return
	 * 'this'. Thus, we also do some special check here.
	 * TODO Maybe we can do better.
	 * 
	 * @param sdg
	 * @param node
	 * @return whether the call graph node will return 'this' 
	 */
	public static boolean isReturnThis(ISDG sdg, CGNode node)
	{
		IMethod method = node.getMethod();
		if(method.isStatic() || 
			method.getNumberOfParameters() <= 0)
		{
			return false;
		}
		TypeReference retType = method.getReturnType();
		TypeReference classRef = method.getDeclaringClass().getReference();
		if(!retType.getName().equals(classRef.getName()))
			return false;
		
		// Some special handling
		if(classRef.equals(TypeReference.JavaLangStringBuilder) ||
			classRef.equals(TypeId.ANDROID_URI_BUILDER) ||
			classRef.equals(TypeReference.JavaLangStringBuffer))
		{
			return true;
		}
		Iterator<Statement> preItr = sdg.getPredNodes(new NormalReturnCallee(node));
		if(!preItr.hasNext())
			return false;
		Statement preStm = preItr.next();
		if(!preStm.getKind().equals(Statement.Kind.NORMAL))
			return false;
		NormalStatement normalStm = (NormalStatement)preStm;
		SSAInstruction preInst = normalStm.getInstruction();
		if(!(preInst instanceof SSAReturnInstruction))
			return false;
		SSAReturnInstruction retInst = (SSAReturnInstruction)preInst;
		int thisValNum = node.getIR().getParameterValueNumbers()[0];
		if(retInst.getNumberOfUses() <= 0 || thisValNum != retInst.getUse(0))
			return false;
		return true;
	}
	public static MethodReference parseMethodSignature(Language lang, TypeReference clazzType, String sigStr)
	{
		sigStr = sigStr.trim();
		int end = sigStr.indexOf(' ');
		if(end < 0)
			throw new IllegalArgumentException("Invalid descriptor string");
		String retTypeStr = sigStr.substring(0, end);
		sigStr = sigStr.substring(end + 1).trim();
		
		end = sigStr.indexOf('(');
		if(end < 0)
			throw new IllegalArgumentException("Invalid descriptor string");
		String methodName = sigStr.substring(0, end);
		if(sigStr.isEmpty() || sigStr.charAt(sigStr.length() - 1) != ')')
			throw new IllegalArgumentException("Invalid descriptor string");
		sigStr = sigStr.substring(end + 1, sigStr.length() - 1).trim();
		String[] paramList = sigStr.isEmpty() ? new String[0] : sigStr.split(",");
		for(int i = 0; i < paramList.length; ++i)
			paramList[i] = paramList[i].trim();
		String descriptorStr = Utils.canonicalDescriptorString(retTypeStr, paramList);
		Descriptor descriptor = Descriptor.findOrCreateUTF8(lang, descriptorStr);
		MethodReference methodRef = MethodReference.findOrCreate(clazzType, Atom.findOrCreateUnicodeAtom(methodName), descriptor);
		return methodRef;
	}
	public static void releaseMemory(AndroidAnalysisContext analysisCtx)
	{
		analysisCtx.wipeCache();
		IClassHierarchy cha = analysisCtx.getClassHierarchy();
		for (IClass klass : cha)
		{
			if (klass instanceof ShrikeClass)
			{
				ShrikeClass c = (ShrikeClass) klass;
				c.clearSoftCaches();
			}
		}
	}
	public static Set<IClass> computeAllSubclassesOrImplementors(IClassHierarchy cha, IClass clazz, Predicate<IClass> allowed)
	{
		Set<IClass> result = new LinkedHashSet<IClass>();
		if(!allowed.apply(clazz))
			return Collections.emptySet();
		Queue<IClass> que = new ArrayDeque<IClass>(3);
		que.add(clazz);
		result.add(clazz);
		while(!que.isEmpty())
		{
			IClass nowClass = que.poll();
			Collection<IClass> subClasses;
			if(nowClass.isInterface())
			{
				subClasses = cha.getImplementors(nowClass.getReference());
			}
			else
			{
				subClasses = cha.getImmediateSubclasses(nowClass);
			}
			for(IClass subClazz : subClasses)
			{
				if(!allowed.apply(subClazz))
					continue;
				if(result.add(subClazz))
				{
					que.add(subClazz);
				}
			}
		}
		return result;
	}
}
