package org.droidslicer.graph.entity.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.android.appSpec.AndroidActivitySpec;
import org.droidslicer.android.appSpec.AndroidApplicationSpec;
import org.droidslicer.android.appSpec.AndroidListenerSpec;
import org.droidslicer.android.appSpec.AndroidProviderSpec;
import org.droidslicer.android.appSpec.AndroidReceiverSpec;
import org.droidslicer.android.appSpec.AndroidServiceSpec;
import org.droidslicer.android.appSpec.EntryCompSpec;
import org.droidslicer.android.appSpec.EntryMethodSpec;
import org.droidslicer.android.manifest.AndroidComponentWithIntentFilter;
import org.droidslicer.android.manifest.AndroidEntryComponent;
import org.droidslicer.android.manifest.AndroidIntentFilter;
import org.droidslicer.android.manifest.AndroidManifest;
import org.droidslicer.android.manifest.AndroidProvider;
import org.droidslicer.android.model.EntryMethodInvoke;
import org.droidslicer.config.AbstractAnalysisConfig;
import org.droidslicer.graph.BehaviorMethod;
import org.droidslicer.graph.entity.ActivityUnit;
import org.droidslicer.graph.entity.ApplicationUnit;
import org.droidslicer.graph.entity.CICCParamCalleeUnit;
import org.droidslicer.graph.entity.CICCReturnCalleeUnit;
import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.ICCParamCalleeUnit;
import org.droidslicer.graph.entity.ICCReturnCalleeUnit;
import org.droidslicer.graph.entity.IMutableStatementInflowUnit;
import org.droidslicer.graph.entity.IMutableStatementOutflowUnit;
import org.droidslicer.graph.entity.IntentFilterUnit;
import org.droidslicer.graph.entity.ProviderUnit;
import org.droidslicer.graph.entity.ReceiverUnit;
import org.droidslicer.graph.entity.ServiceUnit;
import org.droidslicer.graph.entity.UnitEntity;
import org.droidslicer.ifds.CallRecords;
import org.droidslicer.ifds.InstanceFieldPutSolver;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;
import org.droidslicer.value.ConcatValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;
import org.droidslicer.value.IntValue;
import org.droidslicer.value.IntentFilterValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.UnknownValue;
import org.droidslicer.value.UriEncodedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.HeapStatement.HeapParamCallee;
import com.ibm.wala.ipa.slicer.NormalReturnCallee;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.strings.Atom;

