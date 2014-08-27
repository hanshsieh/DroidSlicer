package org.droidslicer.value.solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;
import org.droidslicer.value.AndroidUriBuilderValue;
import org.droidslicer.value.CharValue;
import org.droidslicer.value.ConcatValue;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;
import org.droidslicer.value.FileInputStreamValue;
import org.droidslicer.value.FileOutputStreamValue;
import org.droidslicer.value.FileValue;
import org.droidslicer.value.InetAddressLocalHostValue;
import org.droidslicer.value.InetAddressLoopbackValue;
import org.droidslicer.value.InetAddressValue;
import org.droidslicer.value.InetSocketAddressValue;
import org.droidslicer.value.IntValue;
import org.droidslicer.value.NullValue;
import org.droidslicer.value.OrValue;
import org.droidslicer.value.SQLiteDbValue;
import org.droidslicer.value.SQLiteOpenHelperValue;
import org.droidslicer.value.SharedPreferencesValue;
import org.droidslicer.value.SocketValue;
import org.droidslicer.value.UnknownValue;
import org.droidslicer.value.UriValue;
import org.droidslicer.value.UrlConnectionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;

public class SliceValueSolver extends ConcreteValueSolver
{
	private static class Context
	{
		private final static int MAX_CACHE_SIZE = 20000;
		public ValueSourceSolver valSrcSolver;
		public Set<ValueKey> valsOnStack = new HashSet<ValueKey>();
		public Cache<ValueKey, Pair<ConcreteValue, Integer>> valuesCache = 
				CacheBuilder.newBuilder()
				.maximumSize(MAX_CACHE_SIZE)
				.concurrencyLevel(1)
				.softValues()
				.build();				
	}
	private static class ValueKey
	{
		private final Statement mStm;
		private final CGNode mEndNode;
		private final int mEndInstIdx;
		private final IClass mExpectType;
		private int mHash = -1;
		public ValueKey(Statement stm, CGNode endNode, int endInstIdx, IClass expectType)
		{
			// We allow expect type to be null
			if(stm == null || endNode == null || endInstIdx < 0)
				throw new IllegalArgumentException();
			mStm = stm;
			mEndNode = endNode;
			mEndInstIdx = endInstIdx;
			mExpectType = expectType;
		}
		@Override
		public int hashCode()
		{
			if(mHash == -1)
			{
				mHash = mStm.hashCode();
				mHash += mEndNode.hashCode();
				mHash += mEndInstIdx;
				mHash += mExpectType == null ? 82421 : mExpectType.hashCode();
				if(mHash == -1)
					mHash = 85297;
			}
			return mHash;
		}
		@Override
		public boolean equals(Object other)
		{
			if(this == other)
				return true;
			if(!(other instanceof ValueKey))
				return false;
			ValueKey that = (ValueKey)other;
			if(!mStm.equals(that.mStm) || 
				!mEndNode.equals(that.mEndNode) ||
				mEndInstIdx != that.mEndInstIdx)
			{
				return false;
			}
			if(mExpectType == null)
			{
				if(that.mExpectType != null)
					return false;
			}
			else if(!mExpectType.equals(that.mExpectType))
				return false;
			return true;
		}
	}
	public class CallResultSolver
	{
		private Map<Pair<TypeName, Selector>, CallResultSolverEntry> mEntries
			= new HashMap<Pair<TypeName, Selector>, CallResultSolverEntry>();
		public void addEntry(MethodReference method, CallResultSolverEntry entry)
		{
			mEntries.put(Pair.of(method.getDeclaringClass().getName(), method.getSelector()), entry);
		}
		public void addEntryForAllDescendants(MethodReference method, CallResultSolverEntry entry)
		{
			TypeReference clazzRef = method.getDeclaringClass();
			IClassHierarchy cha = getAnalysisContext().getClassHierarchy();
			IClass clazz = cha.lookupClass(clazzRef);
			if(clazz == null)
				throw new IllegalArgumentException("Fail to find class " + clazz + " in class hierarchy");
			Set<IClass> subclazzes = Utils.computeAllSubclassesOrImplementors(cha, clazz, new Predicate<IClass>()
			{
				@Override
				public boolean apply(IClass clazz)
				{
					ClassLoaderReference clazzLoaderRef = clazz.getClassLoader().getReference();
					if(!clazzLoaderRef.equals(ClassLoaderReference.Primordial) || 
						clazz.isPrivate() || 
						clazz.isArrayClass() || 
						clazz.getReference().equals(TypeId.ANDROID_CONTEXT_WRAPPER.getTypeReference()))
					{
						return false;
					}
					return true;
				}
			});
			for(IClass subclazz : subclazzes)
			{
				MethodReference subMethodRef = MethodReference.findOrCreate(subclazz.getReference(), method.getSelector());
				addEntry(subMethodRef, entry);
			}
		}
		public CallResultSolverEntry getEntry(MethodReference methodRef)
		{
			return mEntries.get(Pair.of(methodRef.getDeclaringClass().getName(), methodRef.getSelector()));
		}
		public ConcreteValue solve(
				Context ctx, 
				NormalReturnCaller retCaller, 
				CGNode calleeNode, 
				CGNode endNode, 
				int endInstIdx, 
				IClass expectType, 
				int maxDepth,
				ProgressMonitor monitor)
			throws CancelException
		{
			try
			{
				CGNode callerNode = retCaller.getNode();
				SSAAbstractInvokeInstruction invokeInst = retCaller.getInstruction();
				MethodReference calleeMethodRef;
				if(calleeNode == null)
					calleeMethodRef = invokeInst.getDeclaredTarget();
				else
					calleeMethodRef = calleeNode.getMethod().getReference();
				int callerInstIdx = retCaller.getInstructionIndex();
 				CallResultSolverEntry entry = getEntry(calleeMethodRef);
				if(entry != null)
					return entry.solve(ctx, callerNode, callerInstIdx, invokeInst, endNode, endInstIdx, maxDepth, monitor);
				else
				{
					// If the call node returns 'this', then continue to track the value of the 'this' parameter
					// passed to the callee.
					if(calleeNode != null && Utils.isReturnThis(calleeNode))
					{
						int thisValNum = invokeInst.getUse(0);
						
						// Notice that the end node and end instruction index should be the original ones, not the current invoke instruction.
						ConcreteValue val = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, thisValNum), endNode, endInstIdx, expectType, maxDepth, monitor);
						return val;
					}
							
					return UnknownValue.getInstance();
				}
			}
			finally
			{
				monitor.done();
			}
		}
	}
	public abstract class CallResultSolverEntry
	{
		private final IntSet mTrackedParams;
		public CallResultSolverEntry(int[] trackedParams)
		{
			MutableIntSet trackedParamsSet = new BitVectorIntSet();
			for(int param : trackedParams)
			{
				trackedParamsSet.add(param);
			}
			mTrackedParams = trackedParamsSet;
		}
		public boolean shouldTrackParameter(int paramIdx)
		{
			return mTrackedParams.contains(paramIdx);
		}
		public abstract ConcreteValue solve(
				Context ctx, 
				CGNode callerNode, 
				int callerInstIdx, 
				SSAAbstractInvokeInstruction invokeInst,
				CGNode endNode,
				int endInstIdx,
				int maxDepth, 
				ProgressMonitor monitor)
			throws CancelException;
	}
	private final static Logger mLogger = LoggerFactory.getLogger(SliceValueSolver.class);
	private final CallResultSolver mCallResultSolver = new CallResultSolver();
	private final IClass 
		mStrBuilderClass, 
		mFileClass, 
		mStrClass, 
		mAndroidIntentClass, 
		mClassClass, 
		mObjClass, 
		mFileInputStreamClass, 
		mFileOutputStreamClass,
		mDbOpenHelperClass, 
		mSqliteDbClass,
		mAndroidUriClass,
		mUriBuilderClass,
		mHttpHostClass,
		mHttpRequestClass,
		mUriClass,
		mUrlClass,
		mUrlConnClass,
		mInetSocketAddrClass,
		mSocketClass,
		mServerSocketClass,
		mInetAddrClass,
		mIntentFilterClass,
		mCompNameClass,
		mAndroidContextClass,
		mSocketFactoryClass,
		mAndroidPrefMgrClass,
		mAndroidAssetMgrClass,
		mAndroidEnvClass;
	public SliceValueSolver(
			AndroidAnalysisContext analysisCtx)
	{
		super(analysisCtx);		
		IClassHierarchy cha = analysisCtx.getClassHierarchy();
		mStrBuilderClass = cha.lookupClass(TypeReference.JavaLangStringBuilder);
		mFileClass = cha.lookupClass(TypeId.FILE.getTypeReference());
		mStrClass = cha.lookupClass(TypeReference.JavaLangString);
		mAndroidIntentClass = cha.lookupClass(TypeId.ANDROID_INTENT.getTypeReference());
		mClassClass = cha.lookupClass(TypeReference.JavaLangClass);
		mObjClass = cha.lookupClass(TypeReference.JavaLangObject);
		mFileInputStreamClass = cha.lookupClass(TypeId.FILE_INPUT_STREAM.getTypeReference());
		mFileOutputStreamClass = cha.lookupClass(TypeId.FILE_OUTPUT_STREAM.getTypeReference());
		mDbOpenHelperClass = cha.lookupClass(TypeId.ANDROID_SQLITE_OPEN_HELPER.getTypeReference());
		mSqliteDbClass = cha.lookupClass(TypeId.ANDROID_SQLITE_DB.getTypeReference());
		mAndroidUriClass = cha.lookupClass(TypeId.ANDROID_URI.getTypeReference());
		mUriBuilderClass = cha.lookupClass(TypeId.ANDROID_URI_BUILDER.getTypeReference());
		mHttpHostClass = cha.lookupClass(TypeId.APACHE_HTTP_HOST.getTypeReference());
		mHttpRequestClass = cha.lookupClass(TypeId.APACHE_HTTP_REQUEST.getTypeReference());
		mUriClass = cha.lookupClass(TypeId.URI.getTypeReference());
		mUrlClass = cha.lookupClass(TypeId.URL.getTypeReference());
		mUrlConnClass = cha.lookupClass(TypeId.URL_CONNECTION.getTypeReference());
		mInetSocketAddrClass = cha.lookupClass(TypeId.INET_SOCKET_ADDRESS.getTypeReference());
		mSocketClass = cha.lookupClass(TypeId.SOCKET.getTypeReference());
		mServerSocketClass = cha.lookupClass(TypeId.SERVER_SOCKET.getTypeReference());
		mInetAddrClass = cha.lookupClass(TypeId.INET_ADDRESS.getTypeReference());
		mIntentFilterClass = cha.lookupClass(TypeId.ANDROID_INTENT_FILTER.getTypeReference());
		mCompNameClass = cha.lookupClass(TypeId.ANDROID_COMPONENT_NAME.getTypeReference());
		mAndroidContextClass = cha.lookupClass(TypeId.ANDROID_CONTEXT.getTypeReference());
		mSocketFactoryClass = cha.lookupClass(TypeId.SOCKET_FACTORY.getTypeReference());
		mAndroidPrefMgrClass = cha.lookupClass(TypeId.ANDROID_PREFERENCE_MGR.getTypeReference());
		mAndroidAssetMgrClass = cha.lookupClass(TypeId.ANDROID_ASSET_MGR.getTypeReference());
		mAndroidEnvClass = cha.lookupClass(TypeId.ANDROID_ENVIRONMENT.getTypeReference());
		if(mStrBuilderClass == null || 
				mFileClass == null || 
				mStrClass == null || 
				mAndroidIntentClass == null || 
				mClassClass == null || 
				mObjClass == null || 
				mFileInputStreamClass == null ||
				mFileOutputStreamClass == null || 
				mDbOpenHelperClass == null ||
				mSqliteDbClass == null ||
				mAndroidUriClass == null ||
				mUriBuilderClass == null ||
				mHttpHostClass == null ||
				mHttpRequestClass == null ||
				mUriClass == null ||
				mUrlClass == null ||
				mUrlConnClass == null ||
				mInetSocketAddrClass == null ||
				mSocketClass == null ||
				mServerSocketClass == null || 
				mIntentFilterClass == null ||
				mCompNameClass == null ||
				mAndroidContextClass == null || 
				mSocketFactoryClass == null || 
				mAndroidPrefMgrClass == null || 
				mAndroidAssetMgrClass == null || 
				mAndroidEnvClass == null)
			throw new IllegalArgumentException("Fail to lookup some class in class hierarchy");
		mCallResultSolver.addEntryForAllDescendants(MethodId.IO_FILE_SYSTEM_GET_SEPARATOR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				return new CharValue('/');
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.IO_FILE_SYSTEM_GET_PATH_SEPERATOR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				return new CharValue(':');
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.FILE_SYSTEM_GET_SEPARATOR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				return new ConstantStringValue("/");
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.STR_VALUE_OF_OBJ.getMethodReference(), new CallResultSolverEntry(new int[]{0})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int objValNum = invokeInst.getUse(0);
				ConcreteValue val = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, objValNum), callerNode, callerInstIdx, mObjClass, maxDepth, monitor);
				val = NullValue.excludeNullValue(val);
				ConcreteValue result = val.getStringValue();
				return result;
			}
		});
		{
			CallResultSolverEntry entryStrValOfPrimitive = new CallResultSolverEntry(new int[]{0})
			{
				@Override
				public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
						SSAAbstractInvokeInstruction invokeInst,
						CGNode endNode, int endInstIdx,
						int maxDepth,
						ProgressMonitor monitor)
					throws CancelException
				{
					int objValNum = invokeInst.getUse(0);

					// TODO Restrict the data type
					ConcreteValue val = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, objValNum), callerNode, callerInstIdx, null, maxDepth, monitor);
					val = NullValue.excludeNullValue(val);
					ConcreteValue result = val.getStringValue();
					return result;
				}
			};
			mCallResultSolver.addEntryForAllDescendants(MethodId.STR_VALUE_OF_BOOL.getMethodReference(), entryStrValOfPrimitive);
			mCallResultSolver.addEntryForAllDescendants(MethodId.STR_VALUE_OF_CHAR.getMethodReference(), entryStrValOfPrimitive);
			mCallResultSolver.addEntryForAllDescendants(MethodId.STR_VALUE_OF_DOUBLE.getMethodReference(), entryStrValOfPrimitive);
			mCallResultSolver.addEntryForAllDescendants(MethodId.STR_VALUE_OF_FLOAT.getMethodReference(), entryStrValOfPrimitive);
			mCallResultSolver.addEntryForAllDescendants(MethodId.STR_VALUE_OF_INT.getMethodReference(), entryStrValOfPrimitive);
			mCallResultSolver.addEntryForAllDescendants(MethodId.STR_VALUE_OF_LONG.getMethodReference(), entryStrValOfPrimitive);
		}
		{
			CallResultSolverEntry entrySysGetProp = new CallResultSolverEntry(new int[]{0})
			{
				@Override
				public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
						SSAAbstractInvokeInstruction invokeInst,
						CGNode endNode, int endInstIdx,
						int maxDepth,
						ProgressMonitor monitor)
					throws CancelException
				{
					int nameValNum = invokeInst.getUse(0);

					// TODO Restrict the data type
					ConcreteValue nameVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, nameValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
					Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(nameVal);
					OrValue result = new OrValue();
					while(itr.hasNext())
					{
						ConcreteValue singleVal = itr.next();
						if(singleVal instanceof ConstantStringValue)
						{
							String str = ((ConstantStringValue)singleVal).getValue();
							if(str.equals("file.separator"))
								result.addValue(new ConstantStringValue("/"));
							else if(str.equals("path.separator"))
								result.addValue(new ConstantStringValue(":"));
							else
								result.addValue(UnknownValue.getInstance());
							// TODO Handle more cases
						}
						else
							result.addValue(UnknownValue.getInstance());
					}
					return result.simplify();
				}
			};
			mCallResultSolver.addEntryForAllDescendants(MethodId.SYSTEM_GET_PROPERTY.getMethodReference(), entrySysGetProp);
			mCallResultSolver.addEntryForAllDescendants(MethodId.SYSTEM_GET_PROPERTY_DEFAULT.getMethodReference(), entrySysGetProp);
		}
		mCallResultSolver.addEntryForAllDescendants(MethodId.STR_CHAR_AT.getMethodReference(), new CallResultSolverEntry(new int[]{0, 1})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int strValNum = invokeInst.getUse(0);
				int idxValNum = invokeInst.getUse(1);
				ConcreteValue strVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, strValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				strVal = NullValue.excludeNullValue(strVal);

				// TODO Restrict the type
				ConcreteValue idxVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, idxValNum), callerNode, callerInstIdx, null, maxDepth, monitor);
				Iterator<ConcreteValue> strValsItr = OrValue.getSingleValueIterator(strVal);
				OrValue result = new OrValue();
				Set<Integer> idxVals = new LinkedHashSet<Integer>();
				Iterator<ConcreteValue> idxValsItr = OrValue.getSingleValueIterator(idxVal);
				while(idxValsItr.hasNext())
				{
					ConcreteValue idxValSingle = idxValsItr.next();
					if(idxValSingle instanceof IntValue)
					{
						int idx = ((IntValue)idxValSingle).getValue();
						idxVals.add(idx);
					}
					else
						result.addValue(UnknownValue.getInstance());
				}
				while(strValsItr.hasNext())
				{
					ConcreteValue strValSingle = strValsItr.next();
					if(strValSingle instanceof ConstantStringValue)
					{
						String str = ((ConstantStringValue)strValSingle).getValue();
						boolean hasIndex = false;
						for(int idx : idxVals)
						{
							if(idx >= 0 && idx < str.length())
							{
								result.addValue(new CharValue(str.charAt(idx)));
								hasIndex = true;
							}
						}
						if(!hasIndex)
							result.addValue(UnknownValue.getInstance());
					}
					else
						result.addValue(UnknownValue.getInstance());
				}
				return result.simplify();
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.STR_CONCAT.getMethodReference(), new CallResultSolverEntry(new int[]{0, 1})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int str1ValNum = invokeInst.getUse(0);
				int str2ValNum = invokeInst.getUse(1);
				ConcreteValue val1 = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, str1ValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				ConcreteValue val2 = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, str2ValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				ConcreteValue result = new ConcatValue(NullValue.excludeNullValue(val1), val2);
				return result;
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.STR_BUILDER_TO_STR.getMethodReference(), new CallResultSolverEntry(new int[]{0})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int builderValNum = invokeInst.getUse(0);
				ConcreteValue strBuilderVal = solveValue(
						ctx, new ParamCaller(callerNode, callerInstIdx, builderValNum), callerNode, callerInstIdx, mStrBuilderClass, maxDepth, monitor);
				ConcreteValue result = NullValue.excludeNullValue(strBuilderVal).getStringValue();
				return result;
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.FILE_GET_PATH.getMethodReference(), new CallResultSolverEntry(new int[]{0})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int fileValNum = invokeInst.getUse(0);
				ConcreteValue fileVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, fileValNum), callerNode, callerInstIdx, mFileClass, maxDepth, monitor);
				fileVal = NullValue.excludeNullValue(fileVal);
				return FileValue.resolvePath(fileVal);
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.FILE_GET_ABSOLUTE_PATH.getMethodReference(), new CallResultSolverEntry(new int[]{0})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int fileValNum = invokeInst.getUse(0);
				ConcreteValue fileVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, fileValNum), callerNode, callerInstIdx, mFileClass, maxDepth, monitor);
				fileVal = NullValue.excludeNullValue(fileVal);
				return FileValue.resolveAbsolutePath(fileVal);
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_URI_BUILDER_BUILD.getMethodReference(), new CallResultSolverEntry(new int[]{0})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int uriBuilderValNum = invokeInst.getUse(0);
				ConcreteValue uriBuilderVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, uriBuilderValNum), callerNode, callerInstIdx, mUriBuilderClass, maxDepth, monitor);
				uriBuilderVal = NullValue.excludeNullValue(uriBuilderVal);
				return getUriFromUriBuilder(uriBuilderVal);
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_URI_PARSE.getMethodReference(), new CallResultSolverEntry(new int[]{0})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int uriStrValNum = invokeInst.getUse(0);
				ConcreteValue uriStrVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, uriStrValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				uriStrVal = NullValue.excludeNullValue(uriStrVal);
				return new UriValue(uriStrVal);
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_URI_WITH_APPENDED_PATH.getMethodReference(), new CallResultSolverEntry(new int[]{0, 1})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int uriValNum = invokeInst.getUse(0);
				int pathSegValNum = invokeInst.getUse(1);
				ConcreteValue uriVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, uriValNum), callerNode, callerInstIdx, mAndroidUriClass, maxDepth, monitor);
				ConcreteValue pathSegVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, pathSegValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				uriVal = NullValue.excludeNullValue(uriVal);
				pathSegVal = NullValue.excludeNullValue(pathSegVal);
				return UriValue.withAppendedEncodedPath(uriVal, pathSegVal);
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_URI_FROM_FILE.getMethodReference(), new CallResultSolverEntry(new int[]{0})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int fileValNum = invokeInst.getUse(0);
				ConcreteValue fileVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, fileValNum), callerNode, callerInstIdx, mFileClass, maxDepth, monitor);
				fileVal = NullValue.excludeNullValue(fileVal);
				return UriValue.fromFile(fileVal);
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_URI_FROM_PARTS.getMethodReference(), new CallResultSolverEntry(new int[]{0, 1, 2})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int schemeValNum = invokeInst.getUse(0);
				int sspValNum = invokeInst.getUse(1);
				int fragValNum = invokeInst.getUse(2);
				ConcreteValue schemeVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, schemeValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				ConcreteValue sspVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, sspValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				ConcreteValue fragVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, fragValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				schemeVal = NullValue.excludeNullValue(schemeVal);
				sspVal = NullValue.excludeNullValue(sspVal);
				fragVal = NullValue.excludeNullValue(fragVal);
				return new UriValue(schemeVal, sspVal, fragVal);
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_URI_BUILD_UPON.getMethodReference(), new CallResultSolverEntry(new int[]{0})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int uriValNum = invokeInst.getUse(0);
				ConcreteValue uriVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, uriValNum), callerNode, callerInstIdx, mAndroidUriClass, maxDepth, monitor);
				uriVal = NullValue.excludeNullValue(uriVal);
				AndroidUriBuilderCFValueSolver uriBuilderSolver = 
						new AndroidUriBuilderCFValueSolver(SliceValueSolver.this, callerNode, callerInstIdx, invokeInst.getDef(), new ValueUsageWalker.FixPointEndCriterion(endNode, endInstIdx), maxDepth);
				uriBuilderSolver.setInitialValue(new AndroidUriBuilderValue(uriVal));
				try
				{
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
					uriBuilderSolver.run(subMonitor);
				}
				finally
				{
					monitor.setSubProgressMonitor(null);
				}
				return uriBuilderSolver.getValue();
			}
		});
		{
			CallResultSolverEntry entryUrlOpenConn = new CallResultSolverEntry(new int[]{0})
			{
				@Override
				public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
						SSAAbstractInvokeInstruction invokeInst,
						CGNode endNode, int endInstIdx,
						int maxDepth,
						ProgressMonitor monitor)
					throws CancelException
				{
					int urlValNum = invokeInst.getUse(0);
					ConcreteValue urlVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, urlValNum), callerNode, callerInstIdx, mUrlClass, maxDepth, monitor);
					urlVal = NullValue.excludeNullValue(urlVal);
					return new UrlConnectionValue(urlVal);
				}
			};
			mCallResultSolver.addEntryForAllDescendants(MethodId.URL_OPEN_CONNECTION_PROXY.getMethodReference(), entryUrlOpenConn);
			mCallResultSolver.addEntryForAllDescendants(MethodId.URL_OPEN_CONNECTION.getMethodReference(), entryUrlOpenConn);
		}
		
		// TODO Handle InetAddress#getAllByName(String) 
		mCallResultSolver.addEntryForAllDescendants(MethodId.INET_ADDR_GET_BY_ADDRESS.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				// TODO Maybe we can do better
				return new InetAddressValue(UnknownValue.getInstance());
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.INET_ADDR_GET_BY_ADDRESS_HOST.getMethodReference(), new CallResultSolverEntry(new int[]{0})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				// TODO Maybe we should do better to handle the byte array IP address
				int hostValNum = invokeInst.getUse(0);
				ConcreteValue hostVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, hostValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				hostVal = NullValue.excludeNullValue(hostVal);
				return new InetAddressValue(hostVal);
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.INET_ADDR_GET_BY_NAME.getMethodReference(), new CallResultSolverEntry(new int[]{0})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int hostValNum = invokeInst.getUse(0);
				ConcreteValue hostVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, hostValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				hostVal = NullValue.excludeNullValue(hostVal);
				return new InetAddressValue(hostVal);
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.INET_ADDR_GET_LOCAL_HOST.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				return new InetAddressLocalHostValue();
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.INET_ADDR_GET_LOOPBACK_ADDR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				return new InetAddressLoopbackValue();
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.INET_SOCKET_ADDR_CREATE_UNRESOLVED.getMethodReference(), new CallResultSolverEntry(new int[]{0, 1})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int hostValNum = invokeInst.getUse(0);
				int portValNum = invokeInst.getUse(1);
				ConcreteValue hostVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, hostValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				
				// TODO Handle expected type for primitive type
				ConcreteValue portVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, portValNum), callerNode, callerInstIdx, null, maxDepth, monitor);
				return new InetSocketAddressValue(hostVal, portVal);
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.SERVER_SOCKET_ACCEPT.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				return new SocketValue(NullValue.getInstance(), true);
			}
		});
		{
			CallResultSolverEntry entrySqliteOpenHelperGetDb = new CallResultSolverEntry(new int[]{0})
			{
				@Override
				public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
						SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
						ProgressMonitor monitor)
					throws CancelException
				{
					int helperValNum = invokeInst.getUse(0);
					ConcreteValue helperVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, helperValNum), callerNode, callerInstIdx, mDbOpenHelperClass, maxDepth, monitor);
					helperVal = NullValue.excludeNullValue(helperVal);
					ConcreteValue pathVal = SQLiteOpenHelperValue.resolvePath(helperVal);
					pathVal = NullValue.excludeNullValue(pathVal);
					String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
					return new SQLiteDbValue(new ConcatValue(new ConstantStringValue("/data/data/" + pkgName + "/databases/"), pathVal));
				}
			};
			mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_SQLITE_OPEN_HELPER_GET_READABLE_DB.getMethodReference(), entrySqliteOpenHelperGetDb);
			mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_SQLITE_OPEN_HELPER_GET_WRITABLE_DB.getMethodReference(), entrySqliteOpenHelperGetDb);
		}
		{
			CallResultSolverEntry entryCtxOpenOrCreateDb = new CallResultSolverEntry(new int[]{1})
			{
				@Override
				public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
						SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
						ProgressMonitor monitor)
					throws CancelException
				{
					int pathValNum = invokeInst.getUse(1);
					ConcreteValue pathVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, pathValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
					pathVal = NullValue.excludeNullValue(pathVal);
					String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
					return new SQLiteDbValue(new ConcatValue(new ConstantStringValue("/data/data/" + pkgName + "/databases/"), pathVal));
				}
			};
			mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_OPEN_OR_CREATE_DATABASE.getMethodReference(), entryCtxOpenOrCreateDb);
			mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_OPEN_OR_CREATE_DATABASE_HANDLER.getMethodReference(), entryCtxOpenOrCreateDb);
		}
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_GET_DB_PATH.getMethodReference(), new CallResultSolverEntry(new int[]{1})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int pathValNum = invokeInst.getUse(1);
				ConcreteValue pathVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, pathValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				pathVal = NullValue.excludeNullValue(pathVal);
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				return new FileValue(new ConcatValue(new ConstantStringValue("/data/data/" + pkgName + "/databases/"), pathVal).simplify());
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_GET_CACHE_DIR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				return new FileValue(new ConstantStringValue("/data/data/" + pkgName + "/cache"));
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_GET_FILES_DIR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				return new FileValue(new ConstantStringValue("/data/data/" + pkgName + "/files"));
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_GET_DIR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int dirValNum = invokeInst.getUse(1);
				ConcreteValue dirVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, dirValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				dirVal = NullValue.excludeNullValue(dirVal);
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				return new FileValue(new ConcatValue(new ConstantStringValue("/data/data/" + pkgName + "/app_"), dirVal).simplify());
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_GET_EXTERNAL_CACHE_DIR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				return new FileValue(new ConstantStringValue("/storage/sdcard/Android/data/" + pkgName + "/cache"));
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_GET_EXTERNAL_FILES_DIR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				// TODO Consider the argument's value
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				return new FileValue(new ConcatValue(new ConstantStringValue("/storage/sdcard/Android/data/" + pkgName +"/files/"), UnknownValue.getInstance()));
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_GET_PACKAGE_NAME.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				return new ConstantStringValue(pkgName);
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_GET_PACKAGE_RESOURCE_PATH.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				// The path would be of the form /data/app/<package_name>-<num>.apk. E.g. /data/app/com.example-1.apk
				return new ConcatValue(new ConstantStringValue("/data/app/" + pkgName + "-"), UnknownValue.getInstance(), new ConstantStringValue(".apk"));
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_GET_PACKAGE_CODE_PATH.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				// The path would be of the form /data/app/<package_name>-<num>.apk. E.g. /data/app/com.example-1.apk
				return new ConcatValue(new ConstantStringValue("/data/app/" + pkgName + "-"), UnknownValue.getInstance(), new ConstantStringValue(".apk"));
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_OPEN_FILE_OUTPUT.getMethodReference(), new CallResultSolverEntry(new int[]{1})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				int pathValNum = invokeInst.getUse(1);
				ConcreteValue pathVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, pathValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				pathVal = NullValue.excludeNullValue(pathVal);
				return new FileOutputStreamValue(new ConcatValue(new ConstantStringValue("/data/data/" + pkgName + "/files/"), pathVal).simplify());
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_OPEN_FILE_INPUT.getMethodReference(), new CallResultSolverEntry(new int[]{1})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				int pathValNum = invokeInst.getUse(1);
				ConcreteValue pathVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, pathValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				pathVal = NullValue.excludeNullValue(pathVal);
				return new FileInputStreamValue(new ConcatValue(new ConstantStringValue("/data/data/" + pkgName + "/files/"), pathVal).simplify());
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_CONTEXT_GET_SHARED_PREFERENCES.getMethodReference(), new CallResultSolverEntry(new int[]{1})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				int nameNum = invokeInst.getUse(1);
				ConcreteValue nameVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, nameNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				nameVal = NullValue.excludeNullValue(nameVal);
				return new SharedPreferencesValue(nameVal);
			}
		});
		{
			CallResultSolverEntry entrySocketFactoryCreateSocket = new CallResultSolverEntry(new int[]{1, 2})
			{
				@Override
				public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
						SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
						ProgressMonitor monitor)
					throws CancelException
				{
					int hostValNum = invokeInst.getUse(1);
					int portValNum = invokeInst.getUse(2);
					ConcreteValue hostVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, hostValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
					
					// TODO Handle expected type for primitive type
					ConcreteValue portVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, portValNum), callerNode, callerInstIdx, null, maxDepth, monitor);
					return new SocketValue(new InetSocketAddressValue(hostVal, portVal), false);
				}
			};
			mCallResultSolver.addEntryForAllDescendants(MethodId.SOCKET_FACTORY_CREATE_SOCKET_STR_INT_ADDR_INT.getMethodReference(), entrySocketFactoryCreateSocket);
			mCallResultSolver.addEntryForAllDescendants(MethodId.SOCKET_FACTORY_CREATE_SOCKET_STR_INT.getMethodReference(), entrySocketFactoryCreateSocket);
		}
		{
			CallResultSolverEntry entrySocketFactoryCreateSocket = new CallResultSolverEntry(new int[]{1, 2})
			{
				@Override
				public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
						SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
						ProgressMonitor monitor)
					throws CancelException
				{
					int addrValNum = invokeInst.getUse(1);
					int portValNum = invokeInst.getUse(2);
					ConcreteValue addrVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, addrValNum), callerNode, callerInstIdx, mInetAddrClass, maxDepth, monitor);
					
					// TODO Handle expected type for primitive type
					ConcreteValue portVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, portValNum), callerNode, callerInstIdx, null, maxDepth, monitor);
					return new SocketValue(new InetSocketAddressValue(addrVal, portVal), false);
				}
			};
			mCallResultSolver.addEntryForAllDescendants(MethodId.SOCKET_FACTORY_CREATE_SOCKET_ADDR_INT_ADDR_INT.getMethodReference(), entrySocketFactoryCreateSocket);
			mCallResultSolver.addEntryForAllDescendants(MethodId.SOCKET_FACTORY_CREATE_SOCKET_ADDR_INT.getMethodReference(), entrySocketFactoryCreateSocket);
		}
		mCallResultSolver.addEntryForAllDescendants(MethodId.SOCKET_FACTORY_CREATE_SOCKET.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				SocketCFValueSolver sockSolver = 
						new SocketCFValueSolver(SliceValueSolver.this, callerNode, callerInstIdx, invokeInst.getDef(), new ValueUsageWalker.FixPointEndCriterion(endNode, endInstIdx), maxDepth - 1);
				sockSolver.run(new SubProgressMonitor(monitor, 10));
				return sockSolver.getValue();
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_PREFERENCE_MGR_GET_DEFAULT_SHARED_PREFERENCES.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				// The name of the shared preference will be <package_name> + "_preferences". E.g. com.example.test_preferences
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				return new SharedPreferencesValue(new ConstantStringValue(pkgName + "_preferences"));
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_PREFERENCE_MGR_GET_SHARED_PREFERENCES.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				// The PreferenceManager can be initiated by a hidden constructor which takes an Activity as argument
				// The default name of the shared preferences will be decided by the the Activity (Context); however, 
				// it can be changed by setSharedPreferencesName
				// TODO Do more accurately
				return new SharedPreferencesValue(UnknownValue.getInstance());
			}
		});
		{
			CallResultSolverEntry entryAssetMgrOpen = new CallResultSolverEntry(new int[]{1})
			{
				@Override
				public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
						SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
						ProgressMonitor monitor)
					throws CancelException
				{
					int nameValNum = invokeInst.getUse(1);
				ConcreteValue nameVal = solveValue(ctx, new ParamCaller(callerNode, callerInstIdx, nameValNum), callerNode, callerInstIdx, mStrClass, maxDepth, monitor);
				
				// The name of the apk would be <pkg_name>-<num>.apk. E.g. com.example-1.apk
				String pkgName = getAnalysisContext().getAppInfo().getManifest().getPackageName();
				nameVal = NullValue.excludeNullValue(nameVal);
				return new FileInputStreamValue(
					new ConcatValue(
						new ConstantStringValue("/data/app/" + pkgName + "-"), UnknownValue.getInstance(), 
						new ConstantStringValue(".apk/assets/"), 
						nameVal));
				}
			};
			mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_ASSET_MGR_OPEN.getMethodReference(), entryAssetMgrOpen);
			mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_ASSET_MGR_OPEN_MODE.getMethodReference(), entryAssetMgrOpen);
		}
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_ENVIRONMENT_GET_DATA_DIR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				return new FileValue(new ConstantStringValue("/data"));
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_ENVIRONMENT_GET_DOWNLOAD_CACHE_DIR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				return new FileValue(new ConstantStringValue("/cache"));
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_ENVIRONMENT_GET_EXTERNAL_STORAGE_DIR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				return new FileValue(new ConstantStringValue("/storage/sdcard"));
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_ENVIRONMENT_GET_EXTERNAL_STORAGE_PUBLIC_DIR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				// TODO Handle the type parameter
				return new FileValue(new ConcatValue(new ConstantStringValue("/storage/sdcard/"), UnknownValue.getInstance()));
			}
		});
		mCallResultSolver.addEntryForAllDescendants(MethodId.ANDROID_ENVIRONMENT_GET_ROOT_DIR.getMethodReference(), new CallResultSolverEntry(new int[]{})
		{
			@Override
			public ConcreteValue solve(Context ctx, CGNode callerNode, int callerInstIdx,
					SSAAbstractInvokeInstruction invokeInst,
					CGNode endNode, int endInstIdx,
					int maxDepth,
					ProgressMonitor monitor)
				throws CancelException
			{
				return new FileValue(new ConstantStringValue("/system"));
			}
		});
	}
	@Override
	public void cleanUp()
	{}
	protected static ConcreteValue getUriFromUriBuilder(ConcreteValue val)
	{
		if(val instanceof AndroidUriBuilderValue)
		{
			AndroidUriBuilderValue uriBuilderVal = (AndroidUriBuilderValue)val;
			return uriBuilderVal.getUriValue();
		}
		else if(val instanceof OrValue)
		{
			OrValue result = new OrValue();
			OrValue orVal = (OrValue)val;
			Iterator<ConcreteValue> itr = orVal.iterator();
			while(itr.hasNext())
			{
				ConcreteValue subVal = itr.next();
				if(subVal instanceof AndroidUriBuilderValue)
					result.addValue(((AndroidUriBuilderValue) subVal).getUriValue());
				else
					result.addValue(UnknownValue.getInstance());
			}
			return result.simplify();
		}
		else
			return UnknownValue.getInstance();
	}
	private boolean isOverridingMethodOf(IMethod overridingMethod, IClass oriClass, Selector oriSel)
	{
		Selector sel = overridingMethod.getSelector();
		if(!sel.equals(oriSel))
			return false;
		IClass clazz = overridingMethod.getDeclaringClass();
		AndroidAnalysisContext analysisCtx = getAnalysisContext();
		IClassHierarchy cha = analysisCtx.getClassHierarchy();
		return cha.isAssignableFrom(oriClass, clazz);
	}
	private boolean checkType(IClass expectedType, IClass checkedType)
	{
		if(expectedType == null)
			return true;
		else
			return getAnalysisContext().getClassHierarchy().isAssignableFrom(expectedType, checkedType);
	}
	
	/**
	 * If the return value is null, we should ignore this allocation site.
	 * @param ctx
	 * @param node
	 * @param instIdx
	 * @param newInst
	 * @param endNode
	 * @param endInst
	 * @return
	 */
	protected ConcreteValue onAllocSource(
			Context ctx, 
			NormalStatement normalStm, 
			SSANewInstruction newInst, 
			CGNode endNode, 
			int endInst, 
			IClass expectType, 
			int maxDepth, 
			ProgressMonitor monitor)
		throws CancelException
	{
		CGNode node = normalStm.getNode();
		int instIdx = normalStm.getInstructionIndex();
		TypeReference newTypeRef = newInst.getNewSite().getDeclaredType();
		int defValNum = newInst.getDef();
		IClassHierarchy cha = getAnalysisContext().getClassHierarchy();
		IClass newType = cha.lookupClass(newTypeRef);
		if(newType == null || !checkType(expectType, newType))
			return null;
		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
		try
		{
			if(cha.isAssignableFrom(mStrBuilderClass, newType))
			{
				StringBuilderCFValueSolver strBuilderSolver = 
						new StringBuilderCFValueSolver(this, node, instIdx, defValNum, new ValueUsageWalker.FixPointEndCriterion(endNode, endInst), maxDepth - 1);
				strBuilderSolver.run(ctx.valSrcSolver.getCallRecords(), subMonitor);
				return strBuilderSolver.getValue();
			}
			else if(cha.isAssignableFrom(mAndroidIntentClass, newType))
			{
				IntentCFValueSolver intentValSolver  = 
						new IntentCFValueSolver(this, node, instIdx, defValNum, new ValueUsageWalker.FixPointEndCriterion(endNode, endInst), maxDepth - 1);
				intentValSolver.run(ctx.valSrcSolver.getCallRecords(), subMonitor);
				return intentValSolver.getValue();
			}
			else if(cha.isAssignableFrom(mUriBuilderClass, newType))
			{
				AndroidUriBuilderCFValueSolver uriBuilderSolver = 
						new AndroidUriBuilderCFValueSolver(this, node, instIdx, defValNum, new ValueUsageWalker.FixPointEndCriterion(endNode, endInst), maxDepth - 1);
				uriBuilderSolver.run(ctx.valSrcSolver.getCallRecords(), subMonitor);
				return uriBuilderSolver.getValue();
			}
			else if(cha.isAssignableFrom(mIntentFilterClass, newType))
			{
				IntentFilterCFValueSolver intentFilterSolver = 
						new IntentFilterCFValueSolver(this, node, instIdx, defValNum, new ValueUsageWalker.FixPointEndCriterion(endNode, endInst), maxDepth - 1);
				intentFilterSolver.run(ctx.valSrcSolver.getCallRecords(), subMonitor);
				return intentFilterSolver.getValue();
			}
			else if(cha.isAssignableFrom(mSocketClass, newType))
			{
				SocketCFValueSolver sockSolver = 
						new SocketCFValueSolver(this, node, instIdx, defValNum, new ValueUsageWalker.FixPointEndCriterion(endNode, endInst), maxDepth - 1);
				sockSolver.run(ctx.valSrcSolver.getCallRecords(), subMonitor);
				return sockSolver.getValue();
			}
			else if(cha.isAssignableFrom(mFileClass, newType))
			{
				FileSliceValueSolver fileSolver = new FileSliceValueSolver(this, normalStm);
				fileSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return fileSolver.getValue();
			}
			else if(cha.isAssignableFrom(mStrClass, newType))
			{
				StringSliceValueSolver strSolver = new StringSliceValueSolver(this, normalStm);
				strSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return strSolver.getValue();
			}
			else if(cha.isAssignableFrom(mFileInputStreamClass, newType))
			{
				FileInputStreamSliceValueSolver fisSolver = new FileInputStreamSliceValueSolver(this, normalStm);
				fisSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return fisSolver.getValue();
			}
			else if(cha.isAssignableFrom(mFileOutputStreamClass, newType))
			{
				FileOutputStreamSliceValueSolver fosSolver = new FileOutputStreamSliceValueSolver(this, normalStm);
				fosSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return fosSolver.getValue();
			}
			else if(cha.isAssignableFrom(mDbOpenHelperClass, newType))
			{
				SQLiteOpenHelperSliceValueSolver dbHelperSolver = new SQLiteOpenHelperSliceValueSolver(this, normalStm);
				dbHelperSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return dbHelperSolver.getValue();
			}
			else if(cha.isAssignableFrom(mSqliteDbClass, newType))
			{
				SQLiteDbSliceValueSolver dbSolver = new SQLiteDbSliceValueSolver(this, normalStm);
				dbSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return dbSolver.getValue();
			}
			else if(cha.isAssignableFrom(mHttpHostClass, newType))
			{
				HttpHostSliceValueSolver httpHostSolver = new HttpHostSliceValueSolver(this, normalStm);
				httpHostSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return httpHostSolver.getValue();
			}
			else if(cha.isAssignableFrom(mHttpRequestClass, newType))
			{
				HttpRequestSliceValueSolver uriReqSolver = new HttpRequestSliceValueSolver(this, normalStm);
				uriReqSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return uriReqSolver.getValue();
			}
			else if(cha.isAssignableFrom(mUriClass, newType))
			{
				UriSliceValueSolver uriSolver = new UriSliceValueSolver(this, normalStm);
				uriSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return uriSolver.getValue();
			}
			else if(cha.isAssignableFrom(mUrlClass, newType))
			{
				UrlSliceValueSolver urlSolver = new UrlSliceValueSolver(this, normalStm);
				urlSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return urlSolver.getValue();
			}
			else if(cha.isAssignableFrom(mUrlConnClass, newType))
			{
				UrlConnectionValueSolver urlConnSolver = new UrlConnectionValueSolver(this, normalStm);
				urlConnSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return urlConnSolver.getValue();
			}
			else if(cha.isAssignableFrom(mInetSocketAddrClass, newType))
			{
				InetSocketAddrSliceValueSolver sockAddrSolver = new InetSocketAddrSliceValueSolver(this, normalStm);
				sockAddrSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return sockAddrSolver.getValue();
			}
			else if(cha.isAssignableFrom(mCompNameClass, newType))
			{
				ComponentNameSliceValueSolver compNameSolver = new ComponentNameSliceValueSolver(this, normalStm);
				compNameSolver.solve(ctx.valSrcSolver.getCallRecords(), maxDepth - 1, subMonitor);
				return compNameSolver.getValue();
			}
			else
			{
				return UnknownValue.getInstance();
			}
		}
		finally
		{
			monitor.setSubProgressMonitor(null);
		}
	}
	private ConcreteValue solveValue(
			Context ctx, Statement rootStm, CGNode endNode, int endInstIdx, IClass expectType, int maxDepth, ProgressMonitor monitor)
		throws CancelException
	{
		if(monitor != null && monitor.isCanceled())
			throw CancelException.make("Operation canceled");
		if(maxDepth <= 0)
			return UnknownValue.getInstance();
		ValueSourceSolver valSrcSolver = ctx.valSrcSolver;
		IntSet facts = valSrcSolver.getStatementFacts(rootStm);
		if(facts == null || facts.isEmpty())
			return UnknownValue.getInstance();
		ValueKey valKey = new ValueKey(rootStm, endNode, endInstIdx, expectType);
		try
		{
			// Prevent circular dependency
			if(!ctx.valsOnStack.add(valKey))
				return UnknownValue.getInstance();
			{
				Pair<ConcreteValue, Integer> pair = ctx.valuesCache.getIfPresent(valKey);
				if(pair != null && pair.getRight() >= maxDepth)
				{
					return pair.getLeft();
				}
			}
			IClassHierarchy cha = getAnalysisContext().getClassHierarchy();
			OrValue result = new OrValue();
			
			// Handle constant sources
			for(Iterator<Object> itr = valSrcSolver.getConstants(facts);
				itr.hasNext();)
			{
				Object constVal = itr.next();
				result.addValue(ConcreteValue.fromConstantValue(constVal));
			}
			
			boolean hasUnknownVal = false;
			// Handle the call flow sources
			for(Iterator<Pair<Statement, Statement>> itr = valSrcSolver.getCallSources(facts);
				!hasUnknownVal && itr.hasNext();)
			{
				Pair<Statement, Statement> call = itr.next();
				Statement src = call.getLeft();
				Statement dest = call.getRight();
				switch(src.getKind())
				{
				case NORMAL_RET_CALLER:
					{
						NormalReturnCaller retCallerStm = (NormalReturnCaller)src;
						TypeReference retTypeRef = retCallerStm.getInstruction().getDeclaredResultType();
						IClass retType = cha.lookupClass(retTypeRef);
						if(!checkType(expectType, retType))
							break;
						ConcreteValue val = mCallResultSolver.solve(ctx, retCallerStm, dest == null ? null : dest.getNode(), endNode, endInstIdx, expectType, maxDepth, monitor);
						assert val != null;
						result.addValue(val);
						if(UnknownValue.isPossiblelUnknown(val))
						{
							hasUnknownVal = true;
						}
						break;
					}
				default:
					break;
				}
			}
			
			// Handle allocation site sources
			for(Iterator<NormalStatement> itr = valSrcSolver.getAllocSources(facts);
					!hasUnknownVal && itr.hasNext();)
			{
				NormalStatement normalStm = itr.next();
				SSAInstruction inst = normalStm.getInstruction();
				if(!(inst instanceof SSANewInstruction))
					continue;
				ConcreteValue val = 
						onAllocSource(ctx, normalStm, (SSANewInstruction)inst, endNode, endInstIdx, expectType, maxDepth, monitor);
				if(val != null)
				{
					result.addValue(val);
					if(UnknownValue.isPossiblelUnknown(val))
						hasUnknownVal = true;
				}
			}
						
			if(mLogger.isDebugEnabled())
			{
				StringBuilder builder = new StringBuilder();
				builder.append("Statements reached by [");
				builder.append(rootStm.toString());
				builder.append("] in backward-slicing:\n");
				for(Iterator<Statement> itr = valSrcSolver.getReachedStatements(facts);
					itr.hasNext();)
				{
					Statement reachedStm = itr.next();
					builder.append('\t');
					builder.append(reachedStm.toString());
					builder.append('\n');
				}
				mLogger.debug("{}", builder.toString());
			}
			ConcreteValue ret = result.simplify();
			ctx.valuesCache.put(valKey, Pair.of(ret, maxDepth));
			return ret;
		}
		finally
		{
			ctx.valsOnStack.remove(valKey);
		}
	}
	@Override
	public ConcreteValue solve(Statement defStm, CGNode endNode,
			int endInstIdx, TypeReference typeRef,
			CallRecords oldCallRecords, int maxDepth, ProgressMonitor monitor)
			throws CancelException
	{
		if(defStm == null || endNode == null || typeRef == null || oldCallRecords == null || monitor == null)
			throw new IllegalArgumentException();
		try
		{
			monitor.beginTask("Solving concrete value", 1000);
			mLogger.debug("Solving value with defining statement: {}", defStm);
			if(maxDepth <= 0)
				return UnknownValue.getInstance();
			IClass type = null;
			IClassHierarchy cha = getAnalysisContext().getClassHierarchy();
			if(typeRef != null)
			{
				if(typeRef.isClassType())
				{
					type = cha.lookupClass(typeRef);
					if(type == null)
						mLogger.warn("Fail to find " + typeRef + " in class hierarchy. Ignore it.");
				}
			}
	
			AndroidAnalysisContext analysisCtx = getAnalysisContext();
			ValueSourceSolver valSrcSolver = 
					new ValueSourceSolver(analysisCtx, new SliceValueFlowFunctions(analysisCtx, mCallResultSolver), oldCallRecords);
			valSrcSolver.setIsRecordCalls(true);
		
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 500);
				valSrcSolver.solve(defStm, subMonitor);
			}
			
			Context ctx = new Context();
			ctx.valSrcSolver = valSrcSolver;
			
			ConcreteValue result;
			{
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 500);
				subMonitor.beginTask("Solving value", 100);
				try
				{
					result = solveValue(ctx, defStm, endNode, endInstIdx, type, maxDepth, subMonitor);
				}
				finally
				{
					subMonitor.done();
				}
			}
			return result;
		}
		finally
		{
			mLogger.debug("Finish solving value with defining statement: {}", defStm);
			monitor.done();
		}
	}
}
