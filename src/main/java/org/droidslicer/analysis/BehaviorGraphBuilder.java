package org.droidslicer.analysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.android.manifest.AndroidManifest;
import org.droidslicer.config.AbstractAnalysisConfig;
import org.droidslicer.config.IntentPermission;
import org.droidslicer.config.PathPermission;
import org.droidslicer.config.ProviderPermission;
import org.droidslicer.graph.BehaviorGraph;
import org.droidslicer.graph.BehaviorMethod;
import org.droidslicer.graph.entity.AppComponentUnit;
import org.droidslicer.graph.entity.CICCParamCalleeUnit;
import org.droidslicer.graph.entity.CICCReturnCalleeUnit;
import org.droidslicer.graph.entity.Call2ReturnRelation;
import org.droidslicer.graph.entity.ComponentReachRelation;
import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.DataDependencyRelation;
import org.droidslicer.graph.entity.FileInputUnit;
import org.droidslicer.graph.entity.FileOutputUnit;
import org.droidslicer.graph.entity.FileSystemDataRelation;
import org.droidslicer.graph.entity.ICCParamCalleeUnit;
import org.droidslicer.graph.entity.ICCParamCallerUnit;
import org.droidslicer.graph.entity.ICCReturnCalleeUnit;
import org.droidslicer.graph.entity.ICCReturnCallerUnit;
import org.droidslicer.graph.entity.IStatementFlowUnit;
import org.droidslicer.graph.entity.IntentCommRelation;
import org.droidslicer.graph.entity.IntentCommUnit;
import org.droidslicer.graph.entity.IntentFilterUnit;
import org.droidslicer.graph.entity.ProviderUnit;
import org.droidslicer.graph.entity.ReceiverUnit;
import org.droidslicer.graph.entity.RelationEntity;
import org.droidslicer.graph.entity.SQLiteDbInputUnit;
import org.droidslicer.graph.entity.SQLiteDbOutputUnit;
import org.droidslicer.graph.entity.ServiceUnit;
import org.droidslicer.graph.entity.SharedPreferencesUnit;
import org.droidslicer.graph.entity.SysICCParamCalleeUnit;
import org.droidslicer.graph.entity.SysICCReturnCalleeUnit;
import org.droidslicer.graph.entity.SysIntentCommUnit;
import org.droidslicer.graph.entity.UnitEntity;
import org.droidslicer.graph.entity.UriCommRelation;
import org.droidslicer.graph.entity.UriCommUnit;
import org.droidslicer.graph.entity.VirtualPermissionUnit;
import org.droidslicer.graph.entity.dependence.EntityDependencySolver;
import org.droidslicer.graph.entity.resolver.UnitEntitiesInfo;
import org.droidslicer.graph.entity.resolver.UnitsSolvingHelper;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.value.ConcatValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;
import org.droidslicer.value.IntentFilterValue;
import org.droidslicer.value.IntentValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.UnknownValue;
import org.droidslicer.value.UriValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;

public class BehaviorGraphBuilder
{
	private final static Logger mLogger = LoggerFactory.getLogger(BehaviorGraphBuilder.class);
	private final AndroidAnalysisContext mAnalysisCtx;
	public BehaviorGraphBuilder(AndroidAnalysisContext analysisCtx)
	{
		if(analysisCtx == null)
			throw new IllegalArgumentException();
		mAnalysisCtx = analysisCtx;
	}
	
