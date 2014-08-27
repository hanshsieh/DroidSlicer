package org.droidslicer.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.droidslicer.android.appSpec.AndroidActivitySpec;
import org.droidslicer.android.appSpec.AndroidApplicationSpec;
import org.droidslicer.android.appSpec.AndroidListenerSpec;
import org.droidslicer.android.appSpec.AndroidProviderSpec;
import org.droidslicer.android.appSpec.AndroidReceiverSpec;
import org.droidslicer.android.appSpec.AndroidServiceSpec;
import org.droidslicer.android.appSpec.AppComponentSpec;
import org.droidslicer.android.appSpec.EntryCompSpec;
import org.droidslicer.graph.entity.resolver.AllocationEntityResolver;
import org.droidslicer.graph.entity.resolver.InvocationEntityResolver;
import org.droidslicer.graph.entity.resolver.ReturnTypeResolver;

import com.google.common.collect.Iterators;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

public abstract class AbstractAnalysisConfig
{
	private boolean mInitialized = false;
	private final HashMap<TypeReference, AndroidListenerSpec> mListenerSpecs = new HashMap<TypeReference, AndroidListenerSpec>();
	private AndroidActivitySpec mActivitySpec = new AndroidActivitySpec();
	private final AndroidReceiverSpec mReceiverSpec = new AndroidReceiverSpec();
	private final AndroidProviderSpec mProviderSpec = new AndroidProviderSpec();
	private final AndroidServiceSpec mServiceSpec = new AndroidServiceSpec();
	private final AndroidApplicationSpec mAppSpec = new AndroidApplicationSpec();
	private final HashMap<MethodReference, InvocationEntityResolver> mInvokeResolvers = 
			new HashMap<MethodReference, InvocationEntityResolver>();
	private final HashMap<TypeName, ReturnTypeResolver> mRetTypeResolver = new HashMap<TypeName, ReturnTypeResolver>();
	private final HashMap<TypeReference, AllocationEntityResolver> mAllocResolvers = 
			new HashMap<TypeReference, AllocationEntityResolver>();
	private final Collection<IntentPermission> mIntentPerms = new ArrayList<IntentPermission>();
	private final Collection<ProviderPermission> mProviderPerms = new ArrayList<ProviderPermission>();
	public AbstractAnalysisConfig()
	{}
	public void initialize(IClassHierarchy cha)
	{
		if(mInitialized)
			throw new IllegalStateException("Already initialized");
		onInitialize(cha);
		mInitialized = true;
	}
	public boolean isInitialized()
	{
		return mInitialized;
	}
	protected abstract void onInitialize(IClassHierarchy cha);
	public void addListenerClass(AndroidListenerSpec listenerSpec)
	{
		if(listenerSpec == null)
			throw new IllegalArgumentException();
		mListenerSpecs.put(listenerSpec.getClassType(), listenerSpec);
	}
	public Iterator<AndroidListenerSpec> listenerSpecsIterator()
	{
		return mListenerSpecs.values().iterator();
	}
	public boolean isListenerClass(TypeReference type)
	{
		return mListenerSpecs.containsKey(type);
	}
	public EntryCompSpec getEntryCompSpec(TypeReference typeRef)
	{
		if(typeRef == null)
			throw new IllegalArgumentException();
		EntryCompSpec spec = mListenerSpecs.get(typeRef);
		if(spec != null)
			return spec;
		if(mActivitySpec.getClassType().equals(typeRef))
			return mActivitySpec;
		if(mReceiverSpec.getClassType().equals(typeRef))
			return mReceiverSpec;
		if(mProviderSpec.getClassType().equals(typeRef))
			return mProviderSpec;
		if(mServiceSpec.getClassType().equals(typeRef))
			return mServiceSpec;
		if(mAppSpec.getClassType().equals(typeRef))
			return mAppSpec;
		return null;
	}
	public AndroidListenerSpec getListenerSpec(TypeReference type)
	{
		return mListenerSpecs.get(type);
	}
	public void addReturnTypeResolver(ReturnTypeResolver resolver)
	{
		mRetTypeResolver.put(resolver.getReturnType().getName(), resolver);
	}
	public ReturnTypeResolver getReturnTypeResolver(TypeName retTypeName)
	{
		return mRetTypeResolver.get(retTypeName);
	}
	public AllocationEntityResolver addAllocResolver(AllocationEntityResolver resolver)
	{
		return mAllocResolvers.put(resolver.getType(), resolver);
	}
	public AllocationEntityResolver getAllocResolver(TypeReference type)
	{
		return mAllocResolvers.get(type);
	}
	public Iterator<AllocationEntityResolver> allocResolversIterator()
	{
		return mAllocResolvers.values().iterator();
	}
	public InvocationEntityResolver addInvocationResolver(InvocationEntityResolver resolver)
	{
		return mInvokeResolvers.put(resolver.getMethodReference(), resolver);
	}
	public InvocationEntityResolver findInvocationResolver(MethodReference methodRef)
	{
		return mInvokeResolvers.get(methodRef);
	}
	public Iterator<InvocationEntityResolver> invocationResolversIterator()
	{
		return mInvokeResolvers.values().iterator();
	}
	public AndroidApplicationSpec getApplicationSpec()
	{
		return mAppSpec;
	}
	public AndroidActivitySpec getActivitySpec()
	{
		return mActivitySpec;
	}
	public AndroidReceiverSpec getReceiverSpec()
	{
		return mReceiverSpec;
	}
	public AndroidProviderSpec getProviderSpec()
	{
		return mProviderSpec;
	}
	public AndroidServiceSpec getServiceSpec()
	{
		return mServiceSpec;
	}
	public Iterator<AppComponentSpec> appComponentSpecsIterator()
	{
		ArrayList<AppComponentSpec> specs = new ArrayList<AppComponentSpec>(4);
		specs.add(getActivitySpec());
		specs.add(getReceiverSpec());
		specs.add(getProviderSpec());
		specs.add(getServiceSpec());
		return specs.iterator();
	}
	public Iterator<EntryCompSpec> entryComponentSpecsIterator()
	{
		ArrayList<EntryCompSpec> specs = new ArrayList<EntryCompSpec>(4);
		specs.add(getActivitySpec());
		specs.add(getReceiverSpec());
		specs.add(getProviderSpec());
		specs.add(getServiceSpec());
		specs.add(getApplicationSpec());
		return Iterators.concat(specs.iterator(), mListenerSpecs.values().iterator());
	}
	public void addProviderPermission(ProviderPermission providerPerm)
	{
		mProviderPerms.add(providerPerm);
	}
	public Collection<ProviderPermission> getProviderPermissions()
	{
		return mProviderPerms;
	}
	public void addIntentPermission(IntentPermission intentPerm)
	{
		mIntentPerms.add(intentPerm);
	}
	public Collection<IntentPermission> getIntentPermissions()
	{
		return mIntentPerms;
	}
	
	public void close()
		throws IOException
	{}
}
