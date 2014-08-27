package org.droidslicer.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import javax.xml.parsers.ParserConfigurationException;

import org.droidslicer.android.AndroidAPKFormatException;
import org.droidslicer.android.manifest.AndroidManifest;
import org.droidslicer.android.manifest.AndroidManifestSyntaxException;
import org.droidslicer.android.model.AndroidModelMethodTargetSelector;
import org.droidslicer.android.model.AppModelBuilder;
import org.droidslicer.android.model.AppModelClass;
import org.droidslicer.android.model.FakeContextImpl;
import org.droidslicer.config.AbstractAnalysisConfig;
import org.droidslicer.config.AbstractGeneralConfig;
import org.droidslicer.config.DefaultGeneralConfig;
import org.droidslicer.config.FileAnalysisConfig;
import org.droidslicer.config.XMLBypassSummaryReader;
import org.droidslicer.graph.entity.resolver.InvocationEntityResolver;
import org.droidslicer.util.DataLoader;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.Utils;
import org.droidslicer.value.solver.ConcreteValueSolver;
import org.droidslicer.value.solver.SliceValueSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.demandpa.flowgraph.AbstractDemandFlowGraph;
import com.ibm.wala.demandpa.flowgraph.AbstractFlowGraph;
import com.ibm.wala.demandpa.flowgraph.DemandPointerFlowGraph;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyClassTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyMethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.HeapExclusions;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.summaries.BypassClassTargetSelector;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;

