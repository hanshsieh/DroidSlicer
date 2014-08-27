package org.droidslicer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.droidslicer.android.appSpec.AndroidListenerSpec;
import org.droidslicer.android.appSpec.EntryCompSpec;
import org.droidslicer.android.appSpec.EntryMethodSpec;
import org.droidslicer.config.APIPermissionParser.APIPermission;
import org.droidslicer.config.EntryPointConfigParser.EntryMethodConfig;
import org.droidslicer.graph.entity.ActivityUnit;
import org.droidslicer.graph.entity.ReceiverUnit;
import org.droidslicer.graph.entity.ServiceUnit;
import org.droidslicer.graph.entity.resolver.APIInvokeResolver;
import org.droidslicer.graph.entity.resolver.ActivityIntentInflowResolver;
import org.droidslicer.graph.entity.resolver.ActivityIntentOutflowResolver;
import org.droidslicer.graph.entity.resolver.FileInOutStreamReturnTypeResolver;
import org.droidslicer.graph.entity.resolver.FileInputEntityResolver;
import org.droidslicer.graph.entity.resolver.FileOutputEntityResolver;
import org.droidslicer.graph.entity.resolver.HttpClientExecResolver;
import org.droidslicer.graph.entity.resolver.IntentCommResolver;
import org.droidslicer.graph.entity.resolver.ReceiverRegisterResolver;
import org.droidslicer.graph.entity.resolver.ReturnFileInOutStreamResolver;
import org.droidslicer.graph.entity.resolver.ReturnSharedPreferencesResolver;
import org.droidslicer.graph.entity.resolver.ReturnUrlConnectionResolver;
import org.droidslicer.graph.entity.resolver.SQLiteDatabaseResolver;
import org.droidslicer.graph.entity.resolver.SQLiteStatementResolver;
import org.droidslicer.graph.entity.resolver.SocketInOutEntityResolver;
import org.droidslicer.graph.entity.resolver.UriCommResolver;
import org.droidslicer.graph.entity.resolver.UrlConnectionAllocResolver;
import org.droidslicer.graph.entity.resolver.UrlOpenStreamResolver;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.TypeId;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.StringStuff;

