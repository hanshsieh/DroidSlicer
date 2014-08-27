package org.droidslicer.android.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.analysis.AndroidAppInfo;
import org.droidslicer.android.appSpec.AndroidActivitySpec;
import org.droidslicer.android.appSpec.AndroidApplicationSpec;
import org.droidslicer.android.appSpec.AndroidServiceSpec;
import org.droidslicer.android.appSpec.EntryCompSpec;
import org.droidslicer.android.appSpec.EntryMethodSpec;
import org.droidslicer.android.manifest.AndroidAppComponent;
import org.droidslicer.android.manifest.AndroidApplication;
import org.droidslicer.android.manifest.AndroidManifest;
import org.droidslicer.config.AbstractAnalysisConfig;
import org.droidslicer.util.InstructionException;
import org.droidslicer.util.InstructionsBuilder;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.TypeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.strings.Atom;

public class AppModelClass extends FakeClass
{
	private static final Logger mLogger = LoggerFactory.getLogger(AppModelClass.class);
	private static final TypeReference TYPE_APP_INFO = 
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/pm/ApplicationInfo");
	private static final TypeReference TYPE_APP = 
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/Application");
	private static final TypeReference TYPE_CONTEXT = 
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/Context");
	private static final int MAX_ALLOC_DEPTH = 2;
	private static class Context
	{
		public AbstractAnalysisConfig config;
		public AndroidAppInfo appInfo;
		public SSAInstructionFactory instsFactory;
		public InstructionsBuilder instsBuilder;
	}
	private final IMethod mEntryMethod;
	public AppModelClass(TypeReference type, IClassHierarchy cha, AbstractAnalysisConfig config, AndroidAppInfo appInfo)
	{
		super(type, cha);

		Language lang = cha.getScope().getLanguage(ClassLoaderReference.Primordial.getLanguage());
	    SSAInstructionFactory instFactory = lang.instructionFactory();
		
		setModifiers(ClassConstants.ACC_PUBLIC);
		
		mEntryMethod = setupEntryOrListenerMethods(instFactory, config, appInfo);
	}
	public IMethod getEntryMethod()
	{
		return mEntryMethod;
	}
	public Collection<Statement> getIndirectIntentFlows(SDG sdg, AbstractAnalysisConfig config)
	{
		CallGraph cg = sdg.getCallGraph();
		ArrayList<Statement> result = new ArrayList<Statement>();
		Set<CGNode> entryNodes = cg.getNodes(mEntryMethod.getReference());
		for(CGNode entryNode : entryNodes)
		{
			IR ir = entryNode.getIR();
			SSAInstruction[] insts = ir.getInstructions();
			Iterator<CallSiteReference> callSitesItr = entryNode.iterateCallSites();
			while(callSitesItr.hasNext())
			{
				CallSiteReference callSite = callSitesItr.next();
				MethodReference declaredTarget = callSite.getDeclaredTarget();
				if(declaredTarget.equals(MethodId.ANDROID_ACTIVITY_SET_INTENT.getMethodReference()))
				{
					IntSet instIdxSet = ir.getCallInstructionIndices(callSite);
					IntIterator instIdxItr = instIdxSet.intIterator();
					while(instIdxItr.hasNext())
					{
						int instIdx = instIdxItr.next();
						SSAInstruction inst = insts[instIdx];
						if(!(inst instanceof SSAAbstractInvokeInstruction))
							continue;
						SSAAbstractInvokeInstruction invokeInst = (SSAAbstractInvokeInstruction)inst;
						int intentValNum = invokeInst.getUse(1);
						result.add(new ParamCaller(entryNode, instIdx, intentValNum));	
					}
				}
			}
		}
		return result;
	}
	public Collection<EntryMethodInvoke> getEntryMethodInvokesInfo(
			AndroidAnalysisContext analysisCtx, AbstractAnalysisConfig config, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Finding entry nodes and associated heap flows", 100);
			Set<MethodReference> entryMethods = new HashSet<MethodReference>();
			{
				Iterator<EntryCompSpec> entrySpecs = config.entryComponentSpecsIterator();
				while(entrySpecs.hasNext())
				{
					EntryCompSpec spec = entrySpecs.next();
					Iterator<EntryMethodSpec> itr = spec.entryMethodsIterator();
					while(itr.hasNext())
					{
						EntryMethodSpec methodSpec = itr.next();
						entryMethods.add(methodSpec.getMethod());
					}
				}
			}
			Collection<Pair<CGNode, CallSiteReference>> entryInvokes = getEntryMethodCallSites(analysisCtx, entryMethods);
			Collection<EntryMethodInvoke> entryMethodInvokes = buildEntryMethodInvokes(analysisCtx, config, entryInvokes);
			monitor.worked(10);
			//Collection<Statement> intentInflows = getIndirectIntentFlows(sdg, config);
			//attachHeapFlowsEntryInvokes(sdg, config, entryMethodInvokes, entryMethods, intentInflows, new SubProgressMonitor(monitor, 90));
			return entryMethodInvokes;
		}
		finally
		{
			monitor.done();
		}
	}
	/*
	public EntryCompsInfo getAppEntryEntities(SDG sdg, AbstractAnalysisConfig config)
	{
		Set<MethodReference> entryMethods = new HashSet<MethodReference>();
		{
			Iterator<EntryCompSpec> entrySpecs = config.entryComponentSpecsIterator();
			while(entrySpecs.hasNext())
			{
				EntryCompSpec spec = entrySpecs.next();
				Iterator<EntryMethodSpec> itr = spec.entryMethodsIterator();
				while(itr.hasNext())
				{
					EntryMethodSpec methodSpec = itr.next();
					entryMethods.add(methodSpec.getMethod());
				}
			}
		}
		Collection<Pair<CGNode, CallSiteReference>> entryInvokes = getAppEntryInvokes(sdg, entryMethods);
		EntryCompsInfo entryCompsInfo = 
				buildEntryEntities(sdg, config, entryInvokes);
		Collection<Statement> intentInflows = getIndirectIntentFlows(sdg, config);
		attachIntentInflow(entryCompsInfo.getEntryCompEntities(), entryMethods, intentInflows, sdg, config);
		return entryCompsInfo;
	}*/
	/*protected void attachHeapFlowsEntryInvokes(
			SDG sdg, 
			AbstractAnalysisConfig config, 
			Collection<EntryMethodInvoke> entryMethodInvokes, 
			final Set<MethodReference> entryMethods, 
			Collection<Statement> intentFlows,
			ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Finding heap flows of intent to entry nodes", 100);
			mLogger.debug("Finding heap flows of intent to entry nodes");
			final AnalysisScope scope = sdg.getClassHierarchy().getScope();
			IClassHierarchy cha = sdg.getClassHierarchy();
			IClass actClass = cha.lookupClass(TypeId.ANDROID_ACTIVITY.getTypeReference());
			if(actClass == null)
				throw new IllegalArgumentException("Fail to find " + TypeId.ANDROID_ACTIVITY.getTypeReference().getName() + " in class hierarchy");
			CallPredicate bypassMethodPredicate = new CallPredicate()
			{
				@Override
				public boolean test(MethodReference declaredTarget, IMethod target)
				{
					if(target != null && target.getDeclaringClass().getClassLoader().getReference().equals(scope.getApplicationLoader()))
						return true;
					return entryMethods.contains(declaredTarget);
				}			
			};
			Map<Statement, IntSet> seeds = new HashMap<Statement, IntSet>();
			{
				for(Statement stm : intentFlows)
				{
					seeds.put(stm, SparseIntSet.singleton(0));
				}
			}
			BypassSliceProblem problem;
			{
				Predicate<Statement> terminatorsPred = Predicate.falsePred();
				problem = new BypassSliceProblem(new SDGSupergraph(sdg), seeds, 1, terminatorsPred, bypassMethodPredicate);
				problem.setCutReturnToSynthetic(false);
			}
			PartiallyBalancedTabulationSolver<Statement, PDG, Object> solver = PartiallyBalancedTabulationSolver
				    .createPartiallyBalancedTabulationSolver(problem, new SubProgressMonitor(monitor, 95));
			
			solver.solve();
			
			Map<Pair<MethodReference, CGNode>, EntryMethodInvoke> entryMethodInvokeMap = new HashMap<Pair<MethodReference, CGNode>, EntryMethodInvoke>();
			{
				for(EntryMethodInvoke methodInvoke : entryMethodInvokes)
				{
					EntryMethodSpec methodSpec = methodInvoke.getEntryMethodSpec();
					if(methodSpec.isStatic())
						throw new IllegalArgumentException("Entry method invocation with static invocation");
					Pair<MethodReference, CGNode> key = Pair.make(methodSpec.getMethod(), methodInvoke.getCalleeNode());
					if(entryMethodInvokeMap.containsKey(key))
						throw new IllegalArgumentException("Two different entry method invocations with same method reference");
					entryMethodInvokeMap.put(key, methodInvoke);
				}
			}
			
			mLogger.debug("Heap param flows of Intent: ");			
			for(CallEntry entry : problem.getBypassedCallers())
			{
				Statement callStm = entry.getCallerStatement();
				Statement entryStm = entry.getEntryStatement();
				if(!(callStm instanceof HeapParamCaller) || !(entryStm instanceof HeapParamCallee))
					continue;
				HeapParamCaller heapCaller = (HeapParamCaller)callStm;
				HeapParamCallee heapCallee = (HeapParamCallee)entryStm;
				MethodReference declaredTarget = heapCaller.getCall().getCallSite().getDeclaredTarget();
				
				// Only activity class should have intent heap flows
				if(!declaredTarget.getDeclaringClass().getName().equals(TypeId.ANDROID_ACTIVITY.getTypeReference().getName()))
					continue;	
				Pair<MethodReference, CGNode> key = Pair.make(declaredTarget, heapCallee.getNode());
				EntryMethodInvoke methodInvoke = entryMethodInvokeMap.get(key);
				if(methodInvoke != null)
				{
					methodInvoke.addHeapFlow(heapCallee);
					mLogger.debug("\t{}", heapCallee);
				}
			}
		}
		finally
		{
			monitor.done();
		}
	}*/
	/*
	protected void attachIntentInflow(
			Map<TypeReference, ComponentUnit> entryEntities,
			final Collection<MethodReference> entryMethods, 
			Collection<Statement> intentFlows,
			SDG sdg, 
			AbstractAnalysisConfig config)
	{
		final AnalysisScope scope = sdg.getClassHierarchy().getScope();
		CallPredicate bypassMethodPredicate = new CallPredicate()
		{
			@Override
			public boolean test(MethodReference declaredTarget, IMethod target)
			{
				if(target != null && target.getDeclaringClass().getClassLoader().getReference().equals(scope.getApplicationLoader()))
					return true;
				return entryMethods.contains(declaredTarget);
			}			
		};
		Map<Statement, IntSet> seeds = new HashMap<Statement, IntSet>();
		{
			for(Statement stm : intentFlows)
			{
				seeds.put(stm, SparseIntSet.singleton(0));
			}
		}
		PointerAnalysis pa = sdg.getPointerAnalysis();
		BypassSliceProblem problem;
		{
			Predicate<Statement> terminatorsPred = Predicate.falsePred();
			problem = new BypassSliceProblem(new SDGSupergraph(sdg), seeds, 1, terminatorsPred, bypassMethodPredicate);
			problem.setCutReturnToSynthetic(false);
		}
		PartiallyBalancedTabulationSolver<Statement, PDG, Object> solver = PartiallyBalancedTabulationSolver
			    .createPartiallyBalancedTabulationSolver(problem, null);
		
		try
		{
			TabulationResult<Statement, PDG, Object> tResult = solver.solve();
			if(mLogger.isDebugEnabled())
			{
				StringBuilder builder = new StringBuilder();
				builder.append("Statements reached: \n");
				for(Statement stm : tResult.getSupergraphNodesReached())
				{
					builder.append('\t');
					builder.append(stm.toString());
					builder.append('\n');
				}
				mLogger.debug("{}", builder.toString());
			}
		}
		catch(CancelException ex)
		{
			// Unreachable
			throw new RuntimeException();
		}
		for(CallEntry entry : problem.getBypassedCallers())
		{
			Statement entryStm = entry.getEntryStatement();
			if(entryStm == null)
				continue;
			CGNode entryNode = entryStm.getNode();
			IR ir = entryNode.getIR();
			IMethod entryMethod = entryNode.getMethod();
			IClass entryClass = entryMethod.getDeclaringClass();
			if(entryMethod.isStatic() || 
				ir.getNumberOfParameters() <= 0 || 
				!entryClass.getClassLoader().getReference().equals(scope.getApplicationLoader()))
			{
				continue;
			}
			
			// Find what the possible concrete type of 'this' parameter
			// Notice that we shouldn't directly use the class of the entry statement, because,
			// for example, C extends A, D extends A, A extends Activity, C and D are registered in 
			// the manifest as entry components, and the method we see here is implemented in A, but not in 
			// C or D. In such case, the class of the entry method is A, but the 'this' parameter could point to
			// C or D.
			int thisValNum = ir.getParameterValueNumbers()[0];
			OrdinalSet<InstanceKey> instances = pa.getPointsToSet(new LocalPointerKey(entryNode, thisValNum));
			for(InstanceKey instance : instances)
			{
				IClass clazz = instance.getConcreteType();
				ComponentUnit compEntity = entryEntities.get(clazz.getReference());
				if(compEntity == null)
					continue;
				Pair<ICCParamCalleeUnit, ICCRetCalleeUnit> pair = compEntity.getEntryMethod(entryMethod.getSelector());
				ICCParamCalleeUnit paramEntity = pair.fst;
				if(paramEntity instanceof CICCParamCalleeUnit)
					((CICCParamCalleeUnit)paramEntity).addOutflowStatement(entryStm);
				mLogger.debug("Out-flow statement added for entry class {}: {}", clazz, entryStm);
			}
		}
	}*/
	protected Collection<EntryMethodInvoke> buildEntryMethodInvokes(
			AndroidAnalysisContext analysisCtx, AbstractAnalysisConfig config, Collection<Pair<CGNode, CallSiteReference>> entryInvokes)
	{
		CallGraph cg = analysisCtx.getCallGraph();
		IClassHierarchy cha = analysisCtx.getClassHierarchy();
		AnalysisScope scope = cha.getScope();
		IClass[] compClasses = buildCompClasses(cha);
		Collection<EntryMethodInvoke> result = new ArrayList<EntryMethodInvoke>();
		for(Pair<CGNode, CallSiteReference> entryInvoke : entryInvokes)
		{
			CGNode callerNode = entryInvoke.getLeft();
			CallSiteReference callSite = entryInvoke.getRight();
			if(callSite.isStatic())
				continue;
			MethodReference declaredTarget = callSite.getDeclaredTarget();
			TypeReference declaredTypeRef = declaredTarget.getDeclaringClass();
			if(callSite.isSpecial() && declaredTarget.getName().equals(MethodReference.initAtom))
			{
				IClass declaredType = cha.lookupClass(declaredTypeRef);
				if(declaredType == null)
					continue;
				IClass baseCompClass = null;
				for(IClass compClass : compClasses)
				{
					if(cha.isSubclassOf(declaredType, compClass))
					{
						baseCompClass = compClass;
						break;
					}
				}
				if(baseCompClass == null)
					continue;
				EntryCompSpec compSpec = config.getEntryCompSpec(baseCompClass.getReference());
				if(compSpec == null)
					continue;
				for(CGNode target : cg.getPossibleTargets(callerNode, callSite))
				{
					IMethod targetMethod = target.getMethod();
					if(!targetMethod.getDeclaringClass().getClassLoader().getReference().equals(scope.getApplicationLoader()))
						continue;
					EntryMethodSpec methodSpec = new EntryMethodSpec(declaredTarget, callSite.isStatic());
					EntryMethodInvoke methodInvoke = new EntryMethodInvoke(compSpec, methodSpec, target);
					result.add(methodInvoke);
				}				
			}
			else if(!callSite.isSpecial())
			{
				EntryCompSpec compSpec = config.getEntryCompSpec(declaredTypeRef);
				if(compSpec == null)
					continue;
				EntryMethodSpec methodSpec = compSpec.getMethodSpec(declaredTarget.getSelector());
				if(methodSpec == null)
					continue;
				for(CGNode target : cg.getPossibleTargets(callerNode, callSite))
				{
					IMethod targetMethod = target.getMethod();
					if(!targetMethod.getDeclaringClass().getClassLoader().getReference().equals(scope.getApplicationLoader()))
					{
						continue;
					}
					int nParam = targetMethod.getNumberOfParameters();
					if(nParam <= 0 || 
						targetMethod.isStatic() || 
						methodSpec.isStatic() != targetMethod.isStatic() || 
						methodSpec.getNumberOfParameters() != nParam)
					{
						continue;
					}
					
					result.add(new EntryMethodInvoke(compSpec, methodSpec, target));
				}
			}
		}
		return result;
	}
	/*
	protected EntryCompsInfo 
		buildEntryEntities(SDG sdg, AbstractAnalysisConfig config, Collection<Pair<CGNode, CallSiteReference>> entryInvokes)
	{
		Map<TypeReference, ComponentUnit> entryCompEntities = 
				new HashMap<TypeReference, ComponentUnit>();
		Map<InstanceKey, Set<CGNode>> instanceNodes = new HashMap<InstanceKey, Set<CGNode>>();
		CallGraph cg = sdg.getCallGraph();
		IClassHierarchy cha = sdg.getClassHierarchy();
		AnalysisScope scope = cha.getScope();
		PointerAnalysis pa = sdg.getPointerAnalysis();
		IClass actClass, receiverClass, providerClass, serviceClass, appClass;
		actClass = cha.lookupClass(TypeId.TYPE_ANDROID_ACTIVITY);
		receiverClass = cha.lookupClass(TypeId.TYPE_ANDROID_RECEIVER);
		providerClass = cha.lookupClass(TypeId.TYPE_ANDROID_PROVIDER);
		serviceClass = cha.lookupClass(TypeId.TYPE_ANDROID_SERVICE);
		appClass = cha.lookupClass(TypeId.TYPE_ANDROID_APPLICATION);
		if(actClass == null || receiverClass == null || providerClass == null || serviceClass == null || appClass == null)
			throw new IllegalArgumentException("Fail to find some of the entry component types in class hierarchy");
		for(Pair<CGNode, CallSiteReference> entryInvoke : entryInvokes)
		{
			CGNode callerNode = entryInvoke.fst;
			CallSiteReference callSite = entryInvoke.snd;
			if(callSite.isStatic())
				continue;
			MethodReference declaredTarget = callSite.getDeclaredTarget();
			TypeReference declaredType = declaredTarget.getDeclaringClass();
			EntryCompSpec compSpec = config.getEntryCompSpec(declaredType);
			if(compSpec == null)
				continue;
			EntryMethodSpec methodSpec = compSpec.getMethodSpec(declaredTarget.getSelector());
			if(methodSpec == null)
				continue;
			for(CGNode target : cg.getPossibleTargets(callerNode, callSite))
			{
				IMethod targetMethod = target.getMethod();
				if(!targetMethod.getDeclaringClass().getClassLoader().getReference().equals(scope.getApplicationLoader()))
				{
					continue;
				}
				int nParam = targetMethod.getNumberOfParameters();
				if(nParam <= 0 || 
					targetMethod.isStatic() || 
					methodSpec.isStatic() != targetMethod.isStatic() || 
					methodSpec.getNumberOfParameters() != nParam)
				{
					continue;
				}
				Selector selector = targetMethod.getSelector();
				IR ir = target.getIR();
				int thisValNum = ir.getParameter(0);
				LocalPointerKey thisPointer = new LocalPointerKey(target, thisValNum);
				OrdinalSet<InstanceKey> instances = pa.getPointsToSet(thisPointer);
				HashSet<IClass> compTypes = new HashSet<IClass>();
				{
					for(InstanceKey instance : instances)
					{
						IClass compType = instance.getConcreteType();
						compTypes.add(compType);
						Set<CGNode> nodes = instanceNodes.get(instance);
						if(nodes == null)
						{
							nodes = new HashSet<CGNode>();
							instanceNodes.put(instance, nodes);
						}
						nodes.add(target);
					}
				}
				for(IClass compType : compTypes)
				{
					ComponentUnit compEntity = entryCompEntities.get(compType.getReference());
					if(compEntity == null)
					{
						if(cha.isSubclassOf(compType, actClass))
						{
							compEntity = new ActivityUnit(compType.getReference());
						}
						else if(cha.isSubclassOf(compType, receiverClass))
						{
							compEntity = new ReceiverUnit(compType.getReference());
						}
						else if(cha.isSubclassOf(compType, providerClass))
						{
							compEntity = new ProviderUnit(compType.getReference());
						}
						else if(cha.isSubclassOf(compType, serviceClass))
						{
							compEntity = new ServiceUnit(compType.getReference());
						}
						else if(cha.isSubclassOf(compType, appClass))
						{
							compEntity = new ApplicationUnit(compType.getReference());
						}
						if(compEntity != null)
							entryCompEntities.put(compType.getReference(), compEntity);	
						else
							continue;
					}
					
					// This should normally not happen
					if(compType.getMethod(selector) == null)
					{
						mLogger.error("Expecting method with selector {} in class {}", selector, compType);
						continue;
					}
					
					MethodReference entryMethod = MethodReference.findOrCreate(compType.getReference(), selector);
					CICCParamCalleeUnit paramEntity;
					CICCRetCalleeUnit retEntity;
					{
						paramEntity = new CICCParamCalleeUnit(entryMethod);
						for(int i = 0; i < nParam; ++i)
						{
							if(methodSpec.isParamTrack(i))
							{
								int paramValNum = ir.getParameter(i);
								paramEntity.addOutflowStatement(new ParamCallee(target, paramValNum));
							}
						}
					}
					{
						retEntity = new CICCRetCalleeUnit(entryMethod);
						if(!targetMethod.getReturnType().equals(TypeReference.Void))
							retEntity.addInflowStatement(new NormalReturnCallee(target));
					}
					compEntity.addEntryMethod(paramEntity, retEntity);
				}
			}					
		}
		return new EntryCompsInfo(entryCompEntities, instanceNodes);
	}*/
	protected static IClass[] buildCompClasses(IClassHierarchy cha)
	{
		IClass[] compClasses = new IClass[]{
				cha.lookupClass(TypeId.ANDROID_ACTIVITY.getTypeReference()),
				cha.lookupClass(TypeId.ANDROID_RECEIVER.getTypeReference()),
				cha.lookupClass(TypeId.ANDROID_PROVIDER.getTypeReference()),
				cha.lookupClass(TypeId.ANDROID_SERVICE.getTypeReference()),
				cha.lookupClass(TypeId.ANDROID_APPLICATION.getTypeReference())};
		for(IClass compClass : compClasses)
		{
			if(compClass == null)
				throw new IllegalArgumentException("Fail to find a component type in class hierarchy");
		}
		return compClasses;
	}
	protected Collection<Pair<CGNode, CallSiteReference>> getEntryMethodCallSites(
			AndroidAnalysisContext analysisCtx, Set<MethodReference> entryMethods)
	{	
		CallGraph cg = analysisCtx.getCallGraph();
		IClassHierarchy cha = analysisCtx.getClassHierarchy();
		IClass[] compClasses = buildCompClasses(cha);
		// We don't need to consider listener here, since listener can only be registered dynamically
		
		ArrayList<Pair<CGNode, CallSiteReference>> result = new ArrayList<Pair<CGNode, CallSiteReference>>();
		Set<CGNode> entryNodes = cg.getNodes(mEntryMethod.getReference());
		for(CGNode entryNode : entryNodes)
		{
			Iterator<CallSiteReference> callSitesItr = entryNode.iterateCallSites();
			while(callSitesItr.hasNext())
			{
				CallSiteReference callSite = callSitesItr.next();
				MethodReference declaredTarget = callSite.getDeclaredTarget();
				if(callSite.isStatic())
					continue;
				if(callSite.isSpecial() && declaredTarget.getName().equals(MethodReference.initAtom))
				{
					TypeReference declaredTypeRef = declaredTarget.getDeclaringClass();
					IClass declaredType = cha.lookupClass(declaredTypeRef);
					if(declaredType == null)
						continue;
					for(IClass compClass : compClasses)
					{
						if(!cha.isSubclassOf(declaredType, compClass))
							continue;
						result.add(Pair.of(entryNode, callSite));
						break;
					}
				}
				else if(!callSite.isSpecial())
				{
					if(entryMethods.contains(declaredTarget))
					{
						result.add(Pair.of(entryNode, callSite));
					}
				}
				
			}
		}
		return result;
	}
	private IMethod setupEntryOrListenerMethods(SSAInstructionFactory instFactory, AbstractAnalysisConfig config, AndroidAppInfo appInfo)
	{
		// Add a method with signature: public static void entry()
		Selector entryMethodSelector = new Selector(
				Atom.findOrCreateAsciiAtom("entry"), 
				Descriptor.findOrCreateUTF8(Language.JAVA, "()V"));
		MethodReference entryMethodRef = MethodReference.findOrCreate(getReference(), entryMethodSelector);
		IClassHierarchy cha = getClassHierarchy();
		
		// The method is static, thus the 1st parameter starts at 1
		InstructionsBuilder instsBuilder = 
				new InstructionsBuilder(
						cha, 
						instFactory, 
						null, 
						0, 
						entryMethodRef.getNumberOfParameters() + 1);
		Context ctx = new Context();
		ctx.appInfo = appInfo;
		ctx.config = config;
		ctx.instsBuilder = instsBuilder;
		ctx.instsFactory = instFactory;
		
		allocFields(ctx);
		
		int appCtxValNum = instsBuilder.addLocal();
		IClass appType = allocApp(appCtxValNum, ctx);
		
		int appInfoValNum = instsBuilder.addLocal();	
		allocAppInfo(appInfoValNum, ctx);
			
		int ctxValNum = instsBuilder.addLocal();
		allocContextImpl(ctxValNum, appCtxValNum, appInfoValNum, ctx);
		
		attachBaseContextIfApply(appCtxValNum, appType.getReference(), ctxValNum, ctx);

		allocAppComponents(ctxValNum, appCtxValNum, ctx);
		
		createEventLoop(ctxValNum, appCtxValNum, ctx);

		MethodSummary entryMethodSummary = new MethodSummary(entryMethodRef);
		entryMethodSummary.setStatic(true);
		entryMethodSummary.setFactory(false);
		for(Map.Entry<Integer, ConstantValue> entry : instsBuilder.getConstants().entrySet())
			entryMethodSummary.addConstant(entry.getKey(), entry.getValue());
		for(SSAInstruction inst : instsBuilder.getInstructions())
			entryMethodSummary.addStatement(inst);
		SummarizedMethod method = new SummarizedMethod(entryMethodRef, entryMethodSummary, this);
		addMethod(method);
		
		if(mLogger.isWarnEnabled())
		{
			for(InstructionException ex : instsBuilder.getSuppressedExceptions())
				mLogger.warn("Exception occurred when building context implementation: {}", ex);
		}
		return method;
	}
	private void allocFields(Context ctx)
	{
		// For each static array fields for the app entry components
		Iterator<EntryCompSpec> entryCompSpecs = ctx.config.entryComponentSpecsIterator();
		while(entryCompSpecs.hasNext())
		{
			EntryCompSpec spec = entryCompSpecs.next();
			
			// We only have one android.app.Application, so we don't need to have a field 
			// for it.
			if(spec instanceof AndroidApplicationSpec)
				continue;
			
			// Allocate the array
			FieldReference fieldRef = findOrCreateTypeListField(spec.getClassType()).getReference();
			int valNum = ctx.instsBuilder.addAllocation(getTypeListFieldType(), null, 1, false);
			SSAPutInstruction inst = ctx.instsFactory.PutInstruction(valNum, fieldRef);
			ctx.instsBuilder.addInstruction(inst);
		}
	}
	
	/**
	 * Add instructions for allocating and initiating ApplicationInfo.
	 * @param appInfoValNum
	 * @param cha
	 * @param instFactory
	 * @param instsBuilder
	 */
	private void allocAppInfo(int appInfoValNum, Context ctx)
	{
		IClassHierarchy cha = getClassHierarchy();
		
		// Add instructions for instantiating an ApplicationInfo
		IClass appInfoType = cha.lookupClass(TYPE_APP_INFO);
		if(appInfoType == null)
			throw new IllegalArgumentException("Fail to find android.content.pm.ApplicationInfo in class hierarchy");
		NewSiteReference appInfoNewSite = new NewSiteReference(ctx.instsBuilder.getNextPC(), TYPE_APP_INFO);
		SSANewInstruction newInst = ctx.instsFactory.NewInstruction(appInfoValNum, appInfoNewSite);
		ctx.instsBuilder.addInstruction(newInst);
		
		// Add instructions for instantiating the class fields
		Collection<IField> fields = appInfoType.getDeclaredInstanceFields();

		for(IField field : fields)
		{
			TypeReference fieldType = field.getFieldTypeReference();
			if(!fieldType.isPrimitiveType())
			{
				int valNum = ctx.instsBuilder.addAllocation(field.getFieldTypeReference(), null, MAX_ALLOC_DEPTH, true);
				SSAPutInstruction putInst = ctx.instsFactory.PutInstruction(appInfoValNum, valNum, field.getReference());
				ctx.instsBuilder.addInstruction(putInst);
			}
		}
	}
	
	/**
	 * Add instructions for instantiating Application.
	 * @param appCtxValNum
	 * @param cha
	 * @param instFactory
	 * @param instsBuilder
	 * @param appInfo
	 */
	private IClass allocApp(int appCtxValNum, Context ctx)
	{
		IClassHierarchy cha = getClassHierarchy();
		IClass appType = cha.lookupClass(TYPE_APP);
		if(appType == null)
			throw new IllegalArgumentException("Fail to find android.app.Application in class hierarchy");
		
		// Check if a Application implementation has been declared in manifest file
		AndroidApplication app = ctx.appInfo.getManifest().getApplication();
		IClass targetType;
		if(app != null)
		{
			TypeReference userAppTypeRef = app.getType();
			IClass userAppType = cha.lookupClass(userAppTypeRef);
			if(userAppType == null)
			{
				// TODO Normally, if the class of the Application isn't found in class hierarchy, the Android app wouldn't run.
				// However, for some cases, the class may actually exist, but is failed to be loaded by Wala.
				// For Android app com.google.android.apps.docs.editors.docs, com.google.android.apps.docs.DocsApplication is 
				// declared as Application in manifest, but its superclass "aDZ" fails to be loaded. 
				// The class aDZ actually exist in the app. The warning produced by Wala when building class hierarchy has been checked,
				// but no reason has been found for why the class isn't loaded. It needs further investigation.
				mLogger.warn("An implementation of Application is declared in manifest (" + userAppTypeRef.getName() + "), but it's not found in class hierarchy. Ignore it.");
				targetType = appType;
			}
			else
			{
				if(!cha.isSubclassOf(userAppType, appType))
					throw new IllegalArgumentException("An implementation of Application is declared in manifest (" + userAppTypeRef.getName() + "), but it isn't a subclass of " + appType.getName());
				targetType = userAppType;
			}
		}
		else
		{
			// No implementation of Application is declared in manifest
			// Use the default one
			targetType = appType;
		}
		
		// Instantiate the class
		SSANewInstruction newInst = ctx.instsFactory.NewInstruction(
				appCtxValNum, new NewSiteReference(ctx.instsBuilder.getNextPC(), targetType.getReference()));
		ctx.instsBuilder.addInstruction(newInst);
		
		if(InstructionsBuilder.getDeclaredMethod(targetType, MethodReference.initSelector) == null)
			throw new IllegalArgumentException("Missing default constructor for " + targetType.getName());
		
		// Call the default constructor
		MethodReference initMethodRef = MethodReference.findOrCreate(targetType.getReference(), MethodReference.initSelector);
		ctx.instsBuilder.addInstsInvocation(initMethodRef, new int[]{appCtxValNum}, IInvokeInstruction.Dispatch.SPECIAL);
		return targetType;
	}

	
	/**
	 * Add instructions to instantiate our own Context implementation
	 * @param ctxValNum
	 * @param appCtxValNum
	 * @param appInfoValNum
	 * @param cha
	 * @param instFactory
	 * @param instsBuilder
	 * @param appInfo
	 */
	private void allocContextImpl(int ctxValNum, int appCtxValNum, int appInfoValNum, Context ctx)
	{
		IClassHierarchy cha = getClassHierarchy();
		TypeReference ctxImplTypeRef = ctx.appInfo.getContextImplType();
		if(ctxImplTypeRef == null)
			throw new IllegalArgumentException("Type of Context implementation hasn't been set");
		
		IClass ctxImplType = cha.lookupClass(ctxImplTypeRef);
		if(ctxImplType == null)
			throw new IllegalArgumentException("Context implementation not found in class hierarchy");
		
		// Instantiate the Context implementation
		SSANewInstruction newInst = ctx.instsFactory.NewInstruction(
				ctxValNum, 
				new NewSiteReference(ctx.instsBuilder.getNextPC(), ctxImplTypeRef));
		ctx.instsBuilder.addInstruction(newInst);
		
		// Invoke its constructor, and pass the instance of Application and ApplicationInfo to it
		Descriptor descriptor = Descriptor.findOrCreate(
				new TypeName[]{TYPE_APP.getName(), TYPE_APP_INFO.getName()}, TypeReference.VoidName);
		Selector selector = new Selector(MethodReference.initAtom, descriptor);
		if(InstructionsBuilder.getDeclaredMethod(ctxImplType, selector) == null)
			throw new IllegalArgumentException("Fail to find expected constructor in Context implementation");
		MethodReference initRef = MethodReference.findOrCreate(
				ctxImplTypeRef, 
				selector);
		ctx.instsBuilder.addInstsInvocation(initRef, new int[]{ctxValNum, appCtxValNum, appInfoValNum}, IInvokeInstruction.Dispatch.SPECIAL);
	}
	
	/**
	 * Allocate the instance of the app components of a specific type, e.g. all implementations of Activity.
	 * Then, the instance is added to the corresponding static array of this class.
	 * @param itr
	 * @param spec
	 * @param baseCtxValNum
	 * @param appCtxValNum
	 * @param ctx
	 */
	private void allocAppComponentsType(Collection<? extends AndroidAppComponent> comps, EntryCompSpec spec, int baseCtxValNum, int appCtxValNum, Context ctx)
	{
		IField field = findOrCreateTypeListField(spec.getClassType());
		boolean shouldSetApp = (spec instanceof AndroidActivitySpec) || (spec instanceof AndroidServiceSpec);
		TypeReference baseCompTypeRef = spec.getClassType();
		IClassHierarchy cha = getClassHierarchy();
		IClass baseCompType = cha.lookupClass(baseCompTypeRef);
		if(baseCompType == null)
			throw new IllegalArgumentException("Fail to find " + baseCompTypeRef.getName() + " in class hierarchy");
		for(AndroidAppComponent appComp : comps)
		{
			TypeReference typeRef = appComp.getType();
			IClass type = cha.lookupClass(typeRef);
			if(type == null)
			{
				mLogger.debug("App component "+ typeRef.getName() + " is declared in manifest, but isn't found in class hierarchy. Ignore it.");
				continue;
			}
			if(!cha.isSubclassOf(type, baseCompType))
			{
				mLogger.debug("App component " + typeRef.getName() + " isn't a subclass of " + baseCompTypeRef.getName() + ". Ignore it.");
				continue;
			}
			int valNum = allocAppComponent(typeRef, field, ctx);
			if(valNum < 0)
				continue;
			attachBaseContextIfApply(valNum, typeRef, baseCtxValNum, ctx);
			attachIntentIfApply(valNum, typeRef, ctx);
			if(shouldSetApp)
				setApplicationField(valNum, appCtxValNum, typeRef, ctx);
		}
	}
	/**
	 * Add instructions to allocate the app components declared in manifest, and add the instances
	 * to the corresponding static array fields.
	 * @param baseCtxValNum
	 * @param appCtxValNum
	 * @param cha
	 * @param instFactory
	 * @param instsBuilder
	 * @param config
	 * @param appInfo
	 */
	private void allocAppComponents(int baseCtxValNum, int appCtxValNum, Context ctx)
	{
		AndroidManifest manifest = ctx.appInfo.getManifest();
		allocAppComponentsType(manifest.getActivities(), ctx.config.getActivitySpec(), baseCtxValNum, appCtxValNum, ctx);
		allocAppComponentsType(manifest.getServices(), ctx.config.getServiceSpec(), baseCtxValNum, appCtxValNum, ctx);
		allocAppComponentsType(manifest.getReceivers(), ctx.config.getReceiverSpec(), baseCtxValNum, appCtxValNum, ctx);
		allocAppComponentsType(manifest.getProviders(), ctx.config.getProviderSpec(), baseCtxValNum, appCtxValNum, ctx);
	}
	
	/**
	 * Instantiate a specific app component implementation, and add the instance to the corresponding 
	 * static array of this class.
	 * In some cases, an app component declared in the manifest may not be in the class hierarchy. It 
	 * may happen when the system libraries loaded in the class hierarchy are not complete. 
	 * For example, the class com.android.vending.billing.BillingService is defined the Google Play billing 
	 * library, which is not part of the Android system library. 
	 * @param typeRef
	 * @param field
	 * @param ctx
	 * @return the value number of the allocated app component instance, or -1 if fail
	 */
	private int allocAppComponent(TypeReference typeRef, IField field, Context ctx)
	{
		IClassHierarchy cha = getClassHierarchy();
		IClass type = cha.lookupClass(typeRef);
		if(type == null)
		{
			// Some APKs have some Activities declared in the manifest but not found in the 
			// class hierarchy. It may be because of obfuscation, and it won't prevent the app from being 
			// installed and run.
			// Example: com.tencent.mm
			mLogger.debug("Fail to find " + typeRef.getName() + " in class hierarchy. Ignore it.");
			return -1;
		}

		// Allocate and instantiate the app component 
		if(InstructionsBuilder.getDeclaredMethod(type, MethodReference.initSelector) == null)
			throw new IllegalArgumentException("Missing default constructor for app component " + type.getName());
		int appCompValNum = ctx.instsBuilder.addAllocation(typeRef, null, 1, false);
		
		// Add the instance to the corresponding array field
		int fieldValNum = ctx.instsBuilder.addLocal();
		if(!field.isStatic())
			throw new IllegalArgumentException("Expecting static field");
		if(!field.getFieldTypeReference().equals(getTypeListFieldType()))
			throw new IllegalArgumentException("Expecting a field of type ArrayList");
		SSAGetInstruction getInst = ctx.instsFactory.GetInstruction(fieldValNum, field.getReference());
		ctx.instsBuilder.addInstruction(getInst);
		MethodReference addMethodRef = MethodId.ARRAY_LIST_ADD_OBJ.getMethodReference();
		ctx.instsBuilder.addInstsInvocation(addMethodRef, new int[]{fieldValNum, appCompValNum}, IInvokeInstruction.Dispatch.VIRTUAL);
		return appCompValNum;
	}
	private void attachBaseContextIfApply(int valNum, TypeReference typeRef, int baseCtxValNum, Context ctx)
	{
		IClassHierarchy cha = getClassHierarchy();
		IClass type = cha.lookupClass(typeRef);
		IClass ctxWrapperType = cha.lookupClass(TypeId.ANDROID_CONTEXT_WRAPPER.getTypeReference());
		if(type == null || ctxWrapperType == null)
			throw new IllegalArgumentException("Fail to find the type in class hierarchy");
		
		// It isn't a sub-class of ContextWrapper, needn't attach base context
		if(!cha.isSubclassOf(type, ctxWrapperType))
			return;
		MethodReference methodRef = MethodId.ANDROID_CTX_WRAPPER_ATTACH_BASE_CTX.getMethodReference();
		if(InstructionsBuilder.getDeclaredMethod(ctxWrapperType, methodRef.getSelector()) == null)
			throw new IllegalArgumentException("Fail to find method attachBaseContext in ContextWrapper");
		ctx.instsBuilder.addInstsInvocation(methodRef, new int[]{valNum, baseCtxValNum}, IInvokeInstruction.Dispatch.VIRTUAL); 
	}
	private void attachIntentIfApply(int compValNum, TypeReference typeRef, Context ctx)
	{
		IClassHierarchy cha = getClassHierarchy();
		IClass type = cha.lookupClass(typeRef);
		IClass actType = cha.lookupClass(TypeId.ANDROID_ACTIVITY.getTypeReference());
		if(type == null)
			throw new IllegalArgumentException("Fail to find component type " + typeRef + " in class hierarchy");
		if(actType == null)
			throw new IllegalArgumentException("Fail to find " + TypeId.ANDROID_ACTIVITY.getTypeReference() + " in a class hierarchy");
		
		// If it is a subclass of android.app.Activity
		if(!cha.isSubclassOf(type, actType))
			return;

		// Allocate a new instance of Intent
		int intentValNum = ctx.instsBuilder.addAllocation(TypeId.ANDROID_INTENT.getTypeReference(), null, 1, false);
		
		// Invoke Activity.setIntent to set the Intent into the Activity
		MethodReference setIntentMethodRef = MethodId.ANDROID_ACTIVITY_SET_INTENT.getMethodReference();
		ctx.instsBuilder.addInstsInvocation(setIntentMethodRef, new int[]{compValNum, intentValNum}, IInvokeInstruction.Dispatch.VIRTUAL);
	}
	
	/**
	 * For each instance field of {@code valNum} with type android.app.Application and declared in {@code typeRef}, 
	 * add instructions to set its value to be the instance pointed by value number {@code appCtxValNum}.
	 * @param valNum
	 * @param appCtxValNum
	 * @param typeRef
	 * @param cha
	 * @param instFactory
	 * @param instsBuilder
	 */
	private void setApplicationField(int valNum, int appCtxValNum, TypeReference typeRef, Context ctx)
	{
		IClassHierarchy cha = getClassHierarchy();
		IClass type = cha.lookupClass(typeRef);
		if(type == null)
			throw new IllegalArgumentException("Fail to find the type in class hierarchy");
		for(IField field : type.getAllInstanceFields())
		{
			TypeReference fieldType = field.getFieldTypeReference();
			if(fieldType.equals(TYPE_APP))
			{
				SSAPutInstruction putInst = ctx.instsFactory.PutInstruction(valNum, appCtxValNum, field.getReference());
				ctx.instsBuilder.addInstruction(putInst);
			}
		}
	}
	private void createEventLoop(int ctxValNum, int appCtxValNum, Context ctx)
	{
		IClassHierarchy cha = getClassHierarchy();
		final int switchValNum = ctx.instsBuilder.addLocal();
		final int switchPC = ctx.instsBuilder.getNextPC();
		
		InstructionsBuilder instsBuilder = ctx.instsBuilder;
		InstructionsBuilder casesBuilder = 
				new InstructionsBuilder(cha, ctx.instsFactory, instsBuilder.getConstants(), switchPC + 1, instsBuilder.getNextLocal());
		List<Integer> labels = new ArrayList<Integer>();
		ctx.instsBuilder = casesBuilder;
		try
		{
			createEventLoopCases(ctxValNum, appCtxValNum, switchPC, labels, ctx);
		}
		finally
		{
			if(mLogger.isDebugEnabled())
			{
				for(InstructionException ex : casesBuilder.getSuppressedExceptions())
					mLogger.debug("Suppressed exception: {}", ex.getMessage());
			}
			ctx.instsBuilder = instsBuilder;
			while(instsBuilder.getNextLocal() < casesBuilder.getNextLocal())
				instsBuilder.addLocal();
		}
		int[] casesAndLabels = new int[labels.size() * 2];
		for(int i = 0; i < labels.size(); ++i)
		{
			casesAndLabels[i * 2] = i;
			casesAndLabels[i * 2 + 1] = labels.get(i);
		}
		SSASwitchInstruction switchInst = ctx.instsFactory.SwitchInstruction(switchValNum, 0, casesAndLabels);
		instsBuilder.addInstruction(switchInst);
		for(SSAInstruction inst : casesBuilder.getInstructions())
			instsBuilder.addInstruction(inst);
	}
	private void createEventLoopCases(
			int ctxValNum, 
			int appCtxValNum, 
			int switchPC,
			List<Integer> labels, 
			Context ctx)
	{		
		// For each entry component
		Iterator<EntryCompSpec> itr = ctx.config.entryComponentSpecsIterator();
		while(itr.hasNext())
		{
			EntryCompSpec spec = itr.next();
			createCasesForEntryClass(spec, ctxValNum, appCtxValNum, switchPC, labels, ctx);
		}
	}
	private int addInstsGetTypeListEntry(TypeReference targetType, Context ctx)
	{
		// Get the array from static field
		int fieldValNum = ctx.instsBuilder.addLocal();
		IField field = findOrCreateTypeListField(targetType);
		if(!field.isStatic())
			throw new IllegalArgumentException("Expecting static field");
		SSAGetInstruction getInst = ctx.instsFactory.GetInstruction(fieldValNum, field.getReference());
		ctx.instsBuilder.addInstruction(getInst);
		
		// Get an instance from the array	
		int idxValNum = ctx.instsBuilder.addLocal();
		int[] paramsGet = new int[]{fieldValNum, idxValNum};
		int retValNum = 
				ctx.instsBuilder.addInstsInvocation(MethodId.ARRAY_LIST_GET_INT.getMethodReference(), paramsGet, IInvokeInstruction.Dispatch.VIRTUAL);
		
		// check-cast
		int targetValNum = ctx.instsBuilder.addLocal();
		SSACheckCastInstruction castInst = ctx.instsFactory.CheckCastInstruction(targetValNum, retValNum, targetType, true);
		ctx.instsBuilder.addInstruction(castInst);
		return targetValNum;
	}
	private void addInstsInvokeTypeEntryCase(
			int targetValNum, 
			IMethod method, 
			int ctxValNum,
			int appCtxValNum,
			int switchPC, 
			List<Integer> labels, 
			Context ctx)
	{
		// Prepare the parameters
		int numParams = method.getNumberOfParameters();
		int[] params = new int[numParams];
		IInvokeInstruction.Dispatch dispatch = InstructionsBuilder.getInvokeDispatch(method);
		int startParamIdx;
		
		// If it is instance method, put the 'this' instance in parameter list
		if(dispatch == IInvokeInstruction.Dispatch.STATIC || numParams == 0)
			startParamIdx = 0;
		else
		{
			params[0] = targetValNum;
			startParamIdx = 1;
		}
		
		// Prepare the remaining parameters
		for(int i = startParamIdx; i < numParams; ++i)
		{
			TypeReference paramType = method.getParameterType(i);
			
			// If the parameter type is Context, use the global Context object
			if(paramType.equals(TYPE_CONTEXT))
				params[i] = ctxValNum;
			
			// Otherwise, allocate a new one
			else
			{
				int paramValNum = ctx.instsBuilder.addAllocation(paramType, null, MAX_ALLOC_DEPTH, true);
				params[i] = paramValNum;
			}
		}
		
		// Invoke the method
		ctx.instsBuilder.addInstsInvocation(method.getReference(), params, dispatch);
		
		// Go back to the switch statement
		ctx.instsBuilder.addInstruction(ctx.instsFactory.GotoInstruction(switchPC));
	}
	private void createCasesForEntryClass(
			EntryCompSpec clazzSpec, 
			int ctxValNum,
			int appCtxValNum,
			int switchPC, 
			List<Integer> labels, 
			Context ctx)
	{
		IClassHierarchy cha = getClassHierarchy();
		TypeReference typeRef = clazzSpec.getClassType();
		IClass type = cha.lookupClass(typeRef);
		if(type == null)
			throw new IllegalArgumentException("Fail to find the class in class hierarchy");
		Iterator<EntryMethodSpec> methodSpecItr = clazzSpec.entryMethodsIterator();
		
		// For each entry method of the entry class
		while(methodSpecItr.hasNext())
		{
			MethodReference methodRef = methodSpecItr.next().getMethod();
			labels.add(ctx.instsBuilder.getNextPC());
			IMethod method = type.getMethod(methodRef.getSelector());
			if(method == null)
				throw new IllegalArgumentException("The method " + methodRef + " isn't declared in the class or its super class");
			
			// Get an instance of the type
			int targetValNum;
			
			// If it is Application, then directly get the reference to the instance from
			// previously stored value number
			if(clazzSpec instanceof AndroidApplicationSpec)
			{
				targetValNum = appCtxValNum;
			}
			else
			{
				// Otherwise, get an instance from the corresponding static array
				targetValNum = addInstsGetTypeListEntry(clazzSpec.getClassType(), ctx);
			}
			
			addInstsInvokeTypeEntryCase(targetValNum, method, ctxValNum, appCtxValNum, switchPC, labels, ctx);
		}
	}
	/**
	 * Return the field name of this class that would be generated if the type is a listener type. 
	 * @param type
	 * @return the field name for the type
	 */
	public static Atom getTypeListFieldName(TypeReference type)
	{
		TypeName typeName = type.getName();
		StringBuilder builder = new StringBuilder();
		builder.append(typeName.getPackage().toString().replace('/', '_'));
		builder.append('_');
		builder.append(typeName.getClassName());
		Atom fieldName = Atom.findOrCreateUnicodeAtom(builder.toString());
		return fieldName;
	}
	public static TypeReference getTypeListFieldType()
	{
		return TypeId.ARRAY_LIST.getTypeReference();
	}
	protected IField findOrCreateTypeListField(TypeReference type)
	{
		Atom fieldName = getTypeListFieldName(type);
		IField field = getField(fieldName, getTypeListFieldType().getName());
		if(field != null)
			return field;
		FieldReference fieldRef = 
				FieldReference.findOrCreate(
					getReference(), 
					fieldName, 
					getTypeListFieldType());
		field = new FakeField(this, fieldRef, ClassConstants.ACC_PUBLIC | ClassConstants.ACC_STATIC);
		addField(field);
		return field;
	}
}
