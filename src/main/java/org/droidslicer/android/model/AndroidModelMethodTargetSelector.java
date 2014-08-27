package org.droidslicer.android.model;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.droidslicer.config.AbstractAnalysisConfig;
import org.droidslicer.util.InstructionsBuilder;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyMethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

public class AndroidModelMethodTargetSelector implements MethodTargetSelector
{
	//private final static int STATICS_PRINT_PERIOD = 100000000;
	private final static int MAX_CACHE_SIZE = 50000;
	protected final static int MAX_ALLOC_DEPTH = 2;
	//private int mStaticPrintCnt = 0;
	protected final Logger mLogger = LoggerFactory.getLogger(AndroidModelMethodTargetSelector.class);
	private final Map<MethodReference, MethodSummary> mMethodSummaries;
	private final IClassHierarchy mCha;
	private final MethodTargetSelector mParent;
	private final ClassHierarchyMethodTargetSelector mChaMethodTargetSelector;
	protected final AppModelClass mModelClass;
	protected final AbstractAnalysisConfig mConfig;
	protected final IClass mCtxWrapperClass;
	private final IClass mExecutorClass; 
	protected final LoadingCache<IMethod, IMethod> mSyntheticMethodsCache = CacheBuilder.newBuilder()
			.maximumSize(MAX_CACHE_SIZE)
			.concurrencyLevel(1)
			.softValues()
			//.recordStats()
			.build(new CacheLoader<IMethod, IMethod>()
			{
				private final Exception ex = new Exception("No summary");
				@Override
				public IMethod load(IMethod method) throws Exception				
				{
					MethodReference methodRef = method.getReference();
					MethodSummary summ;
					summ = findSummary(methodRef);
					if (summ == null)
					{
						IClass clazz = method.getDeclaringClass();
						
						// Do not ignore Class init methods, since it is usually used in initializing class fields
						if(clazz.getClassLoader().getReference().equals(ClassLoaderReference.Primordial))
						{
							// Don't ignore static methods, since it is often used to initialized static fields
							// TODO Maybe we should only considered the static methods that may be invoked by <clinit>
							if(method.isStatic())
								return method;

							// If the method is a non-static method, and it's not constructor or class
							// constructor, and the class of the method is in Android library
							if(!method.isStatic() && !method.isInit() && !method.isClinit())
							{
								// Don't bypass the methods in ContextWrapper
								if(clazz.equals(mCtxWrapperClass))
									return method;
								
								// The class of the method is a proper descendant of ContextWrapper
								if(mCha.isSubclassOf(clazz, mCtxWrapperClass))
								{	
									// If the method override the implementation of ContextWrapper
									IMethod superMethod = mCtxWrapperClass.getMethod(method.getSelector());
									if(superMethod != null)
									{
										// Directly use the implementation in ContextWrapper
										return superMethod;
									}
								}
							}
						}
						if(canIgnore(method))
						{
							summ = generateNoOp(method);
						}
						else
						{
							if(mParent instanceof ClassHierarchyMethodTargetSelector)
								return method;
							throw ex;
						}
					}
					return new SummarizedMethod(methodRef, summ, method.getDeclaringClass());
				}
			});;
	protected final Predicate<MethodReference> mIgnoreMethodPred;
	public AndroidModelMethodTargetSelector(MethodTargetSelector parent,
			Map<MethodReference, MethodSummary> methodSummaries,
			AppModelClass modelClass, Predicate<MethodReference> ignoreMethodPred, AbstractAnalysisConfig config, IClassHierarchy cha)
	{
		mParent = parent;
		mMethodSummaries = methodSummaries;
		mModelClass = modelClass;
		mIgnoreMethodPred = ignoreMethodPred;
		mConfig = config;
		mCha = cha;
		if(parent instanceof ClassHierarchyMethodTargetSelector)
			mChaMethodTargetSelector = (ClassHierarchyMethodTargetSelector)parent;
		else
			mChaMethodTargetSelector = new ClassHierarchyMethodTargetSelector(cha);
		mCtxWrapperClass = cha.lookupClass(TypeId.ANDROID_CONTEXT_WRAPPER.getTypeReference());
		mExecutorClass = cha.lookupClass(TypeId.EXECUTOR.getTypeReference());
		if(mCtxWrapperClass == null)
			throw new IllegalArgumentException("Fail to find " + TypeId.ANDROID_CONTEXT_WRAPPER.getTypeReference().getName() + " in class hierarchy");
		if(mExecutorClass == null)
			throw new IllegalArgumentException("Fail to find " + TypeId.EXECUTOR.getTypeReference().getName() + " in class hierarchy");
	}
	public Map<MethodReference, MethodSummary> getMethodSummaries()
	{
		return mMethodSummaries;
	}
	public MethodTargetSelector getParentSelector()
	{
		return mParent;
	}
	protected ClassHierarchyMethodTargetSelector getClassHierarchySelector()
	{
		return mChaMethodTargetSelector;
	}
	protected boolean canIgnore(IMethod method)
	{
		return mIgnoreMethodPred.apply(method.getReference());
	}
	protected MethodSummary findSummary(MethodReference m)
	{
		return mMethodSummaries.get(m);
	}
	@Override
	public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) 
	{
		if (site == null) 
			throw new IllegalArgumentException("site is null");
		/*if(mLogger.isDebugEnabled())
		{
			++mStaticPrintCnt;
			if(mStaticPrintCnt > STATICS_PRINT_PERIOD)
			{
				mStaticPrintCnt = 0;
				CacheStats stats = mSyntheticMethodsCache.stats();
				mLogger.debug("Cache size: {}, Max cache size: {}, Cache stats: hit rate: {}, total load time: {} sec, load success count: {}, load exception count: {}, request count: {}", 
						mSyntheticMethodsCache.size(),
						MAX_CACHE_SIZE,
						stats.hitRate(),
						stats.totalLoadTime() / (1000.0*1000*1000),
						stats.loadSuccessCount(),
						stats.loadExceptionCount(),
						stats.requestCount());				
			}
		}*/
		// first, see if we'd like to bypass the CHA-based target for the site	
		IMethod chaTarget = mChaMethodTargetSelector.getCalleeTarget(caller, site, receiver);
		if(chaTarget == null)
			return mParent.getCalleeTarget(caller, site, receiver);
		IMethod target = null;
		try
		{
			target = mSyntheticMethodsCache.get(chaTarget);
		}
		catch(ExecutionException ex)
		{}
		if (target != null)
		{
			return target;
		}
		else
		{
			target = mParent.getCalleeTarget(caller, site, receiver);
			if (target != null)
			{
				IMethod bypassTarget = null;
				try
				{
					bypassTarget = mSyntheticMethodsCache.get(target);
				}
				catch(ExecutionException ex)
				{}
				return (bypassTarget == null) ? target : bypassTarget;
			}
			else
				return target;
		}
	}
	public IClassHierarchy getClassHierarchy()
	{
		return mCha;
	}
	protected MethodSummary generateDefaultSummary(IMethod method)
	{
		IClass clazz = method.getDeclaringClass();
		IClassLoader classLoader = clazz.getClassLoader();	
		MethodReference methodRef = method.getReference();
		MethodSummary summary = new MethodSummary(methodRef);
		summary.setStatic(method.isStatic());
		TypeReference retTypeRef = methodRef.getReturnType();
		Language lang = classLoader.getLanguage();
		SSAInstructionFactory insts = lang.instructionFactory();
		int nParam = summary.getNumberOfParameters();
		InstructionsBuilder instsBuilder = new InstructionsBuilder(getClassHierarchy(), insts, summary.getConstants(), summary.getStatements().length, nParam + 1);
		if(classLoader.getReference().equals(mCha.getScope().getPrimordialLoader()))
		{
			for(int useIdx = (summary.isStatic() ? 0 : 1); useIdx < nParam; ++useIdx)
			{
				TypeReference paramTypeRef = summary.getParameterType(useIdx);
				if(!paramTypeRef.isClassType())
					continue;
					
				// Correct the class loader of the parameter type
				IClass paramType = mCha.lookupClass(paramTypeRef);
				if(paramType == null)
					continue;
				paramTypeRef = paramType.getReference();
				
				// If the parameter isn't a listener, skip it
				if(!mConfig.isListenerClass(paramTypeRef) && 
					!paramTypeRef.getName().equals(TypeId.ANDROID_RECEIVER.getTypeReference().getName()))
				{
					continue;
				}
				
				// Find the List static field in the app model class for this parameter type
				Atom fieldName = AppModelClass.getTypeListFieldName(paramTypeRef);
				TypeReference fieldTypeRef = AppModelClass.getTypeListFieldType();
				FieldReference fieldRef = FieldReference.findOrCreate(mModelClass.getReference(), fieldName, fieldTypeRef);
				int fieldValNum = instsBuilder.addLocal();
				
				// Add instruction to get the field (Expecting it to be a descendant of java.util.Collection)
				SSAGetInstruction getInst = insts.GetInstruction(fieldValNum, fieldRef);
				instsBuilder.addInstruction(getInst);
				int paramValNum = useIdx + Utils.FIRST_ARG_VAL_NUM;
				
				// Add instruction to add the listener into the list
				MethodReference addMethodRef = MethodId.COLLECTION_ADD_OBJ.getMethodReference();
				instsBuilder.addInstsInvocation(addMethodRef, new int[]{fieldValNum, paramValNum}, IInvokeInstruction.Dispatch.VIRTUAL);
			}
		}
		if(!retTypeRef.equals(TypeReference.Void))
		{
			// Is the method is non-static, and the return type is equal to 'this'
			if(!method.isStatic() && 
				retTypeRef.getName().equals(method.getDeclaringClass().getName()))
			{
				// Assume that the method return 'this', and add an instruction returning 
				// 'this'
				// TODO Should we try to look at the original method definition to check whether 
				// it really return 'this'?
				// The IR of the method can be obtained by
				// cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
				// See com.ibm.wala.core.tests.ir.DeterministicIRTest
				SSAReturnInstruction retInst = insts.ReturnInstruction(1, false);
				instsBuilder.addInstruction(retInst);
			}
			else
			{
				// Allocate a value for the return type, and return it
				int resultValNum = instsBuilder.addAllocation(retTypeRef, null, MAX_ALLOC_DEPTH, true);
				SSAReturnInstruction retInst = insts.ReturnInstruction(resultValNum, retTypeRef.isPrimitiveType());
				instsBuilder.addInstruction(retInst);
			}
		}
		for(SSAInstruction inst : instsBuilder.getInstructions())
		{
			summary.addStatement(inst);
		}
		return summary;
	}
	protected MethodSummary generateExecutorExecuteSummary(IMethod method)
	{
		IClass clazz = method.getDeclaringClass();
		IClassLoader classLoader = clazz.getClassLoader();
		MethodSummary summary = new MethodSummary(method.getReference());
		summary.setStatic(method.isStatic());
		Language lang = classLoader.getLanguage();
		SSAInstructionFactory insts = lang.instructionFactory();
		int nParam = summary.getNumberOfParameters();
		InstructionsBuilder instsBuilder = 
				new InstructionsBuilder(getClassHierarchy(), insts, summary.getConstants(), summary.getStatements().length, nParam + 1);
		
		// Add instruction for invoking the Runnable.run() method is the Runnable instance
		instsBuilder.addInstsInvocation(
				MethodId.RUNNABLE_RUN.getMethodReference(), new int[]{Utils.FIRST_ARG_VAL_NUM + 1}, IInvokeInstruction.Dispatch.VIRTUAL);
		for(SSAInstruction inst : instsBuilder.getInstructions())
		{
			summary.addStatement(inst);
		}
		return summary;
	}
	public MethodSummary generateNoOp(IMethod method)
	{
		if(method.getSelector().equals(MethodId.EXECUTOR_EXECUTE.getMethodReference().getSelector()))
		{
			IClass clazz = method.getDeclaringClass();
			if(mCha.isAssignableFrom(mExecutorClass, clazz) && !method.isStatic())
				return generateExecutorExecuteSummary(method);
		}
		return generateDefaultSummary(method);
	}
}