	protected void addIntentCommEdges(BehaviorGraph graph, IntentCommUnit entity)
	{
		ConcreteValue intentVal = entity.getIntentValue();
		Class<? extends AppComponentUnit> targetEntityType = entity.getTargetEntityType();
		boolean possibleExplicit = IntentValue.isPossibleExplicit(intentVal);
		boolean possibleImplicit = IntentValue.isPossibleImplicit(intentVal);
		
		for(UnitEntity oEntity : graph.vertexSet())
		{
			if(!targetEntityType.isAssignableFrom(oEntity.getClass()))
				continue;
			AppComponentUnit oAppCompEntity = (AppComponentUnit)oEntity;
			boolean possibleTarget = false;

			// If it is possibly an explicit Intent
			if(possibleExplicit)
			{
				TypeName oTypeName = oAppCompEntity.getType().getName();
				if(IntentValue.isPossibleExpclicitMatch(intentVal, oTypeName))
				{
					possibleTarget = true;
				}
			}
			
			// If it is possibly an implicit Intent, and the component accepts IntentFilter
			if(!possibleTarget && possibleImplicit && oEntity instanceof IntentFilterUnit)
			{
				// Implicit target			
				IntentFilterUnit oIntentFilterEntity = (IntentFilterUnit)oEntity;
				ConcreteValue oIntentFilters = oIntentFilterEntity.getIntentFilterValues();
				oIntentFilters = NullValue.excludeNullValue(oIntentFilters);
				if(IntentFilterValue.isPossibleMatched(oIntentFilters, intentVal))
					possibleTarget = true;
			}
			if(possibleTarget)
				graph.addEdge(entity, oAppCompEntity, new IntentCommRelation());
		}
	}
	protected void addUriCommEdges(BehaviorGraph graph, UriCommUnit entity)
	{
		ConcreteValue uriVal = entity.getUriValue();
		MethodReference targetMethodRef = entity.getTargetMethod();
		TypeReference targetTypeRef = targetMethodRef.getDeclaringClass();
		IClassHierarchy cha = mAnalysisCtx.getClassHierarchy();
		IClass targetType = cha.lookupClass(targetTypeRef);
		if(targetType == null)
			throw new IllegalArgumentException("Fail to find type " + targetTypeRef + " in class hierarchy");
		for(UnitEntity oUnit : graph.vertexSet())
		{
			if(!(oUnit instanceof ProviderUnit))
				continue;
			ProviderUnit providerUnit = (ProviderUnit)oUnit;
			
			// Check if the ContentProvider's authority matches our target authority 
			ConcreteValue providerAuthVal = providerUnit.getAuthorityValue();
			ConcreteValue targetAuthVal = NullValue.excludeNullValue(UriValue.resolveAuthority(uriVal));
			
			// TODO To reduce false-positive, we currently exclude the URI communication with unsolved value
			targetAuthVal = UnknownValue.excludeUnknownValue(targetAuthVal);
			providerAuthVal = UnknownValue.excludeUnknownValue(providerAuthVal);
			if(targetAuthVal instanceof UnknownValue || providerAuthVal instanceof UnknownValue)
				continue;

			if(!ConstantStringValue.isPossibleMatched(providerAuthVal, targetAuthVal))
				continue;
			ConcreteValue pathVal = providerUnit.getPathValue();
			
			// If we can be sure that there's path requirement of the URI
			if(NullValue.isImpossibleNull(pathVal))
			{
				ConcreteValue targetPathVal = UriValue.resolvePath(uriVal);			
				if(!ConstantStringValue.isPossibleMatched(pathVal, targetPathVal))
					continue;
			}
			graph.addEdge(entity, providerUnit, new UriCommRelation());
		}
	}
	protected void addFileAliasEdges(BehaviorGraph graph, FileOutputUnit outputEntity)
	{
		for(UnitEntity oEntity : graph.vertexSet())
		{
			if(!(oEntity instanceof FileInputUnit))
				continue;
			
			// Check if the target entry component type match our target class
			FileInputUnit inputEntity = (FileInputUnit)oEntity;
			
			if(outputEntity.isPossibleAlias(inputEntity))
				graph.addEdge(outputEntity, inputEntity, new FileSystemDataRelation());
		}
	}
	protected void addDbAliasEdges(BehaviorGraph graph, SQLiteDbOutputUnit outputEntity)
	{
		for(UnitEntity oEntity : graph.vertexSet())
		{
			if(oEntity == outputEntity || !(oEntity instanceof SQLiteDbInputUnit))
				continue;
			
			// Check if the target entry component type match our target class
			SQLiteDbInputUnit inputEntity = (SQLiteDbInputUnit)oEntity;
			if(outputEntity.isPossibleAlias(inputEntity))
				graph.addEdge(outputEntity, inputEntity, new FileSystemDataRelation());
			// TODO Handle the case that file written using normal file API are accessed as 
			// database
		}
	}
	protected void addSharedPrefEdges(BehaviorGraph graph, SharedPreferencesUnit unit)
	{
		for(UnitEntity oEntity : graph.vertexSet())
		{
			if(oEntity == unit || !(oEntity instanceof SharedPreferencesUnit))
				continue;
			
			// Check if the target entry component type match our target class
			SharedPreferencesUnit oUnit = (SharedPreferencesUnit)oEntity;
			if(unit.isPossibleAlias(oUnit))
				graph.addEdge(unit, oUnit, new FileSystemDataRelation());
			// TODO Handle the case that file written using normal file API are accessed as 
			// shared-preferences
		}
	}
	protected static ConcreteValue convertPathPerm2Value(PathPermission pathPerm)
	{
		String pathPatStr = pathPerm.getPathPattern();
		switch(pathPerm.getPathPatternType())
		{
		case LITERAL:
			return new ConstantStringValue(pathPatStr);
		case SIMPLE_GLOB:
			return ConstantStringValue.fromAndroidSimpleGlob(pathPatStr);
		case PREFIX:
			return new ConcatValue(new ConstantStringValue(pathPatStr), UnknownValue.getInstance());
		default:
			throw new RuntimeException("Unreachable");
		}
	}
	protected void addComponentReachRelation(BehaviorGraph graph, ComponentUnit comp, UnitEntity unit, Selector selector)
	{
		RelationEntity relation = graph.getEdge(comp, unit);
		if(relation == null)
		{
			ComponentReachRelation reachRelation = new ComponentReachRelation();
			reachRelation.addSelector(selector);
			graph.addEdge(comp, unit, reachRelation);
		}
		else if(relation instanceof ComponentReachRelation)
		{
			ComponentReachRelation reachRelation = (ComponentReachRelation)relation;
			reachRelation.addSelector(selector);
		}
		else
			throw new IllegalArgumentException("Edges with component units as source can only be " + ComponentReachRelation.class.getSimpleName());
	}
	protected Set<String> filterDeclaredPermissions(Collection<String> perms)
	{
		Set<String> filteredPerms = new HashSet<String>();
		AndroidManifest manifest = mAnalysisCtx.getAppInfo().getManifest();
		for(String perm : perms)
		{
			boolean hasPerm = false;
			for(AndroidManifest.Permission declaredPerm : manifest.getPermissions())
			{
				if(declaredPerm.getName().equals(perm))
				{
					hasPerm = true;
					break;
				}
			}
			if(hasPerm)
				filteredPerms.add(perm);
		}
		return filteredPerms;
	}
	protected void buildSystemProviderReadVertices(BehaviorGraph graph, ProviderUnit providerUnit, MethodReference methodRef, Collection<String> readPerms)
	{
		// Add the vertex for provider unit if it hasn't been added
		graph.addVertex(providerUnit);
		CICCParamCalleeUnit paramUnit = new CICCParamCalleeUnit(methodRef);
		CICCReturnCalleeUnit retUnit = new CICCReturnCalleeUnit(methodRef);
		VirtualPermissionUnit permUnit = new VirtualPermissionUnit();
		for(String perm : readPerms)
			permUnit.addPermission(perm);
		graph.addVertex(paramUnit);
		graph.addVertex(retUnit);
		graph.addVertex(permUnit);
		graph.addEdge(permUnit, retUnit, new DataDependencyRelation());
		Selector selector = methodRef.getSelector();
		addComponentReachRelation(graph, providerUnit, paramUnit, selector);
		addComponentReachRelation(graph, providerUnit, permUnit, selector);
		addComponentReachRelation(graph, providerUnit, retUnit, selector);
		providerUnit.addEntryMethod(paramUnit, retUnit);
	}
	protected void buildSystemProviderWriteVertices(BehaviorGraph graph, ProviderUnit providerUnit, MethodReference methodRef, Collection<String> writePerms)
	{
		// Add the vertex for provider unit if it hasn't been added
		graph.addVertex(providerUnit);
		ICCParamCalleeUnit paramUnit = new SysICCParamCalleeUnit(methodRef);
		ICCReturnCalleeUnit retUnit = new SysICCReturnCalleeUnit(methodRef);
		VirtualPermissionUnit permUnit = new VirtualPermissionUnit();
		for(String perm : writePerms)
			permUnit.addPermission(perm);
		graph.addVertex(paramUnit);
		graph.addVertex(retUnit);
		graph.addVertex(permUnit);
		graph.addEdge(paramUnit, permUnit, new DataDependencyRelation());
		Selector selector = methodRef.getSelector();
		addComponentReachRelation(graph, providerUnit, paramUnit, selector);
		addComponentReachRelation(graph, providerUnit, permUnit, selector);
		addComponentReachRelation(graph, providerUnit, retUnit, selector);
		providerUnit.addEntryMethod(paramUnit, retUnit);
	}
	protected void buildSystemProviderVertices(
			BehaviorGraph graph, ConcreteValue authVal, ConcreteValue pathVal, Collection<String> readPerms, Collection<String> writePerms)
	{
		readPerms = filterDeclaredPermissions(readPerms);
		writePerms = filterDeclaredPermissions(writePerms);
		// Setup the provider component unit
		TypeReference typeRef = generateRandomType("SystemProvider_");
		ProviderUnit providerUnit = new ProviderUnit(typeRef);
		providerUnit.setAuthorityValue(authVal);
		providerUnit.setPathValue(pathVal);
		providerUnit.setSystemComponent(true);
		
		// ContentProvider read
		if(!readPerms.isEmpty())
		{
			{
				MethodReference methodRef = MethodReference.findOrCreate(typeRef, MethodId.ANDROID_PROVIDER_QUERY.getMethodReference().getSelector());
				buildSystemProviderReadVertices(graph, providerUnit, methodRef, readPerms);
			}
			{
				MethodReference methodRef = MethodReference.findOrCreate(typeRef, MethodId.ANDROID_PROVIDER_QUERY_CANCEL.getMethodReference().getSelector());
				buildSystemProviderReadVertices(graph, providerUnit, methodRef, readPerms);
			}
		}
		
		// ContentProvider write
		if(!writePerms.isEmpty())
		{
			{
				MethodReference methodRef = MethodReference.findOrCreate(typeRef, MethodId.ANDROID_PROVIDER_INSERT.getMethodReference().getSelector());
				buildSystemProviderWriteVertices(graph, providerUnit, methodRef, writePerms);
			}
			{
				MethodReference methodRef = MethodReference.findOrCreate(typeRef, MethodId.ANDROID_PROVIDER_BULK_INSERT.getMethodReference().getSelector());
				buildSystemProviderWriteVertices(graph, providerUnit, methodRef, writePerms);
			}
			{
				MethodReference methodRef = MethodReference.findOrCreate(typeRef, MethodId.ANDROID_PROVIDER_UPDATE.getMethodReference().getSelector());
				buildSystemProviderWriteVertices(graph, providerUnit, methodRef, writePerms);
			}
			{
				MethodReference methodRef = MethodReference.findOrCreate(typeRef, MethodId.ANDROID_PROVIDER_DELETE.getMethodReference().getSelector());
				buildSystemProviderWriteVertices(graph, providerUnit, methodRef, writePerms);
			}
		}
	}
	protected void buildSystemProviderVertices(BehaviorGraph graph)
	{
		AbstractAnalysisConfig config = mAnalysisCtx.getAnalysisConfig();
		for(ProviderPermission providerPerm : config.getProviderPermissions())
		{
			// Build authorities
			ConcreteValue authVal;
			{
				OrValue auths = new OrValue();
				for(String auth : providerPerm.getAuthories())
				{
					auths.addValue(new ConstantStringValue(auth));
				}
				if(auths.isEmpty())
					throw new IllegalArgumentException("Finding a system provider without any authority");
				authVal = auths.simplify();
			}
			Set<String> readPerms = new HashSet<String>();
			Set<String> writePerms = new HashSet<String>();
			{
				String readPerm = providerPerm.getReadPermission();
				if(readPerm != null)
					readPerms.add(readPerm);
			}
			{
				String writePerm = providerPerm.getWritePermission();
				if(writePerm != null)
					writePerms.add(writePerm);
			}
			buildSystemProviderVertices(graph, authVal, NullValue.getInstance(), readPerms, writePerms);
			
			// Set path
			for(PathPermission pathPerm : providerPerm.getPathPermissions())
			{
				ConcreteValue pathVal = convertPathPerm2Value(pathPerm);
				Set<String> pathReadPerms = new HashSet<String>();
				Set<String> pathWritePerms = new HashSet<String>();
				
				// Merge the read permission of the provider and the path
				pathReadPerms.addAll(readPerms);
				if(pathPerm.getReadPermission() != null)
					pathReadPerms.add(pathPerm.getReadPermission());
				
				// Merge the write permission of the provider and the path
				pathWritePerms.addAll(writePerms);
				if(pathPerm.getWritePermission() != null)
					pathWritePerms.add(pathPerm.getWritePermission());
				
				// Build the vertices for the read/write of the provider
				buildSystemProviderVertices(graph, authVal, pathVal, pathReadPerms, pathWritePerms);
			}
		}
	}
	protected TypeReference generateRandomType(String prefix)
	{
		IClassHierarchy cha = mAnalysisCtx.getClassHierarchy();
		AnalysisScope scope = mAnalysisCtx.getAnalysisScope();
		String pkgName = BehaviorGraphBuilder.class.getPackage().getName().replace('.', '/');
		TypeReference typeRef;
		do
		{
			typeRef = TypeReference.findOrCreate(scope.getPrimordialLoader(), 
					"L" + pkgName + "/" + prefix + ((int)(Math.random() * 1000000)));
		}while(cha.lookupClass(typeRef) != null);
		return typeRef;
	}
	protected void buildSystemReceiver(BehaviorGraph graph)
	{
		AbstractAnalysisConfig config = mAnalysisCtx.getAnalysisConfig();
		int receiverCnt = 0;
		for(IntentPermission intentPerm : config.getIntentPermissions())
		{
			VirtualPermissionUnit permUnit = new VirtualPermissionUnit();
			Set<String> filteredPerms = filterDeclaredPermissions(intentPerm.getSenderPermissions());
			if(filteredPerms.isEmpty())
				continue;
			for(String perm : filteredPerms)
				permUnit.addPermission(perm);
			
			++receiverCnt;
			TypeReference typeRef = generateRandomType("SystemReceiver" + receiverCnt + "_");
			ReceiverUnit unit = new ReceiverUnit(typeRef);
			unit.setSystemComponent(true);
			MethodReference receiverMethodRef = MethodReference.findOrCreate(typeRef, MethodId.ANDROID_RECEIVER_ON_RECEIVE.getMethodReference().getSelector());
			graph.addVertex(unit);
			graph.addVertex(permUnit);
			addComponentReachRelation(graph, unit, permUnit, receiverMethodRef.getSelector());
			
			SysICCParamCalleeUnit param = new SysICCParamCalleeUnit(receiverMethodRef);
			SysICCReturnCalleeUnit ret = new SysICCReturnCalleeUnit(receiverMethodRef);
			unit.addEntryMethod(param, ret);
			graph.addVertex(param);
			graph.addVertex(ret);
			addComponentReachRelation(graph, unit, param, receiverMethodRef.getSelector());
			addComponentReachRelation(graph, unit, ret, receiverMethodRef.getSelector());
			graph.addEdge(param, permUnit, new DataDependencyRelation());
		}
	}
	protected void buildSystemBroadcaster(BehaviorGraph graph)
	{
		TypeReference typeRef = generateRandomType("SystemBroadcaster_");
		
		ServiceUnit unit = new ServiceUnit(typeRef);
		unit.setSystemComponent(true);
		graph.addVertex(unit);
		MethodReference serviceMethodRef = MethodReference.findOrCreate(typeRef, MethodId.ANDROID_SERVICE_ON_CREATE.getMethodReference().getSelector());
		{
			SysICCParamCalleeUnit param = new SysICCParamCalleeUnit(serviceMethodRef);
			SysICCReturnCalleeUnit ret = new SysICCReturnCalleeUnit(serviceMethodRef);
			unit.addEntryMethod(param, ret);
			graph.addVertex(param);
			graph.addVertex(ret);
			addComponentReachRelation(graph, unit, param, serviceMethodRef.getSelector());
			addComponentReachRelation(graph, unit, ret, serviceMethodRef.getSelector());
		}

		AbstractAnalysisConfig config = mAnalysisCtx.getAnalysisConfig();
		
		for(IntentPermission intentPerm : config.getIntentPermissions())
		{
			// Add the vertex for getting data with the permissions
			Set<String> filteredPerms = filterDeclaredPermissions(intentPerm.getReceiverPermissions());
			if(filteredPerms.isEmpty())
				continue;
			VirtualPermissionUnit permUnit = new VirtualPermissionUnit();
			for(String perm : filteredPerms)
				permUnit.addPermission(perm);
			graph.addVertex(permUnit);
			addComponentReachRelation(graph, unit, permUnit, serviceMethodRef.getSelector());
			
			// Add the vertex for sending the data via broadcast
			SysIntentCommUnit iccUnit = new SysIntentCommUnit(ReceiverUnit.class);
			IntentValue intentVal = new IntentValue();
			intentVal.setIntentAction(new ConstantStringValue(intentPerm.getAction()));
			iccUnit.setIntentValue(intentVal);
			graph.addVertex(iccUnit);
			addComponentReachRelation(graph, unit, iccUnit, serviceMethodRef.getSelector());
			
			// Add the edge for the dependency from the permission use point to the ICC point
			graph.addEdge(permUnit, iccUnit, new DataDependencyRelation());
		}
	}
	protected UnitEntitiesInfo buildVertices(BehaviorGraph graph, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			if(monitor.isCanceled())
				throw CancelException.make("Operation canceled");
			monitor.beginTask("Building vertices", 100);
			UnitsSolvingHelper unitsSolver = new UnitsSolvingHelper(mAnalysisCtx);
			
			// Resolving the entities
			UnitEntitiesInfo unitsInfo;
			{
				monitor.subTask("Solving entities");
				mLogger.info("Solving entities");
				
				Stopwatch watch = Stopwatch.createStarted();
				
				unitsInfo = unitsSolver.solve(new SubProgressMonitor(monitor, 90));
				
				mLogger.info("Finished solving entities. Elapsed time: {}", watch);
			}
			
			// Add the units to the graph
			for(UnitEntity unit : unitsInfo.getUnits())
			{
				graph.addVertex(unit);
				if(unit instanceof ComponentUnit)
				{
					ComponentUnit compUnit = (ComponentUnit)unit;
					for(Pair<ICCParamCalleeUnit, ICCReturnCalleeUnit> pair : compUnit.getEntryMethods())
					{
						ICCParamCalleeUnit paramUnit = pair.getLeft();
						ICCReturnCalleeUnit retUnit = pair.getRight();
						graph.addVertex(paramUnit);
						graph.addVertex(retUnit);
						
						Selector selector = paramUnit.getMethod().getSelector();
						addComponentReachRelation(graph, compUnit, paramUnit, selector);
						addComponentReachRelation(graph, compUnit, retUnit, selector);
					}
				}
			}
			
			// Build the virtual units for the system provider
			buildSystemProviderVertices(graph);
			buildSystemBroadcaster(graph);
			buildSystemReceiver(graph);
					
			monitor.worked(10);
			return unitsInfo;
		}
		finally
		{
			monitor.done();
		}
	}
	protected Set<BehaviorMethod> getReachableMethodsForUnit(BehaviorGraph graph, UnitEntity unit)
	{
		Set<BehaviorMethod> result = new LinkedHashSet<BehaviorMethod>();
		for(RelationEntity relation : graph.incomingEdgesOf(unit))
		{
			if(!(relation instanceof ComponentReachRelation))
				continue;
			ComponentReachRelation compRel = (ComponentReachRelation)relation;
			UnitEntity srcUnit = graph.getEdgeSource(relation);
			if(!(srcUnit instanceof ComponentUnit))
				continue;
			ComponentUnit compUnit = (ComponentUnit)srcUnit;
			for(Selector selector : compRel.getSelectors())
				result.add(new BehaviorMethod(compUnit, selector));
		}
		return result;
	}
	protected Set<ComponentUnit> getReachableCompsForUnit(BehaviorGraph graph, UnitEntity unit)
	{
		Set<ComponentUnit> result = new LinkedHashSet<ComponentUnit>();
		for(RelationEntity relation : graph.incomingEdgesOf(unit))
		{
			if(!(relation instanceof ComponentReachRelation))
				continue;
			ComponentReachRelation compRel = (ComponentReachRelation)relation;
			UnitEntity srcUnit = graph.getEdgeSource(relation);
			if(!(srcUnit instanceof ComponentUnit))
				continue;
			result.add((ComponentUnit)srcUnit);
		}
		return result;
	}
	protected void buildEdges(BehaviorGraph graph, UnitEntitiesInfo unitsInfo, ProgressMonitor monitor)
		throws CancelException
	{
		try
		{
			monitor.beginTask("Building edges", 100);
			
			for(Pair<ICCParamCallerUnit, ICCReturnCallerUnit> pair : unitsInfo.getCall2ReturnRelation())
			{
				graph.addEdge(pair.getLeft(), pair.getRight(), new Call2ReturnRelation());
			}
			
			for(UnitEntity unit : unitsInfo.getUnits())
			{
				// Notice that we must only allow the {@link ICCRetCalleeUnit} to be reachable from the <component, selector>
				// of its corresponding one; otherwise, the data flow propagation result on the resulting behavior graph
				// will be incorrect.
				if(unit instanceof ComponentUnit || unit instanceof ICCParamCalleeUnit || unit instanceof ICCReturnCalleeUnit)
					continue;
				for(BehaviorMethod bMethod: unitsInfo.getReachableMethods(unit))
				{
					addComponentReachRelation(graph, bMethod.getComponent(), unit, bMethod.getSelector());
				}
			}

			mLogger.info("Solving dependencies...");
			monitor.subTask("Solving dependencies between entities");

			// Solve the entity dependencies
			EntityDependencySolver dependencySolver =
					new EntityDependencySolver(mAnalysisCtx, unitsInfo);
			for(UnitEntity unit : graph.vertexSet())
			{
				if(unit instanceof IStatementFlowUnit)
				{
					IStatementFlowUnit stmEntity = (IStatementFlowUnit)unit;
					dependencySolver.addEntity(stmEntity);
				}
			}
			
			// Solving dependencies between entities
			{
				ProgressMonitor subMonitor = new SubProgressMonitor(monitor, 55);
				
				Stopwatch watch = Stopwatch.createStarted();
				
				dependencySolver.solve(subMonitor);
				
				mLogger.info("Finished solving dependencies. Elapsed time: {}", watch);
			}
			
			// Add edges for the data dependencies
			{
				mLogger.info("Adding edges for data dependencies");
				Stopwatch watch = Stopwatch.createStarted();
				MutableIntSet unionFacts = new BitVectorIntSet();
				for(IStatementFlowUnit dstUnit : dependencySolver.getEntities())
				{
					if(!(dstUnit instanceof UnitEntity))
						continue;
					unionFacts.clear();					
					for(Statement stm : dstUnit.getInflowStatements())
					{
						IntSet facts = dependencySolver.getStatementFacts(stm);
						unionFacts.addAll(facts);
					}
					Set<BehaviorMethod> dstReachableMethods = null;
					for(EntityDependencySolver.Dependence entry : dependencySolver.getDepends(unionFacts))
					{
						IStatementFlowUnit srcUnit = entry.getUnit();
						if(!(srcUnit instanceof UnitEntity))
							continue;
						
						// Filter out the obvious incorrect dependency
						// It can also improve efficiency in complicated application
						if(srcUnit instanceof ICCParamCalleeUnit && dstUnit instanceof ICCReturnCalleeUnit)
						{
							ICCParamCalleeUnit paramUnit = (ICCParamCalleeUnit)srcUnit;
							ICCReturnCalleeUnit retUnit = (ICCReturnCalleeUnit)dstUnit;
							TypeName paramClassName = paramUnit.getMethod().getDeclaringClass().getName();
							TypeName retClassName = retUnit.getMethod().getDeclaringClass().getName();
							if(!paramClassName.equals(retClassName))
								continue;
						}
						Set<CGNode> heapChannelNodes = entry.getChannelNodes();
						if(heapChannelNodes.isEmpty())
						{
							graph.addEdge((UnitEntity)srcUnit, (UnitEntity)dstUnit, new DataDependencyRelation());
							continue;
						}
						if(dstReachableMethods == null)
							dstReachableMethods = getReachableMethodsForUnit(graph, (UnitEntity)dstUnit);
						final Set<ComponentUnit> srcReachableComps = getReachableCompsForUnit(graph, (UnitEntity)srcUnit);
						Set<BehaviorMethod> conds = new LinkedHashSet<BehaviorMethod>();
						for(CGNode channelNode : heapChannelNodes)
						{
							// TODO To reduce false-positive, we currently only allow data-flow to cross 
							// ICC entry points within a same component. We should do better.
							Set<BehaviorMethod> methods = Sets.filter(
									unitsInfo.getReachableMethodsForNode(channelNode), 
									new Predicate<BehaviorMethod>()
							{
								@Override
								public boolean apply(BehaviorMethod method)
								{
									return srcReachableComps.contains(method.getComponent());
								}
							});
							for(BehaviorMethod method : methods)
							{
								if(dstReachableMethods.contains(method))
									conds.add(method);
							}
						}
						graph.addEdge((UnitEntity)srcUnit, (UnitEntity)dstUnit, new DataDependencyRelation(conds));
					}
				}
				mLogger.info("Elapsed time for finding edges for data dependencies: {}", watch);
			}
			
			// Find implicit relationship
			{
				mLogger.info("Adding edges for implicit relationship");
				Stopwatch watch = Stopwatch.createStarted();
				for(UnitEntity entity : graph.vertexSet())
				{
					if(entity instanceof IntentCommUnit)
					{
						addIntentCommEdges(graph, (IntentCommUnit)entity);
					}
					else if(entity instanceof UriCommUnit)
					{
						addUriCommEdges(graph, (UriCommUnit)entity);
					}
					else if(entity instanceof FileOutputUnit)
					{
						addFileAliasEdges(graph, (FileOutputUnit)entity);
					}
					else if(entity instanceof SQLiteDbOutputUnit)
					{
						addDbAliasEdges(graph, (SQLiteDbOutputUnit)entity);
					}
					else if(entity instanceof SharedPreferencesUnit)
					{
						addSharedPrefEdges(graph, (SharedPreferencesUnit)entity);
					}
				}
				mLogger.info("Elapsed time for finding edges for implicit relationship: {}", watch);
			}
		}
		finally
		{
			monitor.done();
		}
	}
	public BehaviorGraph build(ProgressMonitor monitor)
		throws CancelException
	{
		if(monitor == null)
			throw new IllegalArgumentException();
		try
		{
			monitor.beginTask("Building behavior graph", 100);
			BehaviorGraph graph = new BehaviorGraph();
			
			// Build the vertices
			UnitEntitiesInfo unitsInfo = buildVertices(graph, new SubProgressMonitor(monitor, 50));
			
			// Build the edges
			buildEdges(graph, unitsInfo, new SubProgressMonitor(monitor, 50));
			
			return graph;
		}
		finally
		{
			monitor.done();
		}
	}
}