public class UnitsSolvingHelper
{
	private class InstVisitor extends SSAInstruction.Visitor
	{
		// The class loader stored in MethodReference may be incorrect. 
		// To avoid the need to look up the real class loader, we do not use TypeReference as
		// key here, but instead use <TypeName, Descriptor>
		private final HashMap<Pair<TypeName, Selector>, InvocationEntityResolver> mInvokeResolversMap = 
				new HashMap<Pair<TypeName, Selector>, InvocationEntityResolver>();
		private final HashMap<TypeName, AllocationEntityResolver> mAllocResolversMap =
				new HashMap<TypeName, AllocationEntityResolver>();
		private final UnitsResolverContext mResolverCtx;
		private boolean mShouldStop;
		private final ProgressMonitor mMonitor;
		private int mInstIdx;
		public InstVisitor(UnitsResolverContext resolverCtx, ProgressMonitor monitor)
		{
			mResolverCtx = resolverCtx;
			mMonitor = monitor;
			prepareResolversMap();
		}
		public void visit(SSAInstruction inst, int instIdx)
			throws CancelException
		{
			mShouldStop = false;
			mInstIdx = instIdx;
			inst.visit(this);
			if(mShouldStop)
				throw CancelException.make("Operation canceled");
		}
		@Override
		public void visitInvoke(SSAInvokeInstruction invokeInst)
		{
			CGNode node = mResolverCtx.getCurrentNode();
			AndroidAnalysisContext analysisCtx = mResolverCtx.getAnalysisContext();
			CallSiteReference callSiteRef = invokeInst.getCallSite();
			IClassHierarchy cha = analysisCtx.getClassHierarchy();
			Set<CGNode> targets = analysisCtx.getCallGraph().getPossibleTargets(node, callSiteRef);
			AbstractAnalysisConfig config = analysisCtx.getAnalysisConfig();
			try
			{
				if(targets.isEmpty())
				{
					// Fall back to conservative method
					MethodReference declaredTargetRef = callSiteRef.getDeclaredTarget();
					IMethod declaredTarget = cha.resolveMethod(declaredTargetRef);
					if(declaredTarget == null)
						return;
					InvocationEntityResolver invokeResolver = 
							mInvokeResolversMap.get(
									Pair.of(declaredTargetRef.getDeclaringClass().getName(), declaredTargetRef.getSelector()));
					if(invokeResolver != null)
						invokeResolver.resolve(mResolverCtx, invokeInst, mInstIdx, new SubProgressMonitor(mMonitor, 10));
					ClassLoaderReference classLoaderRef = declaredTarget.getDeclaringClass().getClassLoader().getReference();
					if(classLoaderRef.equals(analysisCtx.getAnalysisScope().getPrimordialLoader()))
					{
						ReturnTypeResolver retTypeResolver = config.getReturnTypeResolver(declaredTarget.getReturnType().getName());
						if(retTypeResolver != null)
							retTypeResolver.resolve(mResolverCtx, invokeInst, mInstIdx, new SubProgressMonitor(mMonitor, 10));
					}
				}
				else
				{
					boolean retTypeResolved = false;
					for(CGNode target : targets)
					{
						IMethod method = target.getMethod();
						InvocationEntityResolver invokeResolver = 
								mInvokeResolversMap.get(
										Pair.of(method.getDeclaringClass().getName(), method.getSelector()));
						if(invokeResolver != null)
							invokeResolver.resolve(mResolverCtx, invokeInst, mInstIdx, new SubProgressMonitor(mMonitor, 10));
						if(!retTypeResolved)
						{
							ClassLoaderReference classLoaderRef = method.getDeclaringClass().getClassLoader().getReference();
							if(classLoaderRef.equals(analysisCtx.getAnalysisScope().getPrimordialLoader()))
							{
								retTypeResolved = true;
								ReturnTypeResolver retTypeResolver = config.getReturnTypeResolver(method.getReturnType().getName());
								if(retTypeResolver != null)
									retTypeResolver.resolve(mResolverCtx, invokeInst, mInstIdx, new SubProgressMonitor(mMonitor, 10));
							}
						}
					}
				}
			}
			catch(CancelException ex)
			{
				mShouldStop = true;
			}
		}
		@Override
		public void visitNew(SSANewInstruction newInst) 
		{
			NewSiteReference newSite = newInst.getNewSite();
			TypeReference typeRef = newSite.getDeclaredType();
			AllocationEntityResolver resolver = mAllocResolversMap.get(typeRef.getName());
			if(resolver != null)
			{
				try
				{
					resolver.resolve(mResolverCtx, newInst, mInstIdx, new SubProgressMonitor(mMonitor, 10));
				}
				catch(CancelException ex)
				{
					mShouldStop = true;
				}
			}
		}
		private void prepareResolversMap()
		{
			AndroidAnalysisContext analysisCtx = mResolverCtx.getAnalysisContext();
			mInvokeResolversMap.clear();
			{
				Iterator<InvocationEntityResolver> resolversItr = analysisCtx.getAnalysisConfig().invocationResolversIterator();
				IClassHierarchy cha = analysisCtx.getClassHierarchy();
				while(resolversItr.hasNext())
				{
					InvocationEntityResolver resolver = resolversItr.next();
					
					// Also add all the descendants who are in Android library
					MethodReference methodRef = resolver.getMethodReference();
					IClass clazz = cha.lookupClass(methodRef.getDeclaringClass());
					if(clazz == null)
					{
						// If the method doesn't exit in class hierarchy, IClassHierarchy.getPossibleTarget(MethodReference) will throw
						// AssertionError; thus, we check it here.
						// Notice that this case may happen in normal case when some of the ICC methods not present in current API level.
						mLogger.debug("Method {} for invocation resolver isn't found in class hierarchy. Ignore it.", methodRef);
						continue;
					}
					Collection<IClass> subClazzes = Utils.computeAllSubclassesOrImplementors(cha, clazz, new Predicate<IClass>()
					{
						@Override
						public boolean apply(IClass clazz)
						{
							ClassLoaderReference clazzLoaderRef = clazz.getClassLoader().getReference();
							if(!clazzLoaderRef.equals(ClassLoaderReference.Primordial) || 
								clazz.isPrivate() || 
								clazz.isArrayClass())
							{
								return false;
							}
							return true;
						}
					});
					for(IClass subClazz : subClazzes)
					{
						mInvokeResolversMap.put(Pair.of(subClazz.getName(), methodRef.getSelector()), resolver);
					}
				}
			}
			
			mAllocResolversMap.clear();
			{
				IClassHierarchy cha = analysisCtx.getClassHierarchy();
				Iterator<AllocationEntityResolver> resolversItr = analysisCtx.getAnalysisConfig().allocResolversIterator();
				while(resolversItr.hasNext())
				{
					AllocationEntityResolver resolver = resolversItr.next();
					
					TypeReference type = resolver.getType();
					
					Collection<IClass> subClasses = cha.computeSubClasses(type);
					for(IClass subClass : subClasses)
						mAllocResolversMap.put(subClass.getReference().getName(), resolver);
				}
			}
		}
	}
	private static final Logger mLogger = LoggerFactory.getLogger(UnitsSolvingHelper.class);
	private final static Selector SEL_ACT_CANDIDATE = new Selector(Atom.findOrCreateAsciiAtom("onCreate"), 
			Descriptor.findOrCreate(new TypeName[]{
					TypeId.ANDROID_BUNDLE.getTypeReference().getName()
				}, TypeReference.VoidName));
		private final static Selector SEL_SERVICE_CANDIDATE = new Selector(Atom.findOrCreateAsciiAtom("onCreate"), 
			Descriptor.findOrCreate(new TypeName[]{}, TypeReference.VoidName));
		private final static Selector SEL_APP_CANDIDATE = new Selector(Atom.findOrCreateAsciiAtom("onCreate"), 
			Descriptor.findOrCreate(new TypeName[]{}, TypeReference.VoidName));
		private final static Selector SEL_RECEIVER_CANDIDATE = new Selector(Atom.findOrCreateAsciiAtom("onReceive"), 
			Descriptor.findOrCreate(new TypeName[]{
				TypeId.ANDROID_CONTEXT.getTypeReference().getName(),
				TypeId.ANDROID_INTENT.getTypeReference().getName()
			}, TypeReference.VoidName));
	private final AndroidAnalysisContext mAnalysisCtx;
		
