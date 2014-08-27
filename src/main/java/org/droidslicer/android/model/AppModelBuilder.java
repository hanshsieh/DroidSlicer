/**
 *
 * Copyright (c) 2009-2012,
 *
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, Rogan Creswick <creswick@galois.com>)
 *  Steve Suh    <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */
package org.droidslicer.android.model;

import org.droidslicer.analysis.AndroidAppInfo;
import org.droidslicer.config.AbstractAnalysisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;

public class AppModelBuilder
{
	private final static Logger mLogger = LoggerFactory.getLogger(AppModelBuilder.class);
	private final static String PACKAGE_PREFIX = "droidslicer_";
	private static final String ANDROID_MODEL_CLASS_NAME = "AndroidModel";
	private static final String CONTEXT_IMPL_CLASS_NAME = "ContextImpl";
    
    private IClassHierarchy mCha;
    
    private AnalysisScope mScope;
                
    private final AbstractAnalysisConfig mAnalysisConfig;
        
    private final AndroidAppInfo mAppInfo;
    
    private FakeContextImpl mFakeContextClazz;
    
    private AppModelClass mAppModelClazz;
    
	
	public AppModelBuilder(IClassHierarchy cha, AnalysisScope scope, AbstractAnalysisConfig config, AndroidAppInfo appInfo)
	{
    	this.mCha = cha;
    	this.mScope = scope;
    	this.mAnalysisConfig = config;
    	this.mAppInfo = appInfo;
    }
	public FakeContextImpl getFakeContextClass()
	{
		return mFakeContextClazz;
	}
	public AppModelClass getAppModelClass()
	{
		return mAppModelClazz;
	}
	public IMethod getEntryMethod()
	{
		return mAppModelClazz.getEntryMethod();
	}
	public void build()
	{
		TypeReference appModelClassRef = null, ctxImplClassRef = null;
		ClassLoaderReference loader = mScope.getLoader(AnalysisScope.SYNTHETIC);
		
		// Create a valid class name for entry class and context implementation
		do
		{
			String pkgName = PACKAGE_PREFIX + ((int)(Math.random() * 1000000));
			//String pkgName = PACKAGE_PREFIX; // debug
			appModelClassRef = TypeReference.findOrCreate(loader, 
					"L" + pkgName + "/" + ANDROID_MODEL_CLASS_NAME);
			ctxImplClassRef = TypeReference.findOrCreate(loader, 
					"L" + pkgName + "/" + CONTEXT_IMPL_CLASS_NAME);
			if(mCha.lookupClass(appModelClassRef) == null && mCha.lookupClass(ctxImplClassRef) == null)
				break;
		}while(true);
		
		mAppInfo.setContextImplType(ctxImplClassRef);

		// Create a synthetic class extending android.content.Context
		mFakeContextClazz = new FakeContextImpl(ctxImplClassRef, appModelClassRef, mCha);

		// Add the class to class hierarchy
		mCha.addClass(mFakeContextClazz);

		// Create a synthetic class for the Android model
		mAppModelClazz = new AppModelClass(appModelClassRef, mCha, mAnalysisConfig, mAppInfo);
		
		// Add the class to class hierarchy
		mCha.addClass(mAppModelClazz);
		
		if(mLogger.isDebugEnabled())
		{
			mLogger.debug("Context implementation: \n{}", mFakeContextClazz.toString());
			mLogger.debug("App model: \n{}", mAppModelClazz.toString());
		}
	}
}