public class AndroidAnalysisContext
{
	private final static boolean DEFAULT_METHOD_IGNORE = true;
	private static final Logger mLogger = LoggerFactory.getLogger(AndroidAnalysisContext.class);
	protected static class IgnoreMethodPredicate implements Predicate<MethodReference>
	{
		private final Map<Atom, Boolean> mIgnoredPackages;
		private final Map<TypeReference, Boolean> mIgnoredClasses;
		private final Set<MethodReference> mIgnoredMethods;
		public IgnoreMethodPredicate(Map<Atom, Boolean> ignoredPackages, Map<TypeReference, Boolean> ignoredClasses, Set<MethodReference> ignoredMethods)
		{
			mIgnoredPackages = ignoredPackages;
			mIgnoredClasses = ignoredClasses;
			mIgnoredMethods = ignoredMethods;
		}
		@Override
		public boolean apply(MethodReference method)
		{
			TypeReference typeRef = method.getDeclaringClass();
			if(!typeRef.getClassLoader().equals(ClassLoaderReference.Primordial))
				return false;
			if(mIgnoredMethods.contains(method))
				return true;
			Boolean ignore;
			ignore = mIgnoredClasses.get(typeRef);
			if(ignore != null)
				return ignore;
			ignore = mIgnoredPackages.get(typeRef.getName().getPackage());
			if(ignore != null)
				return ignore;
						
			// Delegate to default decision
			return DEFAULT_METHOD_IGNORE;
		}
		
	}
	private final AndroidAppInfo mAppInfo;
	private final AbstractAnalysisConfig mAnalysisConfig;
	private final FakeContextImpl mFakeContextClass;
	private final AppModelClass mAppModelClass;
	private final AnalysisOptions mAnalysisOpts;
	private final CallGraph mCg;
	private final PointerAnalysis mPa;
	private SoftReference<AbstractDemandFlowGraph> mFlowGraph = null;
	private SoftReference<SDG> mSdg = null;
	private final ConcreteValueSolver mValSolver;
	private final HeapExclusions mHeapExclusions;
	private final Map<PointerKey, Set<Statement>> mInvStmMods;
	private final Map<PointerKey, Set<Statement>> mInvStmRefs;
	private AndroidAnalysisContext(
			AnalysisOptions analysisOpts,
			AbstractAnalysisConfig analysisConfig, 
			AndroidAppInfo appInfo,
			FakeContextImpl fakeContextClass, 
			AppModelClass appModelClass,
			CallGraph cg,
			PointerAnalysis pa)
	{
		mAnalysisOpts = analysisOpts;
		mAppInfo = appInfo;
		mAnalysisConfig = analysisConfig;
		mFakeContextClass = fakeContextClass;
		mAppModelClass = appModelClass;
		mCg = cg;
		mPa = pa;
		mSdg = null;
		mFlowGraph = null;
		mValSolver = new SliceValueSolver(this);
		mHeapExclusions = new AndroidHeapExclusions(appModelClass);
		{
			ModRef modRef = ModRef.make();
			ExtendedHeapModel heapModel = new DelegatingExtendedHeapModel(pa.getHeapModel());
			mInvStmMods = new HashMap<PointerKey, Set<Statement>>();
			mInvStmRefs = new HashMap<PointerKey, Set<Statement>>();
			for (CGNode node : mCg)
			{
				IR ir = node.getIR();
				if (ir == null)
					continue;
				SSAInstruction[] insts = ir.getInstructions();
				for (int instIdx = 0; instIdx < insts.length; ++instIdx)
				{
					SSAInstruction inst = ir.getInstructions()[instIdx];
					if (inst == null)
						continue;
					if(inst instanceof SSANewInstruction)
					{
						SSANewInstruction newInst = (SSANewInstruction)inst;
						IClass type = getClassHierarchy().lookupClass(newInst.getConcreteType());
						if(type == null)
						{
							// If the allocated type isn't found in class hierarchy, it will ModRef.getMod(...) will 
							// fail with AssertionError
							continue;
						}
					}
					Set<PointerKey> mods = modRef.getMod(node, heapModel, pa, inst, mHeapExclusions, true);
					Set<PointerKey> refs = modRef.getRef(node, heapModel, pa, inst, mHeapExclusions);
					NormalStatement normal = new NormalStatement(node, instIdx);
					for(PointerKey mod : mods)
					{
						Set<Statement> stms = mInvStmMods.get(mod);
						if(stms == null)
						{
							stms = new LinkedHashSet<Statement>();
							mInvStmMods.put(mod, stms);
						}
						stms.add(normal);
					}
					for(PointerKey ref : refs)
					{
						Set<Statement> stms = mInvStmRefs.get(ref);
						if(stms == null)
						{
							stms = new LinkedHashSet<Statement>();
							mInvStmRefs.put(ref, stms);
						}
						stms.add(normal);
					}					
				}
			}
		}
	}
	public void wipeCache()
	{
		/*WeakReference<SDG> ref = null;
		if(mLogger.isDebugEnabled())
		{
			SDG sdg = mSdg == null ? null : mSdg.get();
			if(sdg != null)
			{
				ref = new WeakReference<SDG>(sdg);
			}
		}*/
		mSdg = null;
		mFlowGraph = null;
		/*
		if(mLogger.isDebugEnabled())
		{
			mLogger.debug("DEBUG, release it after test");
			Runtime runtime = Runtime.getRuntime();
			runtime.gc();
			if(ref != null && ref.get() != null)
			{
				throw new RuntimeException("SDG isn't released");
			}
		}*/
	}
	public Set<Statement> getWritesToStaticField(StaticFieldKey pointer)
	{
		Set<Statement> result = mInvStmMods.get(pointer);
		if(result == null)
			return Collections.emptySet();
		else
			return result;
	}
	public Set<Statement> getWritesToInstanceField(PointerKey pointer, IField field)
	{
		pointer = AbstractFlowGraph.convertPointerKeyToHeapModel(pointer, mPa.getHeapModel());
		Set<Statement> result = new LinkedHashSet<Statement>();
		for(InstanceKey instance : mPa.getPointsToSet(pointer))
		{
			Set<Statement> stms = mInvStmMods.get(mPa.getHeapModel().getPointerKeyForInstanceField(instance, field));
			if(stms != null)
				result.addAll(stms);
		}
		return result;
	}
	public Set<Statement> getReadsToStaticField(StaticFieldKey pointer)
	{
		Set<Statement> result = mInvStmRefs.get(pointer);
		if(result == null)
			return Collections.emptySet();
		else
			return result;
	}
	public Set<Statement> getReadsToInstanceField(PointerKey pointer, IField field)
	{
		pointer = AbstractFlowGraph.convertPointerKeyToHeapModel(pointer, mPa.getHeapModel());
		Set<Statement> result = new LinkedHashSet<Statement>();
		for(InstanceKey instance : mPa.getPointsToSet(pointer))
		{
			Set<Statement> stms = mInvStmRefs.get(mPa.getHeapModel().getPointerKeyForInstanceField(instance, field));
			if(stms != null)
				result.addAll(stms);
		}
		return result;
	}
	protected SDG rebuildSDG()
	{
		mSdg = null;
		mFlowGraph = null;
		SDG sdg = makeSDG(mCg, mPa, mAppModelClass);
		mSdg = new SoftReference<SDG>(sdg);
		
		// Flow graph is dependent on SDG; thus, if SDG is rebuilt, 
		// flow graph must also be rebuilt
		mLogger.debug("SDG is rebuilt");
		return sdg;
	}
	protected AbstractDemandFlowGraph rebuildFlowGraph()
	{
		// Release the old flow graph first
		mFlowGraph = null;
		SDG sdg = getSDG();
		assert sdg != null;
		AbstractDemandFlowGraph flowGraph = makeFlowGraph(sdg);
		mFlowGraph = new SoftReference<AbstractDemandFlowGraph>(flowGraph);
		mLogger.debug("Flow graph is rebuilt");
		return flowGraph;
	}
	public PointerAnalysis getPointerAnalysis()
	{
		return mPa;
	}
	public ConcreteValueSolver getValueSolver()
	{
		return mValSolver;
	}
	public IClassHierarchy getClassHierarchy()
	{
		return mCg.getClassHierarchy();
	}
	public AnalysisOptions getAnalysisOptions()
	{
		return mAnalysisOpts;
	}
	public CallGraph getCallGraph()
	{
		return mCg;
	}
	public AnalysisScope getAnalysisScope()
	{
		return mCg.getClassHierarchy().getScope();
	}
	public SDG getSDG()
	{
		SDG sdg = mSdg == null ? null : mSdg.get();
		if(sdg == null)
			sdg = rebuildSDG();
		return sdg;
	}
	public AbstractDemandFlowGraph getFlowGraph()
	{
		AbstractDemandFlowGraph flowGraph = mFlowGraph == null ? null : mFlowGraph.get();
		if(flowGraph == null)
			flowGraph = rebuildFlowGraph();
		return flowGraph;
	}
	public AndroidAppInfo getAppInfo()
	{
		return mAppInfo;
	}
	public FakeContextImpl getFakeContextClass()
	{
		return mFakeContextClass;
	}
	public AppModelClass getAppModelClass()
	{
		return mAppModelClass;
	}
	public AbstractAnalysisConfig getAnalysisConfig()
	{
		return mAnalysisConfig;
	}
	protected static XMLBypassSummaryReader makeBypassSummary(DataLoader dataLoader, AnalysisScope analysisScope)
		throws IOException
	{
		// Load method summary
		XMLBypassSummaryReader methodSummary;
		try
		{
			FileInputStream methodSummaryInput = null;
			try
			{
				methodSummaryInput = new FileInputStream(dataLoader.getAndroidMethodSummary());
				methodSummary = new XMLBypassSummaryReader(
						methodSummaryInput, analysisScope);
			}
			finally
			{
				if(methodSummaryInput != null)
					methodSummaryInput.close();
			}
		}
		catch(SAXException ex)
		{
			throw new IOException(ex);
		}
		catch(ParserConfigurationException ex)
		{
			throw new IOException(ex);
		}
		return methodSummary;
	}
	protected static AnalysisScope makeAnalysisScope(AndroidAppInfo appInfo, DataLoader dataLoader)
		throws IOException
	{
		JarFile classesJar = new JarFile(appInfo.getClassesJar());
		Module appModule = new JarFileModule(classesJar);
		AnalysisScope analysisScope = AnalysisScope.createJavaAnalysisScope();
		dataLoader.loadSystemLibraries(analysisScope);
		analysisScope.addToScope(ClassLoaderReference.Primordial, new JarFile(dataLoader.getAndroidMethodSummaryHelper()));
		analysisScope.addToScope(analysisScope.getApplicationLoader(), appModule);
		//analysisScope.setExclusions(FileOfClasses.createFileOfClasses(dataLoader.getAndroidExlucsions()));
		return analysisScope;
	}
	protected static IClassHierarchy makeClassHierarchy(AnalysisScope analysisScope, XMLBypassSummaryReader methodSummary)
		throws ClassHierarchyException
	{
		// Setup class loader factory
		DelegatingClassLoaderFactory classLoaderFactory = new DelegatingClassLoaderFactory(analysisScope.getExclusions());
		for(FieldSpec fieldSpec : methodSummary.getFields())
		{
			classLoaderFactory.addExtraField(fieldSpec);
		}
		ClassHierarchy cha = ClassHierarchy.make(analysisScope, classLoaderFactory);
		if(mLogger.isDebugEnabled())
		{
			for(Iterator<Warning> itr = Warnings.iterator(); itr.hasNext();)
			{
				Warning warn = itr.next();
				mLogger.debug("{}", warn);
			}
		}
		Warnings.clear();
		return cha;
	}
	protected static AnalysisOptions makeAnalysisOptions()
	{
		AnalysisOptions analysisOpts = new AnalysisOptions();
		analysisOpts.setReflectionOptions(ReflectionOptions.FULL);
		return analysisOpts;
	}
	protected static Set<MethodReference> makeBypassMethods(IClassHierarchy cha, AbstractAnalysisConfig analysisConfig)
	{
		Set<MethodReference> bypassMethods = new HashSet<MethodReference>();
		Iterator<InvocationEntityResolver> invokeResolverItr = analysisConfig.invocationResolversIterator();
		while(invokeResolverItr.hasNext())
		{
			InvocationEntityResolver invokeResolver = invokeResolverItr.next();
			MethodReference methodRef = invokeResolver.getMethodReference();
			IClass clazz = cha.lookupClass(methodRef.getDeclaringClass());
			if(clazz == null || clazz.getMethod(methodRef.getSelector()) == null)
			{
				// If the method isn't found in class hierarchy, IClassHierarchy.getPossibleTarget(MethodReference) 
				// will throw AssertionError; thus, we check if the method exists in the class hierarchy first.
				mLogger.debug("Method {} of invocation resolver isn't found in class hierarchy. Ignore it", methodRef);
				continue;
			}
			Collection<IMethod> targets = cha.getPossibleTargets(methodRef);
			for(IMethod target : targets)
			{
				if(target.getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Primordial))
					bypassMethods.add(target.getReference());
			}
		}
		return bypassMethods;
	}
	protected static void setupMethodTargetSelector(
			AnalysisOptions analysisOpts,
			IClassHierarchy cha, 
			AbstractAnalysisConfig analysisConfig, 
			XMLBypassSummaryReader methodSummary,
			FakeContextImpl fakeContextClass,
			AppModelClass appModelClass)
	{
		Set<MethodReference> bypassMethods = makeBypassMethods(cha, analysisConfig);
		
		Map<TypeReference, Boolean> ignoredClasses = methodSummary.getIgnoredClasses();
		
		// Do not ignore the definition of our model classes
		ignoredClasses.put(fakeContextClass.getReference(), false);
		ignoredClasses.put(appModelClass.getReference(), false);
		IgnoreMethodPredicate ignoreMethodPred = 
				new IgnoreMethodPredicate(methodSummary.getIgnoredPackages(), ignoredClasses, bypassMethods);
		MethodTargetSelector methodSelector = new ClassHierarchyMethodTargetSelector(cha);				
		methodSelector = new AndroidModelMethodTargetSelector(
				methodSelector,
				methodSummary.getSummaries(), 
				appModelClass,
				ignoreMethodPred,
				analysisConfig,
				cha);
		analysisOpts.setSelector(methodSelector);
	}
	protected static void setupClassTargetSelector(
			AnalysisOptions analysisOpts,
			IClassHierarchy cha, 
			AnalysisScope analysisScope,
			XMLBypassSummaryReader methodSummary)
	{
		ClassTargetSelector classSelector = new ClassHierarchyClassTargetSelector(cha);
		classSelector = new BypassClassTargetSelector(
				classSelector, 
				methodSummary.getAllocatableClasses(), 
				cha,
				cha.getLoader(analysisScope.getLoader(AnalysisScope.SYNTHETIC)));

		analysisOpts.setSelector(classSelector);
	}
	protected static CallGraphBuilder buildCallGraphBuilder(
			AnalysisOptions analysisOpts,
			IClassHierarchy cha)
	{
		AnalysisCache cache = new AnalysisCache();
		SSAPropagationCallGraphBuilder cgBuilder = 
				new FixedZeroXCFABuilder(cha, analysisOpts, cache, null, null, ZeroXInstanceKeys.NONE);
		/*SSAPropagationCallGraphBuilder cgBuilder = 
				ZeroXCFABuilder.make(cha, analysisOpts, cache, null, null, ZeroXInstanceKeys.NONE);*/
		/*SSAPropagationCallGraphBuilder cgBuilder = 
				new FixedZeroXCFABuilder(cha, analysisOpts, cache, null, null, ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.SMUSH_THROWABLES);*/
		return cgBuilder;
	}
	protected static CallGraph makeCallGraph(AnalysisOptions analysisOpts, CallGraphBuilder cgBuilder, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Building call graph", 100);
			CallGraph cg;
			{
				try
				{
					mLogger.info("Building call graph");
					Stopwatch watch = Stopwatch.createStarted();
					cg = cgBuilder.makeCallGraph(analysisOpts, new SubProgressMonitor(monitor, 100));
					mLogger.info("Call graph generation finished. Elapsed time: {}", watch);
				}
				finally
				{
					monitor.setSubProgressMonitor(null);
				}
			}
	    	if(mLogger.isDebugEnabled())
	    	{
	    		Utils.writeDotFile(Utils.pruneCallGraphForApp(cg), "callgraph.dot");
	    		//Utils.writeDotFile(mCg, "android_test.dot");
	    	}
	    	return cg;
		}
		finally
		{
			monitor.done();
		}
	}
	protected SDG makeSDG(CallGraph cg, PointerAnalysis pa, final AppModelClass appModelClass)
	{
		
		// Notice that we shouldn't use NO_BASE_NO_EXCEPTIONS, since it will miss base pointer dependency for array access.
		// For example, if we ignore base-ptr dependency, then arrayload 1[29] won't depend on the definition point of '1'.
		//return new SDG(cg, pa, ModRef.make(), Slicer.DataDependenceOptions.NO_EXCEPTIONS, Slicer.ControlDependenceOptions.NONE, mHeapExclusions);
		return new SDG(cg, pa, ModRef.make(), Slicer.DataDependenceOptions.NO_HEAP_NO_EXCEPTIONS, Slicer.ControlDependenceOptions.NONE, mHeapExclusions);
	}
	protected AbstractDemandFlowGraph makeFlowGraph(SDG sdg)
	{
		CallGraph cg = sdg.getCallGraph();
		PointerAnalysis pa = sdg.getPointerAnalysis();
		HeapModel heapModel = pa.getHeapModel();
		ReversedPABasedMemoryAccessMap mem = new ReversedPABasedMemoryAccessMap(cg, pa, mInvStmMods, mInvStmRefs);
		DemandPointerFlowGraph flowGraph = new DemandPointerFlowGraph(cg, heapModel, mem, sdg.getClassHierarchy());
		return flowGraph;
	}
	public static AndroidAnalysisContext make(
			AbstractAnalysisConfig analysisConfig, 
			AndroidAppInfo appInfo, 
			DataLoader dataLoader, 
			ProgressMonitor monitor)
			throws IOException, ClassHierarchyException, CancelException
	{
		try
		{
			monitor.beginTask("Building analysis context", 1000);
			
			monitor.subTask("Loading analysis scope");
			AnalysisScope analysisScope = makeAnalysisScope(appInfo, dataLoader);
			monitor.worked(50);
			
			monitor.subTask("Loading bypass summary");
			XMLBypassSummaryReader methodSummary = makeBypassSummary(dataLoader, analysisScope);
			monitor.worked(10);
			
			monitor.subTask("Making class hierarchy");
			IClassHierarchy cha = makeClassHierarchy(analysisScope, methodSummary);
			analysisConfig.initialize(cha);
			monitor.worked(10);
			
			AnalysisOptions analysisOpts = makeAnalysisOptions();
	
			FakeContextImpl fakeContextClass;
			AppModelClass appModelClass;
	
			// Setup app model
			{
				AppModelBuilder modelBuilder = new AppModelBuilder(cha, analysisScope, analysisConfig, appInfo);
		    	modelBuilder.build();
		    	fakeContextClass = modelBuilder.getFakeContextClass();
		    	appModelClass = modelBuilder.getAppModelClass();
				Entrypoint modelEntrypoint = new DefaultEntrypoint(modelBuilder.getEntryMethod(), cha);
				ArrayList<Entrypoint> entrypoints = new ArrayList<Entrypoint>();
				entrypoints.add(modelEntrypoint);
				analysisOpts.setEntrypoints(entrypoints);
			}
			
			// Setup method target selector
			setupMethodTargetSelector(analysisOpts, cha, analysisConfig, methodSummary, fakeContextClass, appModelClass);
			
			// Setup class target selector
			setupClassTargetSelector(analysisOpts, cha, analysisScope, methodSummary);
			
			// Build basic call graph and pointer analysis
			
			CallGraphBuilder cgBuilder = buildCallGraphBuilder(analysisOpts, cha);
			CallGraph cg = makeCallGraph(analysisOpts, cgBuilder, new SubProgressMonitor(monitor, 800));		
			PointerAnalysis pa = cgBuilder.getPointerAnalysis();
			
			monitor.subTask("Building flow graph");
			monitor.worked(60);
			
			if(mLogger.isDebugEnabled())
			{
				for (Iterator<Warning> wi = Warnings.iterator(); wi.hasNext();) {
					Warning w = wi.next();
					mLogger.debug(w.getMsg());
				}
			}
			Warnings.clear();
			return new AndroidAnalysisContext(analysisOpts, analysisConfig, appInfo, fakeContextClass, appModelClass, cg, pa);
		}
		finally
		{
			monitor.done();
		}
	}
	public void close()
		throws IOException
	{
		AnalysisScope scope = getAnalysisScope();
		for(ClassLoaderReference classLoaderRef : scope.getLoaders())
		{
			for(Module module : scope.getModules(classLoaderRef))
			{
				if(module instanceof JarFileModule)
				{
					JarFileModule jarModule = (JarFileModule)module;
					JarFile jarFile = jarModule.getJarFile();
					try
					{
						jarFile.close();
					}
					catch(Exception ex)
					{}
				}
			}
		}
		try
		{
			mAppInfo.close();
		}
		catch(Exception ex)
		{}
		try
		{
			mAnalysisConfig.close();
		}
		catch(Exception ex)
		{}
	}
	@Override
	protected void finalize()
		throws Throwable
	{
		close();
	}
	public static AndroidAnalysisContext makeDefault(File appFile, ProgressMonitor monitor)
			throws IOException, CancelException
	{
		return makeDefault(appFile, Predicates.<String>alwaysTrue(), monitor);
	}
	public static AndroidAnalysisContext makeDefault(File appFile, Predicate<String> permsOfInterestPred, ProgressMonitor monitor)
		throws IOException, CancelException
	{
		try
		{
			monitor.beginTask("Initializing analysis context", 1000);
			
			monitor.subTask("Getting the list of supported Android SDK versions");;
			Collection<Integer> supportedVers = DataLoader.getSupportedAndroidAPIVers();
			if(mLogger.isInfoEnabled())
			{
				StringBuilder builder = new StringBuilder("Supported Android SDK versions: ");
				boolean first = true;
				for(Integer ver : supportedVers)
				{
					if(!first)
						builder.append(", ");
					else
						first = false;
					builder.append(ver);
				}
				if(first)
					throw new IOException("No appropriate SDK version available, please put at least one SDK in the config directory");
				mLogger.info(builder.toString());
			}
			monitor.worked(10);
			
			monitor.subTask("Parsing manifest and DEX code");
			AbstractGeneralConfig config = new DefaultGeneralConfig();
			mLogger.info("Analyzing Android app: {}", appFile.getAbsolutePath());
			AndroidAppInfo appInfo = AndroidAppInfo.createFromAPK(new FileInputStream(appFile), config.getTempDirectory());
			AndroidManifest manifest = appInfo.getManifest();
			int minSDKVersion = manifest.hasMinSDKVersion() ? manifest.getMinSDKVersion() : 1;
			int maxSDKVersion = manifest.hasMaxSDKVersion() ? manifest.getMaxSDKVersion() : -1;
			int targetSDKVersion = manifest.hasTargetSDKVersion() ? manifest.getTargetSDKVersion() : minSDKVersion;
			mLogger.info("Min. SDK version: {}", minSDKVersion);
			mLogger.info("Max. SDK version: {}", maxSDKVersion);
			mLogger.info("Target SDK version: {}", targetSDKVersion);
			int sdkVer = Integer.MAX_VALUE;
			int maxSdkVer = Integer.MIN_VALUE;
			for(int ver : supportedVers)
			{
				maxSdkVer = Math.max(maxSdkVer, ver);
				if(ver >= minSDKVersion)
					sdkVer = Math.min(sdkVer, ver);
			}
			if(sdkVer == Integer.MAX_VALUE)
			{
				sdkVer = maxSdkVer;
				mLogger.warn("No supported Android SDK version >= Min. SDK version, using {} for analysis", sdkVer);
			}
			else
				mLogger.info("Using SDK version {} for analysis", sdkVer);
			
			monitor.worked(20);
			
			DataLoader dataLoader = new DataLoader(sdkVer);
			AbstractAnalysisConfig analysisConfig;
			{
				Collection<InputStream> inputStreams = new ArrayList<InputStream>();
				try
				{
					FileInputStream listenersInput, apiPermsInput, providerPermsInput, intentPermsInput, entryPointsInput;
					{
						listenersInput = new FileInputStream(dataLoader.getListeners());
						inputStreams.add(listenersInput);
					}
					{
						apiPermsInput = new FileInputStream(dataLoader.getApiPermissions());
						inputStreams.add(apiPermsInput);
					}
					{
						intentPermsInput = new FileInputStream(dataLoader.getIntentPermissions());
						inputStreams.add(intentPermsInput);
					}
					{
						providerPermsInput = new FileInputStream(dataLoader.getProviderPermissions());
						inputStreams.add(providerPermsInput);
					}
					{
						entryPointsInput = new FileInputStream(dataLoader.getEntryPoints());
						inputStreams.add(entryPointsInput);
					}
					
					monitor.subTask("Parsing Android model config files");
					analysisConfig = 
							new FileAnalysisConfig(
									apiPermsInput, 
									intentPermsInput, 
									providerPermsInput, 
									listenersInput, 
									entryPointsInput, 
									permsOfInterestPred);
				}
				finally
				{
					for(InputStream input : inputStreams)
					{
						try
						{
							input.close();
						}
						catch(Exception ex)
						{}
					}
				}
			}
			monitor.worked(10);
			
			return AndroidAnalysisContext.make(analysisConfig, appInfo, dataLoader, new SubProgressMonitor(monitor, 960));
		}
		catch(ClassHierarchyException ex)
		{
			throw new IOException(ex);
		}
		catch(AndroidAPKFormatException ex)
		{
			throw new IOException(ex);
		}
		catch(AndroidManifestSyntaxException ex)
		{
			throw new IOException(ex);
		}
		finally
		{
			monitor.done();
		}
	}
}