public class FileAnalysisConfig extends AbstractAnalysisConfig
{
	private final Predicate<String> mPermOfInterestPred;
	private boolean mInitialized = false;
	public FileAnalysisConfig(
			InputStream apiPermInput, 
			InputStream intentPerm, 
			InputStream providerUris, 
			InputStream listenerInput, 
			InputStream entryPointInput
			)
		throws IOException
	{
		this(apiPermInput, intentPerm, providerUris, listenerInput, entryPointInput, Predicates.<String>alwaysTrue());
	}
	public FileAnalysisConfig(
			InputStream apiPermInput, 
			InputStream intentPerm, 
			InputStream providerUris, 
			InputStream listenerInput, 
			InputStream entryPointInput,
			Predicate<String> permOfInterestPred)
		throws IOException
	{
		mPermOfInterestPred = permOfInterestPred;
		// The API permission must be put at first, because some special handling 
		// may need to override the default action
		addAPIPermissionMethods(apiPermInput);
		addSpecialInvokes();
		addIntentPermissions(intentPerm);
		addProviderUris(providerUris);
		addListeners(listenerInput);
		addEntryPointMethods(entryPointInput);
		addICCMethods();
		addDatabaseResolvers();
		addNetworkResolvers();
		addFileResolvers();
		addSharedPreferencesResolvers();
		addReceiverRegisterResolvers();
	}
	protected void addSpecialInvokes()
	{
		// TODO Runtime.exec, new ProcessBuilder().start()
		/*{
			APIPermission apiPerm;
			while((apiPerm = apiPermParser.read()) != null)
			{
				APIInvokeResolver resolver = new APIInvokeResolver(MethodId.METHOD, false);
				int nParam = apiPerm.getNumberOfParameters();
				for(int i = 0; i < nParam; ++i)
				{
					resolver.setParamTrack(i, apiPerm.isParamTrack(i));
					resolver.setParamListener(i, apiPerm.isParamListener(i));
					resolver.setParamResolve(i, apiPerm.isParamResolve(i));
				}
				for(String perm : apiPerm.getPermissions())
				{
					resolver.addPermission(perm);
				}
				resolver.setReturnTrack(apiPerm.isReturnTrack());
				addInvocationResolver(resolver);
			}
		}*/
	}
	protected void addReceiverRegisterResolvers()
	{
		{
			ReceiverRegisterResolver resolver = 
					new ReceiverRegisterResolver(MethodId.ANDROID_CONTEXT_REGISTER_RECEIVER.getMethodReference(), false, 1, 2);
			addInvocationResolver(resolver);
		}
		{
			ReceiverRegisterResolver resolver = 
					new ReceiverRegisterResolver(MethodId.ANDROID_CONTEXT_REGISTER_RECEIVER_PERM_HANDLER.getMethodReference(), false, 1, 2);
			addInvocationResolver(resolver);
		}
	}
	protected void addProviderUris(InputStream input)
		throws IOException
	{
		ProviderPermissionParser parser = null;
		try
		{
			parser = new ProviderPermissionParser(input);
			ProviderPermission provider;
			while((provider = parser.read()) != null)
			{
				{
					String readPerm = provider.getReadPermission();
					String writePerm = provider.getWritePermission();
					if(readPerm != null && !mPermOfInterestPred.apply(readPerm))
						provider.setReadPermission(null);
					if(writePerm != null && !mPermOfInterestPred.apply(writePerm))
						provider.setWritePermission(null);
				}
				Set<PathPermission> pathPerms = new LinkedHashSet<PathPermission>(provider.getPathPermissions());
				for(PathPermission pathPerm : pathPerms)
				{
					String pathReadPerm = pathPerm.getReadPermission();
					String pathWritePerm = pathPerm.getWritePermission();
					if(pathReadPerm != null && !mPermOfInterestPred.apply(pathReadPerm))
						pathPerm.setReadPermission(null);
					if(pathWritePerm != null && !mPermOfInterestPred.apply(pathWritePerm))
						pathPerm.setWritePermission(null);
					if(pathPerm.getReadPermission() == null && pathPerm.getWritePermission() == null)
						provider.removePathPermission(pathPerm);
				}
				if(provider.getReadPermission() != null ||
					provider.getWritePermission() != null ||
					!provider.getPathPermissions().isEmpty())
				{
					addProviderPermission(provider);
				}
			}
		}
		finally
		{
			if(parser != null)
			{
				try
				{
					parser.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	protected void addIntentPermissions(InputStream input)
		throws IOException
	{
		IntentPermissionParser parser = null;
		try
		{
			parser = new IntentPermissionParser(input);
			IntentPermission intentPerm = null;
			while((intentPerm = parser.read()) != null)
			{
				Set<String> senderPerms = new LinkedHashSet<String>(intentPerm.getSenderPermissions());
				Set<String> receiverPerms = new LinkedHashSet<String>(intentPerm.getReceiverPermissions());
				for(String perm : senderPerms)
				{
					if(!mPermOfInterestPred.apply(perm))
					{
						intentPerm.removeSenderPermission(perm);
					}
				}
				for(String perm : receiverPerms)
				{
					if(!mPermOfInterestPred.apply(perm))
					{
						intentPerm.removeReceiverPermission(perm);
					}
				}
				if(!intentPerm.getSenderPermissions().isEmpty() || !intentPerm.getReceiverPermissions().isEmpty())
					addIntentPermission(intentPerm);
			}
		}
		finally
		{
			if(parser != null)
			{
				try
				{
					parser.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	protected void addAPIPermissionMethods(InputStream apiPermInput)
		throws IOException
	{
		APIPermissionParser apiPermParser = null;
		try
		{
			apiPermParser = new APIPermissionParser(apiPermInput);
			{
				APIPermission apiPerm;
				while((apiPerm = apiPermParser.read()) != null)
				{
					APIInvokeResolver resolver = new APIInvokeResolver(apiPerm.getMethodReference(), apiPerm.isStatic());
					int nParam = apiPerm.getNumberOfParameters();
					for(String perm : apiPerm.getPermissions())
					{
						if(mPermOfInterestPred.apply(perm))
							resolver.addPermission(perm);
					}
					boolean hasListenerParam = false;
					for(int i = 0; i < nParam; ++i)
					{
						if(apiPerm.isParamListener(i))
						{
							resolver.setParamListener(i, true);
							hasListenerParam = true;
						}
						else
							resolver.setParamListener(i, false);
						if(!resolver.getPermissions().isEmpty())
						{
							resolver.setParamTrack(i, apiPerm.isParamTrack(i));
							resolver.setTrackParamListener(i, apiPerm.isParamListener(i));
						}
						resolver.setParamResolve(i, apiPerm.isParamResolve(i));
					}
					if(!resolver.getPermissions().isEmpty())
						resolver.setReturnTrack(apiPerm.isReturnTrack());	
					if(hasListenerParam || !resolver.getPermissions().isEmpty())
					{
						addInvocationResolver(resolver);						
					}
				}
			}
		}
		finally
		{
			if(apiPermParser != null)
			{
				try
				{
					apiPermParser.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	protected void addListeners(InputStream listenerInput)
		throws IOException
	{
		ListenerConfigParser listenerParser = null;
		try
		{
			listenerParser = new ListenerConfigParser(listenerInput);
			{
				String classType;
				while((classType = listenerParser.read()) != null)
				{
					TypeReference clazzTypeRef = TypeReference.findOrCreate(ClassLoaderReference.Primordial, StringStuff.deployment2CanonicalTypeString(classType));
					AndroidListenerSpec spec = new AndroidListenerSpec(clazzTypeRef);
					addListenerClass(spec);
				}
			}
		}
		finally
		{
			if(listenerParser != null)
			{
				try
				{
					listenerParser.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	protected void addEntryPointMethods(InputStream entryPointInput)
		throws IOException
	{
		EntryPointConfigParser entryPointParser = null;
		try
		{
			entryPointParser = new EntryPointConfigParser(entryPointInput);
			{
				EntryMethodConfig entryMethod;
				while((entryMethod = entryPointParser.read()) != null)
				{
					MethodReference methodRef = entryMethod.getMethodReference();
					TypeReference typeRef = methodRef.getDeclaringClass();
					EntryCompSpec compSpec;
					if(typeRef.equals(TypeId.ANDROID_ACTIVITY.getTypeReference()))
						compSpec = getActivitySpec();
					else if(typeRef.equals(TypeId.ANDROID_RECEIVER.getTypeReference()))
						compSpec = getReceiverSpec();
					else if(typeRef.equals(TypeId.ANDROID_PROVIDER.getTypeReference()))
						compSpec = getProviderSpec();
					else if(typeRef.equals(TypeId.ANDROID_SERVICE.getTypeReference()))
						compSpec = getServiceSpec();
					else if(typeRef.equals(TypeId.ANDROID_APPLICATION.getTypeReference()))
						compSpec = getApplicationSpec();
					else
						throw new IOException("Unknown entry component type: " + typeRef);
					EntryMethodSpec methodSpec = new EntryMethodSpec(methodRef, entryMethod.isStatic());
					int nParam = entryMethod.getNumberOfParameters();
					for(int i = 0; i < nParam; ++i)
						methodSpec.setParamTrack(i, entryMethod.isParamTrack(i));
					compSpec.addEntryMethod(methodSpec);
				}
			}
		}
		finally
		{
			if(entryPointParser != null)
			{
				try
				{
					entryPointParser.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	protected void addActivityICCResolvers()
	{
		{
			IntentCommResolver resolver = new IntentCommResolver(ActivityUnit.class, MethodId.ANDROID_CONTEXT_START_ACTIVITY.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
		{
			IntentCommResolver resolver = new IntentCommResolver(ActivityUnit.class, MethodId.ANDROID_CONTEXT_START_ACTIVITY_BUNDLE.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
		
		// Activity in-flow
		{
			ActivityIntentInflowResolver resolver = new ActivityIntentInflowResolver(MethodId.ANDROID_ACTIVITY_GET_INTENT.getMethodReference(), false);
			addInvocationResolver(resolver);
		}
		
		// Activity out-flow
		{
			ActivityIntentOutflowResolver resolver = new ActivityIntentOutflowResolver(MethodId.ANDROID_ACTIVITY_SET_RESULT.getMethodReference(), false);
			addInvocationResolver(resolver);
		}
		{
			ActivityIntentOutflowResolver resolver = new ActivityIntentOutflowResolver(MethodId.ANDROID_ACTIVITY_SET_RESULT_DATA.getMethodReference(), false);
			addInvocationResolver(resolver);
		}
	}
	protected void addReceiverICCResolvers()
	{
		{
			IntentCommResolver resolver = new IntentCommResolver(ReceiverUnit.class, MethodId.ANDROID_CONTEXT_SEND_BROADCAST_PERM.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
		{
			IntentCommResolver resolver = new IntentCommResolver(ReceiverUnit.class, MethodId.ANDROID_CONTEXT_SEND_BROADCAST.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
		{
			IntentCommResolver resolver = new IntentCommResolver(ReceiverUnit.class, MethodId.ANDROID_CONTEXT_SEND_BROADCAST_AS_USER.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
		{
			IntentCommResolver resolver = new IntentCommResolver(ReceiverUnit.class, MethodId.ANDROID_CONTEXT_SEND_BROADCAST_AS_USER_PERM.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
		{
			IntentCommResolver resolver = new IntentCommResolver(ReceiverUnit.class, MethodId.ANDROID_CONTEXT_SEND_ORDERED_BROADCAST_RESULT.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
		{
			IntentCommResolver resolver = new IntentCommResolver(ReceiverUnit.class, MethodId.ANDROID_CONTEXT_SEND_ORDERED_BROADCAST.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
		{
			IntentCommResolver resolver = new IntentCommResolver(ReceiverUnit.class, MethodId.ANDROID_CONTEXT_SEND_ORDERED_BROADCAST_AS_USER_RESULT.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
		{
			IntentCommResolver resolver = new IntentCommResolver(ReceiverUnit.class, MethodId.ANDROID_CONTEXT_SEND_STICKY_BROADCAST.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
		{
			IntentCommResolver resolver = new IntentCommResolver(ReceiverUnit.class, MethodId.ANDROID_CONTEXT_SEND_STICKY_BROADCAST_AS_USER.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
		{
			IntentCommResolver resolver = new IntentCommResolver(ReceiverUnit.class, MethodId.ANDROID_CONTEXT_SEND_STICKY_ORDERED_BROADCAST.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
		{
			IntentCommResolver resolver = new IntentCommResolver(ReceiverUnit.class, MethodId.ANDROID_CONTEXT_SEND_STICKY_ORDERED_BROADCAST_AS_USER.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
	}
	protected void addProviderICCResolvers()
	{
		{
			UriCommResolver resolver = new UriCommResolver(MethodId.ANDROID_PROVIDER_INSERT.getMethodReference(), MethodId.ANDROID_CONTENT_RESOLVER_INSERT.getMethodReference(), false, 1);
			resolver.setParamTrack(1, true);
			resolver.setParamTrack(2, true);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			UriCommResolver resolver = new UriCommResolver(MethodId.ANDROID_PROVIDER_BULK_INSERT.getMethodReference(), MethodId.ANDROID_CONTENT_RESOLVER_BULK_INSERT.getMethodReference(), false, 1);
			resolver.setParamTrack(1, true);
			resolver.setParamTrack(2, true);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			UriCommResolver resolver = new UriCommResolver(MethodId.ANDROID_PROVIDER_QUERY.getMethodReference(), MethodId.ANDROID_CONTENT_RESOLVER_QUERY.getMethodReference(), false, 1);
			resolver.setParamTrack(1, true);
			resolver.setParamTrack(2, true);
			resolver.setParamTrack(3, true);
			resolver.setParamTrack(4, true);
			resolver.setParamTrack(5, true);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			UriCommResolver resolver = new UriCommResolver(MethodId.ANDROID_PROVIDER_QUERY_CANCEL.getMethodReference(), MethodId.ANDROID_CONTENT_RESOLVER_QUERY_CANCEL.getMethodReference(), false, 1);
			resolver.setParamTrack(1, true);
			resolver.setParamTrack(2, true);
			resolver.setParamTrack(3, true);
			resolver.setParamTrack(4, true);
			resolver.setParamTrack(5, true);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			UriCommResolver resolver = new UriCommResolver(MethodId.ANDROID_PROVIDER_DELETE.getMethodReference(), MethodId.ANDROID_CONTENT_RESOLVER_DELETE.getMethodReference(), false, 1);
			resolver.setParamTrack(1, true);
			resolver.setParamTrack(2, true);
			resolver.setParamTrack(3, true);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			UriCommResolver resolver = new UriCommResolver(MethodId.ANDROID_PROVIDER_UPDATE.getMethodReference(), MethodId.ANDROID_CONTENT_RESOLVER_UPDATE.getMethodReference(), false, 1);
			resolver.setParamTrack(1, true);
			resolver.setParamTrack(2, true);
			resolver.setParamTrack(3, true);
			resolver.setParamTrack(4, true);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		// TODO Handle ContentResolver#applyBatch
		// TODO Handle ContentResolver#call
	}
	protected void addServiceICCResolvers()
	{
		{
			IntentCommResolver resolver = new IntentCommResolver(ServiceUnit.class, MethodId.ANDROID_CONTEXT_START_SERVICE.getMethodReference(), false, 1);
			addInvocationResolver(resolver);
		}
	}
	protected void addICCMethods()
	{
		addActivityICCResolvers();
		addReceiverICCResolvers();
		addProviderICCResolvers();
		addServiceICCResolvers();
	}
	protected void addSQLiteStatementResolvers()
	{
		{
			SQLiteStatementResolver resolver = new SQLiteStatementResolver(MethodId.ANDROID_SQLITE_STM_EXEC.getMethodReference(), false, false);
			addInvocationResolver(resolver);
		}
		{
			SQLiteStatementResolver resolver = new SQLiteStatementResolver(MethodId.ANDROID_SQLITE_STM_EXEC_INSERT.getMethodReference(), false, false);
			addInvocationResolver(resolver);
		}
		{
			SQLiteStatementResolver resolver = new SQLiteStatementResolver(MethodId.ANDROID_SQLITE_STM_EXEC_UPDATE_DELETE.getMethodReference(), false, false);
			addInvocationResolver(resolver);
		}
		{
			SQLiteStatementResolver resolver = new SQLiteStatementResolver(MethodId.ANDROID_SQLITE_STM_SIMPLE_QUERY_BLOB_FILE_FD.getMethodReference(), false, true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteStatementResolver resolver = new SQLiteStatementResolver(MethodId.ANDROID_SQLITE_STM_SIMPLE_QUERY_LONG.getMethodReference(), false, true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteStatementResolver resolver = new SQLiteStatementResolver(MethodId.ANDROID_SQLITE_STM_SIMPLE_QUERY_STR.getMethodReference(), false, true);
			addInvocationResolver(resolver);
		}
	}
	protected void addSQLiteDatabaseResolvers()
	{
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_EXEC_SQL.getMethodReference(), false);
			resolver.setParamTrack(1, true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_INSERT.getMethodReference(), false);
			resolver.setParamTrack(3, true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_INSERT_OR_THROW.getMethodReference(), false);
			resolver.setParamTrack(3, true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_INSERT_CONFLICT.getMethodReference(), false);
			resolver.setParamTrack(3, true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_QUERY_LIMIT.getMethodReference(), false);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_QUERY_DISTINCT_LIMIT_CANCEL.getMethodReference(), false);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_QUERY.getMethodReference(), false);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_QUERY_DISTINCT_LIMIT.getMethodReference(), false);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_QUERY_FACT_CANCEL.getMethodReference(), false);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_QUERY_FACT.getMethodReference(), false);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_RAW_QUERY_CANCEL.getMethodReference(), false);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_RAW_QUERY.getMethodReference(), false);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_RAW_QUERY_FACT.getMethodReference(), false);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_RAW_QUERY_FACT_CANCEL.getMethodReference(), false);
			resolver.setReturnTrack(true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_REPLACE.getMethodReference(), false);
			resolver.setParamTrack(3, true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_REPLACE_THROW.getMethodReference(), false);
			resolver.setParamTrack(3, true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_UPDATE.getMethodReference(), false);
			resolver.setParamTrack(2, true);
			addInvocationResolver(resolver);
		}
		{
			SQLiteDatabaseResolver resolver = new SQLiteDatabaseResolver(MethodId.ANDROID_SQLITE_DB_UPDATE_CONFLICT.getMethodReference(), false);
			resolver.setParamTrack(2, true);
			addInvocationResolver(resolver);
		}		
	}
	protected void addDatabaseResolvers()
	{
		addSQLiteStatementResolvers();
		addSQLiteDatabaseResolvers();
	}
	protected void addHttpClientResolvers()
	{
		{
			HttpClientExecResolver resolver = new HttpClientExecResolver(MethodId.HTTP_CLIENT_EXECUTE_REQ_HANDLER_CTX.getMethodReference());
			addInvocationResolver(resolver);
		}
		{
			HttpClientExecResolver resolver = new HttpClientExecResolver(MethodId.HTTP_CLIENT_EXECUTE_REQ.getMethodReference());
			addInvocationResolver(resolver);
		}
		{
			HttpClientExecResolver resolver = new HttpClientExecResolver(MethodId.HTTP_CLIENT_EXECUTE_HOST_REQ_HANDLER_CTX.getMethodReference());
			addInvocationResolver(resolver);
		}
		{
			HttpClientExecResolver resolver = new HttpClientExecResolver(MethodId.HTTP_CLIENT_EXECUTE_REQ_CTX.getMethodReference());
			addInvocationResolver(resolver);
		}
		{
			HttpClientExecResolver resolver = new HttpClientExecResolver(MethodId.HTTP_CLIENT_EXECUTE_REQ_HANDLER.getMethodReference());
			addInvocationResolver(resolver);
		}
		{
			HttpClientExecResolver resolver = new HttpClientExecResolver(MethodId.HTTP_CLIENT_EXECUTE_HOST_REQ_HANDLER.getMethodReference());
			addInvocationResolver(resolver);
		}
		{
			HttpClientExecResolver resolver = new HttpClientExecResolver(MethodId.HTTP_CLIENT_EXECUTE_HOST_REQ.getMethodReference());
			addInvocationResolver(resolver);
		}
		{
			HttpClientExecResolver resolver = new HttpClientExecResolver(MethodId.HTTP_CLIENT_EXECUTE_HOST_REQ_CTX.getMethodReference());
			addInvocationResolver(resolver);
		}
	}
	protected void addURLConnectionResolvers()
	{
		addInvocationResolver(new UrlOpenStreamResolver());
		addAllocResolver(new UrlConnectionAllocResolver());
		addReturnTypeResolver(new ReturnUrlConnectionResolver());
	}
	protected void addSocketResolvers()
	{
		addInvocationResolver(new SocketInOutEntityResolver(MethodId.SOCKET_GET_INPUT_STREAM.getMethodReference()));
		addInvocationResolver(new SocketInOutEntityResolver(MethodId.SOCKET_GET_OUTPUT_STREAM.getMethodReference()));
	}
	protected void addNetworkResolvers()
	{
		addHttpClientResolvers();
		addURLConnectionResolvers();
		addSocketResolvers();
	}
	protected void addFileInputStreamResolvers()
	{
		addAllocResolver(new FileInputEntityResolver());
		addReturnTypeResolver(new FileInOutStreamReturnTypeResolver(true));
		addInvocationResolver(new ReturnFileInOutStreamResolver(MethodId.ANDROID_ASSET_MGR_OPEN.getMethodReference(), false, true));
		addInvocationResolver(new ReturnFileInOutStreamResolver(MethodId.ANDROID_ASSET_MGR_OPEN_MODE.getMethodReference(), false, true));
	}
	protected void addFileOutputStreamResolvers()
	{
		addAllocResolver(new FileOutputEntityResolver());
		addReturnTypeResolver(new FileInOutStreamReturnTypeResolver(false));
	}
	protected void addFileResolvers()
	{
		addFileInputStreamResolvers();
		addFileOutputStreamResolvers();
	}
	protected void addSharedPreferencesResolvers()
	{
		addReturnTypeResolver(new ReturnSharedPreferencesResolver());
	}
	@Override
	protected void onInitialize(IClassHierarchy cha)
	{
		if(mInitialized)
			return;
		mInitialized = true;
		Iterator<AndroidListenerSpec> listenersItr = listenerSpecsIterator();
		while(listenersItr.hasNext())
		{
			AndroidListenerSpec spec = listenersItr.next();
			Iterator<IMethod> methodsItr = AndroidListenerSpec.getDefaultListenerMethods(spec.getClassType(), cha);
			while(methodsItr.hasNext())
			{
				IMethod method = methodsItr.next();
				EntryMethodSpec methodSpec = new EntryMethodSpec(method.getReference(), method.isStatic());
				int nParam = methodSpec.getNumberOfParameters();
				for(int i = 1; i < nParam; ++i)
					methodSpec.setParamTrack(i, true);
				spec.addEntryMethod(methodSpec);
			}
		}		
	}
}