	public UnitsSolvingHelper(
			AndroidAnalysisContext analysisCtx)
	{
		mAnalysisCtx = analysisCtx;
	}
	
	public UnitEntitiesInfo solve(ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Resolving entities", 1000);
			AbstractAnalysisConfig config = mAnalysisCtx.getAnalysisConfig();
			
			// Be careful not to keep a reference to PointerAnalysis or FlowGraph here; otherwise, 
			// we can no garbage collect it to release memory
			Collection<EntryMethodInvoke> entryInvokes = 
					mAnalysisCtx.getAppModelClass().getEntryMethodInvokesInfo(mAnalysisCtx, config, new SubProgressMonitor(monitor, 20));		
			return resolveUnits(entryInvokes, new SubProgressMonitor(monitor, 980));
		}
		finally
		{
			monitor.done();
		}
	}

	protected Collection<Statement> getIndirectIntentFlows()
	{
		CallGraph cg = mAnalysisCtx.getCallGraph();
		ArrayList<Statement> result = new ArrayList<Statement>();
		Set<CGNode> entryNodes = cg.getNodes(mAnalysisCtx.getAppModelClass().getEntryMethod().getReference());
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
	protected Collection<Pair<CGNode, CallSiteReference>> getAppEntryInvokes(Set<MethodReference> entryMethods)
	{	
		IMethod modelEntryMethod = mAnalysisCtx.getAppModelClass().getEntryMethod();
		CallGraph cg = mAnalysisCtx.getCallGraph();	
		ArrayList<Pair<CGNode, CallSiteReference>> result = new ArrayList<Pair<CGNode, CallSiteReference>>();
		Set<CGNode> entryNodes = cg.getNodes(modelEntryMethod.getReference());
		for(CGNode entryNode : entryNodes)
		{
			Iterator<CallSiteReference> callSitesItr = entryNode.iterateCallSites();
			while(callSitesItr.hasNext())
			{
				CallSiteReference callSite = callSitesItr.next();
				MethodReference declaredTarget = callSite.getDeclaredTarget();
				if(entryMethods.contains(declaredTarget))
				{
					result.add(Pair.of(entryNode, callSite));
				}
			}
		}
		return result;
	}
	protected ICCParamCalleeUnit buildICCParamUnit(EntryMethodInvoke entryMethodInvoke, MethodReference entryMethodRef)
	{
		CGNode entryNode = entryMethodInvoke.getCalleeNode();
		IR ir = entryNode.getIR();
		EntryMethodSpec methodSpec = entryMethodInvoke.getEntryMethodSpec();
		int nParam = entryNode.getMethod().getNumberOfParameters();
		Set<HeapParamCallee> heapFlows = entryMethodInvoke.getHeapFlows();
		CICCParamCalleeUnit paramEntity = new CICCParamCalleeUnit(entryMethodRef);
		for(int i = 0; i < nParam; ++i)
		{
			if(methodSpec.isParamTrack(i))
			{
				int paramValNum = ir.getParameter(i);
				paramEntity.addOutflowStatement(new ParamCallee(entryNode, paramValNum));
			}
		}
		for(HeapParamCallee heapStm : heapFlows)
			paramEntity.addOutflowStatement(heapStm);
		return paramEntity;
	}
	protected ICCReturnCalleeUnit buildICCRetCalleeUnit(EntryMethodInvoke entryMethodInvoke, MethodReference entryMethodRef, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Building ICC return callee unit", 100);
			CGNode entryNode = entryMethodInvoke.getCalleeNode();
			IMethod entryMethod = entryNode.getMethod();
			CICCReturnCalleeUnit retEntity = new CICCReturnCalleeUnit(entryMethodRef);
			TypeReference retTypeRef = entryMethod.getReturnType();
			switch(MethodId.getMethodId(entryMethodInvoke.getEntryMethodSpec().getMethod()))
			{
			case ANDROID_PROVIDER_QUERY:
			case ANDROID_PROVIDER_QUERY_CANCEL:
			case ANDROID_PROVIDER_BULK_INSERT:
			case ANDROID_PROVIDER_INSERT:
			case ANDROID_PROVIDER_UPDATE:
			case ANDROID_PROVIDER_DELETE:
			case ANDROID_PROVIDER_GET_TYPE:
				{
					NormalReturnCallee retCallee = new NormalReturnCallee(entryNode);
					retEntity.addInflowStatement(retCallee);
					if(retTypeRef.isClassType() && !retTypeRef.getName().equals(TypeId.STRING.getTypeReference().getName()))
					{
						Set<LocalPointerKey> pointers = new HashSet<LocalPointerKey>();
						for(Iterator<Statement> preStms = mAnalysisCtx.getSDG().getPredNodes(retCallee); preStms.hasNext(); )
						{
							Statement preStm = preStms.next();
							if(!preStm.getKind().equals(Statement.Kind.NORMAL))
								continue;
							NormalStatement normalPreStm = (NormalStatement)preStm;
							SSAInstruction preInst = normalPreStm.getInstruction();
							if(!(preInst instanceof SSAReturnInstruction))
								continue;
							SSAReturnInstruction retInst = (SSAReturnInstruction)preInst;
							if(retInst.returnsVoid() || retInst.returnsPrimitiveType())
								continue;
							int retValNum = retInst.getResult();
							pointers.add(new LocalPointerKey(preStm.getNode(), retValNum));
						}
						InstanceFieldPutSolver putSolver = new InstanceFieldPutSolver(mAnalysisCtx);
						Collection<Statement> putStms = putSolver.solve(
								pointers, new CallRecords(mAnalysisCtx.getCallGraph()), new SubProgressMonitor(monitor, 100));
						for(Statement putStm : putStms)
						{
							retEntity.addInflowStatement(putStm);
						}
					}
					break;
				}
			default:
				break;
			}
			return retEntity;
		}
		finally
		{
			monitor.done();
		}
	}
	protected Selector getFilteredSelector(EntryMethodSpec methodSpec)
	{
		MethodReference methodRef = methodSpec.getMethod();
		TypeReference typeRef = methodRef.getDeclaringClass();
		if(typeRef.getName().equals(TypeId.ANDROID_ACTIVITY.getTypeReference().getName()))
		{
			return SEL_ACT_CANDIDATE;
		}
		else if(typeRef.getName().equals(TypeId.ANDROID_RECEIVER.getTypeReference().getName()))
		{
			return SEL_RECEIVER_CANDIDATE;
		}
		else if(typeRef.getName().equals(TypeId.ANDROID_SERVICE.getTypeReference().getName()))
		{
			return SEL_SERVICE_CANDIDATE;
		}
		else if(typeRef.getName().equals(TypeId.ANDROID_APPLICATION.getTypeReference().getName()))
		{
			return SEL_APP_CANDIDATE;
		}
		else
			return methodRef.getSelector();
	}
	protected void setupResolverContext(
			Collection<EntryMethodInvoke> entryMethodInvokes, UnitsResolverContext resolverCtx, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			int nEntryInvokes = entryMethodInvokes.size();
			monitor.beginTask("Setuping unit resolver context", 10 * nEntryInvokes);
			IClassHierarchy cha = mAnalysisCtx.getClassHierarchy();
			AnalysisScope scope = cha.getScope();
			PointerAnalysis pa = mAnalysisCtx.getPointerAnalysis();
			IClass actClass, receiverClass, providerClass, serviceClass, appClass;
			
			Map<TypeReference, ComponentUnit> compUnits = new HashMap<TypeReference, ComponentUnit>();
			Map<CGNode, Set<BehaviorMethod>> compUnitsForNode = 
					new HashMap<CGNode, Set<BehaviorMethod>>();
			actClass = cha.lookupClass(TypeId.ANDROID_ACTIVITY.getTypeReference());
			receiverClass = cha.lookupClass(TypeId.ANDROID_RECEIVER.getTypeReference());
			providerClass = cha.lookupClass(TypeId.ANDROID_PROVIDER.getTypeReference());
			serviceClass = cha.lookupClass(TypeId.ANDROID_SERVICE.getTypeReference());
			appClass = cha.lookupClass(TypeId.ANDROID_APPLICATION.getTypeReference());
			if(actClass == null || receiverClass == null || providerClass == null || serviceClass == null || appClass == null)
				throw new IllegalArgumentException("Fail to find some of the entry component types in class hierarchy");
			for(EntryMethodInvoke entryMethodInvoke : entryMethodInvokes)
			{
				monitor.worked(10);
				CGNode entryNode = entryMethodInvoke.getCalleeNode();
				IMethod entryMethod = entryNode.getMethod();
				if(!entryMethod.getDeclaringClass().getClassLoader().getReference().equals(scope.getApplicationLoader()))
				{
					continue;
				}
				int nParam = entryMethod.getNumberOfParameters();
				if(nParam <= 0 || entryMethod.isStatic())
					continue;
				IR ir = entryNode.getIR();
				if(ir == null)
					continue;
				
				// Get or create the set of component units for the current node
				// We will update it later
				Set<BehaviorMethod> entryPointsForCurrNode = compUnitsForNode.get(entryNode);
				if(entryPointsForCurrNode == null)
				{
					entryPointsForCurrNode = new HashSet<BehaviorMethod>();
					compUnitsForNode.put(entryNode, entryPointsForCurrNode);
				}
				
				
				// Find what instance can the 'this' pointer points to
				int thisValNum = ir.getParameter(0);
				LocalPointerKey thisPointer = new LocalPointerKey(entryNode, thisValNum);
				OrdinalSet<InstanceKey> instances = pa.getPointsToSet(thisPointer);
				
				EntryCompSpec compSpec = entryMethodInvoke.getEntryCompSpec();
				
				// If it is for listener
				if(compSpec instanceof AndroidListenerSpec)
				{
					
					// Collect the instance <--> call graph node mapping
					for(InstanceKey instance : instances)
					{
						resolverCtx.addEntryNodeForInstance(instance, entryNode);
					}	
				}
				else
				{
					// Collect the types that the 'this' pointer can point to
					HashSet<IClass> compTypes = new HashSet<IClass>();
					{
						for(InstanceKey instance : instances)
						{
							IClass compType = instance.getConcreteType();
							compTypes.add(compType);
							resolverCtx.addEntryNodeForInstance(instance, entryNode);
						}
					}
				
					// For each possible concrete type of 'this' pointer
					for(IClass compType : compTypes)
					{
						// Get or create a component unit for the concrete type of 'this'
						ComponentUnit compUnit = compUnits.get(compType.getReference());
						if(compUnit == null)
						{
							if(compSpec instanceof AndroidActivitySpec)
							{
								compUnit = new ActivityUnit(compType.getReference());
							}
							else if(compSpec instanceof AndroidReceiverSpec)
							{
								compUnit = new ReceiverUnit(compType.getReference());
							}
							else if(compSpec instanceof AndroidProviderSpec)
							{
								compUnit = new ProviderUnit(compType.getReference());
							}
							else if(compSpec instanceof AndroidServiceSpec)
							{
								compUnit = new ServiceUnit(compType.getReference());
							}
							else if(compSpec instanceof AndroidApplicationSpec)
							{
								compUnit = new ApplicationUnit(compType.getReference());
							}
							if(compUnit != null)
								compUnits.put(compType.getReference(), compUnit);
							else
								continue;
						}
						
						
						// Create a parameter callee unit and return callee unit for the invoke
						EntryMethodSpec methodSpec = entryMethodInvoke.getEntryMethodSpec();
						MethodReference entryMethodRef = 
								MethodReference.findOrCreate(compType.getReference(), methodSpec.getMethod().getSelector());
						ICCParamCalleeUnit paramEntity = buildICCParamUnit(entryMethodInvoke, entryMethodRef);
						ICCReturnCalleeUnit retEntity = 
								buildICCRetCalleeUnit(entryMethodInvoke, entryMethodRef, new SubProgressMonitor(monitor, 0));
						
						// Add the 2 units to the component unit
						compUnit.addEntryMethod(paramEntity, retEntity);
						
						// Add the component unit to the component units that this call graph node can be reached
						entryPointsForCurrNode.add(new BehaviorMethod(compUnit, entryMethodRef.getSelector()));
					}
				}
			}
			
			// Collect the entry call graph nodes, and the corresponding set of component units that can reach 
			// the nodes
			for(Map.Entry<CGNode, Set<BehaviorMethod>> entry : compUnitsForNode.entrySet())
			{
				resolverCtx.addPendingNode(entry.getKey(), entry.getValue());
			}
			
			// Collect the information for the entry components in the manifest
			AndroidManifest manifest = mAnalysisCtx.getAppInfo().getManifest();
			Map<TypeName, AndroidEntryComponent> manifestCompsMap = new HashMap<TypeName, AndroidEntryComponent>();
			{
				@SuppressWarnings("unchecked")
				Iterator<? extends AndroidEntryComponent> compItr = 
					Iterators.concat(
							manifest.getActivities().iterator(), 
							manifest.getServices().iterator(), 
							manifest.getProviders().iterator(), 	
							manifest.getReceivers().iterator(),
							Iterators.singletonIterator(manifest.getApplication()));
				while(compItr.hasNext())
				{
					AndroidEntryComponent comp = compItr.next();
					manifestCompsMap.put(comp.getType().getName(), comp);
				}
			}
			for(ComponentUnit comp : compUnits.values())
			{
				// Find whether there's corresponding information in the manifest
				AndroidEntryComponent manifestComp = manifestCompsMap.get(comp.getType().getName());
				if(manifestComp != null)
				{				
					// If the entry component has intent filter, attach the intent filter information from the manifest
					if(comp instanceof IntentFilterUnit && manifestComp instanceof AndroidComponentWithIntentFilter)
					{
						IntentFilterUnit intentFilterUnit = (IntentFilterUnit)comp;
						AndroidComponentWithIntentFilter filterComp = (AndroidComponentWithIntentFilter)manifestComp;
						Iterator<AndroidIntentFilter> filterItr = filterComp.intentFilterIterator();
						
						// For each intent filter
						while(filterItr.hasNext())
						{
							AndroidIntentFilter filter = filterItr.next();
							
							// Convert the intent filter to concrete value
							IntentFilterValue intentFilterVal = convertManifestIntentFilter(filter);
							
							// Add the concrete value to the entity
							intentFilterUnit.addIntentFilterValue(intentFilterVal);
						}
					}
					
					// If it is a content provider, attach the information from the manifest
					if(comp instanceof ProviderUnit && manifestComp instanceof AndroidProvider)
					{
						ProviderUnit providerUnit = (ProviderUnit)comp;
						AndroidProvider providerComp = (AndroidProvider)manifestComp;
						OrValue auths = new OrValue();
						for(String auth : providerComp.getAuthorities())
						{
							auths.addValue(new ConstantStringValue(auth));
						}
						if(auths.isEmpty())
							providerUnit.setAuthorityValue(NullValue.getInstance());
						else
							providerUnit.setAuthorityValue(auths.simplify());
					}
				}
				
				resolverCtx.addComponentUnit(comp);
			}
		}
		finally
		{
			monitor.done();
		}
	}
	protected static IntentFilterValue convertManifestIntentFilter(AndroidIntentFilter intentFilter)
	{
		IntentFilterValue intentFilterVal = new IntentFilterValue();
		Iterator<String> actionsItr = intentFilter.actionNamesIterator();
		while(actionsItr.hasNext())
		{
			intentFilterVal.addAction(new ConstantStringValue(actionsItr.next()));
		}
		Iterator<String> catsItr = intentFilter.categoryNamesIterator();
		while(catsItr.hasNext())
		{
			intentFilterVal.addCategory(new ConstantStringValue(catsItr.next()));
		}
		Iterator<String> schemesItr = intentFilter.dataSchemesIterator();
		while(schemesItr.hasNext())
		{
			intentFilterVal.addDataScheme(new ConstantStringValue(schemesItr.next()));
		}
		Iterator<String> hostsItr = intentFilter.dataHostsIterator();
		while(hostsItr.hasNext())
		{
			String host = hostsItr.next();
			IntIterator portsItr = intentFilter.dataPortsIterator();
			while(portsItr.hasNext())
			{
				int port = portsItr.next();
				String authority = UriEncodedValue.encodeForAndroid(host) + ":" + port;
				intentFilterVal.addDataAuthority(new ConstantStringValue(authority));
			}
		}
		{
			Iterator<String> pathsItr = intentFilter.dataPathsIterator();
			while(pathsItr.hasNext())
			{
				String path = pathsItr.next();

				// By the source code of android.content.IntentFilter, it seems that Android framework won't 
				// encode the path.
				intentFilterVal.addDataPath(new ConstantStringValue(path), new IntValue(IntentFilterValue.PATTERN_LITERAL));
			}
		}
		{
			Iterator<String> pathPatsItr = intentFilter.dataPathPatternsIterator();
			while(pathPatsItr.hasNext())
			{
				ConcreteValue pathPatVal = ConstantStringValue.fromAndroidSimpleGlob(pathPatsItr.next());
				intentFilterVal.addDataPath(pathPatVal, new IntValue(IntentFilterValue.PATTERN_SIMPLE_GLOB));
			}
		}
		{
			Iterator<String> pathPrefixItr = intentFilter.dataPathPrefixsIterator();
			while(pathPrefixItr.hasNext())
			{
				ConcatValue pathPatVal = new ConcatValue(new ConstantStringValue(pathPrefixItr.next()), UnknownValue.getInstance());
				intentFilterVal.addDataPath(pathPatVal, new IntValue(IntentFilterValue.PATTERN_PREFIX));
			}
		}
		{
			Iterator<String> mimesItr = intentFilter.dataMimeTypesIterator();
			while(mimesItr.hasNext())
			{
				intentFilterVal.addDataMimeType(new ConstantStringValue(mimesItr.next()));
			}
		}
		return intentFilterVal;
	}
	protected UnitEntitiesInfo resolveUnits(Collection<EntryMethodInvoke> entryInvokes, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Resolving units", 1000);
			mLogger.debug("Resolving units");
			UnitsResolverContext resolverCtx = new UnitsResolverContext(mAnalysisCtx);
			setupResolverContext(entryInvokes, resolverCtx, new SubProgressMonitor(monitor, 50));
			InstVisitor instVisitor = new InstVisitor(resolverCtx, monitor);
			AnalysisScope scope = mAnalysisCtx.getAnalysisScope();
			CGNode node;
			while((node = resolverCtx.processNext()) != null)
			{
				IMethod method = node.getMethod();
				if(!resolverCtx.isScanned(node))
				{
					resolverCtx.setScanned(node);
					if(!method.isSynthetic())
					{
						IR ir = node.getIR();
						
						// For native methods, IR is possible to be null
						if(ir != null)
						{
							SSAInstruction[] insts = ir.getInstructions();
							for(int instIdx = 0; instIdx < insts.length; ++instIdx)
							{
								SSAInstruction inst = insts[instIdx];
								if(inst == null)
									continue;
								instVisitor.visit(inst, instIdx);
							}
						}
					}
				}
				Set<BehaviorMethod> comps = resolverCtx.getReachableMethods();
				Iterator<CGNode> succItr = 
					Iterators.concat(mAnalysisCtx.getCallGraph().getSuccNodes(node), resolverCtx.getExtraReachedNodes(node).iterator());
				while(succItr.hasNext())
				{
					CGNode succ = succItr.next();
					IMethod succMethod = succ.getMethod();
					ClassLoaderReference succLoaderRef = succ.getMethod().getDeclaringClass().getClassLoader().getReference();
					// If the method of the successor node is synthetic, then we should process it.
					// For example, Thread.start
					if(!succMethod.isSynthetic() && !succLoaderRef.equals(scope.getApplicationLoader()))
						continue;
					resolverCtx.addPendingNode(succ, comps);
				}
				monitor.worked(10);
			}
			UnitEntitiesInfo unitsInfo = new UnitEntitiesInfo();
			
			// Add the units other than component units
			for(UnitEntity unit : resolverCtx.getUnits())
			{
				if(!(unit instanceof ComponentUnit))
				{
					Collection<BehaviorMethod> methods = resolverCtx.getReachableMethods(unit);
					unitsInfo.addUnit(unit, methods);
				}
			}
			
			// Add the component units
			for(ComponentUnit comp : resolverCtx.getComponentUnits())
			{
				unitsInfo.addUnit(comp, null);
			}
			
			// Add the call-to-return relation
			unitsInfo.addCall2ReturnRelations(resolverCtx.getCall2ReturnRelations());
			unitsInfo.setNode2ReachableMap(resolverCtx.getNode2ReachableMethodsMap());
			
			// Attach the activity in-flows
			for(Statement inflow : resolverCtx.getActivityInFlows())
			{
				Set<BehaviorMethod> methods = resolverCtx.getReachableMethods(inflow.getNode());
				for(BehaviorMethod method : methods)
				{
					ComponentUnit comp = method.getComponent();
					if(!(comp instanceof ActivityUnit))
						continue;
					Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit> pair = comp.getEntryMethod(method.getSelector());
					if(pair == null)
						continue;
					ICCParamCalleeUnit param = pair.getLeft();
					if(!(param instanceof IMutableStatementOutflowUnit))
						continue;
					IMutableStatementOutflowUnit mutableParam = (IMutableStatementOutflowUnit)param;
					mutableParam.addOutflowStatement(inflow);
				}
			}
			
			// Attach the activity out-flows
			for(Statement outflow : resolverCtx.getActivityOutFlows())
			{
				Set<BehaviorMethod> methods = resolverCtx.getReachableMethods(outflow.getNode());
				for(BehaviorMethod method : methods)
				{
					ComponentUnit comp = method.getComponent();
					if(!(comp instanceof ActivityUnit))
						continue;
					Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit> pair = comp.getEntryMethod(method.getSelector());
					if(pair == null)
						continue;
					ICCReturnCalleeUnit ret = pair.getRight();
					if(!(ret instanceof IMutableStatementInflowUnit))
						continue;
					IMutableStatementInflowUnit mutableRet = (IMutableStatementInflowUnit)ret;
					mutableRet.addInflowStatement(outflow);
				}
			}
			return unitsInfo;
		}
		finally
		{
			monitor.done();
		}
	}
}
