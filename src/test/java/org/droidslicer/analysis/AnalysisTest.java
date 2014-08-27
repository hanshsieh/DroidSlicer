package org.droidslicer.analysis;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.droidslicer.analysis.AndroidAnalysisContext;
import org.droidslicer.analysis.BehaviorGraphBuilder;
import org.droidslicer.graph.BehaviorGraph;
import org.droidslicer.graph.BehaviorSupergraph;
import org.droidslicer.signature.BehaviorSignature;
import org.droidslicer.signature.BehaviorSignaturesTester;
import org.droidslicer.util.GraphUtils;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;

public class AnalysisTest 
{
	private static final Logger mLogger = LoggerFactory.getLogger(AnalysisTest.class);
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_DATABASE = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_DATABASE";
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_DATABASE = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_DATABASE";
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS";
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS";
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_FILE = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_FILE";
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_FILE = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_FILE";
	protected final static String SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_CONTACTS_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS = "SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_CONTACTS_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS";
	protected final static String SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_CONTACTS_TO_DATA_APP_ANY_ICC_RET_CALLER = "SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_CONTACTS_TO_DATA_APP_ANY_ICC_RET_CALLER";
	protected final static String SIG_DATA_APP_ANY_DATABASE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS = "SIG_DATA_APP_ANY_DATABASE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS";
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_READ_PHONE_STATE_TO_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_READ_PHONE_STATE_TO_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET";
	protected final static String SIG_DATA_APP_ANY_FILE_TO_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET = "SIG_DATA_APP_ANY_FILE_TO_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET";
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_READ_PHONE_STATE_TO_DATA_APP_ANY_FILE = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_READ_PHONE_STATE_TO_DATA_APP_ANY_FILE";
	protected final static String SIG_DATA_APP_ANY_FILE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS = "SIG_DATA_APP_ANY_FILE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS";
	protected final static String SIG_DATA_APP_ANY_SHARED_PREFERENCES_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS = "SIG_DATA_APP_ANY_SHARED_PREFERENCES_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS";
	protected final static String SIG_DATA_SYS_ANY_ANDROID_PERMISSION_RECEIVE_BOOT_COMPLETED_TO_DATA_APP_RECEIVER_ICC_PARAM_CALLEE = "SIG_DATA_SYS_ANY_ANDROID_PERMISSION_RECEIVE_BOOT_COMPLETED_TO_DATA_APP_RECEIVER_ICC_PARAM_CALLEE";
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_SHARED_PREFERENCES = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_SHARED_PREFERENCES";
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_SHARED_PREFERENCES = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_SHARED_PREFERENCES";
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET";
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET";
	protected final static String SIG_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET_TO_DATA_APP_ANY_FILE = "SIG_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET_TO_DATA_APP_ANY_FILE";
	protected final static String SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_WRITE_EXTERNAL_STORAGE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS = "SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_WRITE_EXTERNAL_STORAGE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS";
	protected final static String SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_EXTERNAL_STORAGE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS = "SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_EXTERNAL_STORAGE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS";
	protected final static String SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_EXTERNAL_STORAGE_TO_DATA_APP_ANY_ICC_RET_CALLER = "SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_EXTERNAL_STORAGE_TO_DATA_APP_ANY_ICC_RET_CALLER";
	protected final static String SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_WRITE_EXTERNAL_STORAGE_TO_DATA_APP_ANY_ICC_RET_CALLER = "SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_WRITE_EXTERNAL_STORAGE_TO_DATA_APP_ANY_ICC_RET_CALLER";
	private final static File SIG_FILE = new File("config/semantic_signatures_full.xml");
	protected AndroidAnalysisContext mAnalysisCtx;
	protected BehaviorSupergraph mSupergraph;
	protected Collection<BehaviorSignature> mMatchedSigs;
	@Before
	public void setUp() throws Exception 
	{
		mMatchedSigs = null;
		mSupergraph = null;
	}
	
	protected void analyze(File sample)
		throws Exception
	{
		ProgressMonitor monitor = new ProgressMonitor();
		try
		{
			monitor.beginTask("Start analyzing sample", 1000);
			mAnalysisCtx = AndroidAnalysisContext.makeDefault(sample, new SubProgressMonitor(monitor, 100));
			BehaviorGraph graph;
			{
				BehaviorGraphBuilder graphBuilder = 
		    			new BehaviorGraphBuilder(mAnalysisCtx);
				graph = graphBuilder.build(new SubProgressMonitor(monitor, 800));
			}
			{
				BehaviorSignaturesTester sigTester = new BehaviorSignaturesTester(graph, SIG_FILE);
				sigTester.test(new SubProgressMonitor(monitor, 100));
				mSupergraph = sigTester.getSupergraph();
				mMatchedSigs = sigTester.getMatchedSignatures();
			}
		}
		finally
		{
			monitor.done();
		}
	}
	protected void checkMatchedSigs(String[] expectSigs)
		throws Exception
	{
		if(mMatchedSigs.size() != expectSigs.length)
			throw new Exception("Unexpected number of matched signatures");
		for(String expectSig : expectSigs)
		{
			boolean found = false;
			for(BehaviorSignature sig : mMatchedSigs)
			{
				if(sig.getName().equals(expectSig))
				{
					found = true;
					break;
				}
			}
			if(!found)
				throw new Exception("Expected signature " + expectSig + " isn't found");
		}
	}

	@After
	public void tearDown() throws Exception
	{
		mAnalysisCtx = null;
		mSupergraph = null;
		mMatchedSigs = null;
	}

	@Test
	public void testBasicGPS2SMSLeak() throws Exception
	{
		File sample = new File("data/testdata/android/BasicGPS2SMSLeak/apk/BasicGPS2SMSLeak.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testBasicGPS2SMSLeak2() throws Exception
	{
		File sample = new File("data/testdata/android/BasicGPS2SMSLeak2/apk/BasicGPS2SMSLeak2.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testBasicLocation2File() throws Exception
	{
		File sample = new File("data/testdata/android/BasicLocation2File/apk/BasicLocation2File.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_FILE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_FILE
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testBufferedLocation2File() throws Exception
	{
		File sample = new File("data/testdata/android/BufferedLocation2File/apk/BufferedLocation2File.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_FILE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_FILE
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testBufferedLocation2File2() throws Exception
	{
		File sample = new File("data/testdata/android/BufferedLocation2File2/apk/BufferedLocation2File2.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_FILE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_FILE
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testContactsLeak() throws Exception
	{
		File sample = new File("data/testdata/android/ContactsLeak/apk/ContactsLeak.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_CONTACTS_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_CONTACTS_TO_DATA_APP_ANY_ICC_RET_CALLER
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testContentProviderAccess() throws Exception
	{
		File sample = new File("data/testdata/android/ContentProviderAccess/apk/ContentProviderAccess.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testContentProviderAccess2() throws Exception
	{
		File sample = new File("data/testdata/android/ContentProviderAccess2/apk/ContentProviderAccess2.apk");
		analyze(sample);
		String[] expectSigs = {};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testContentProviderAccess3() throws Exception
	{
		File sample = new File("data/testdata/android/ContentProviderAccess3/apk/ContentProviderAccess3.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testContentProviderAccess4() throws Exception
	{
		File sample = new File("data/testdata/android/ContentProviderAccess4/apk/ContentProviderAccess4.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_DATABASE
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testContentProviderAccess5() throws Exception
	{
		File sample = new File("data/testdata/android/ContentProviderAccess5/apk/ContentProviderAccess5.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testContentProviderAccess6() throws Exception
	{
		File sample = new File("data/testdata/android/ContentProviderAccess6/apk/ContentProviderAccess6.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_DATABASE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testContentProviderAccess7() throws Exception
	{
		File sample = new File("data/testdata/android/ContentProviderAccess7/apk/ContentProviderAccess7.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_WRITE_EXTERNAL_STORAGE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_EXTERNAL_STORAGE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_EXTERNAL_STORAGE_TO_DATA_APP_ANY_ICC_RET_CALLER,
			SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_WRITE_EXTERNAL_STORAGE_TO_DATA_APP_ANY_ICC_RET_CALLER
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testContentProviderAccess8() throws Exception
	{
		File sample = new File("data/testdata/android/ContentProviderAccess8/apk/ContentProviderAccess8.apk");
		analyze(sample);
		String[] expectSigs = {};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testEncryptedPhoneState2URL() throws Exception
	{
		File sample = new File("data/testdata/android/EncryptedPhoneState2URL/apk/EncryptedPhoneState2URL.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_READ_PHONE_STATE_TO_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET,
			SIG_DATA_APP_ANY_FILE_TO_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_READ_PHONE_STATE_TO_DATA_APP_ANY_FILE
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testExplicitInterActivityLeak() throws Exception
	{
		File sample = new File("data/testdata/android/ExplicitInterActivityLeak/apk/ExplicitInterActivityLeak.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testExplicitInterActivityLeak2() throws Exception
	{
		File sample = new File("data/testdata/android/ExplicitInterActivityLeak2/apk/ExplicitInterActivityLeak2.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testExplicitInterActivityLeak3() throws Exception
	{
		File sample = new File("data/testdata/android/ExplicitInterActivityLeak3/apk/ExplicitInterActivityLeak3.apk");
		analyze(sample);
		String[] expectSigs = {};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testGPS2SMSLeakViaHeap() throws Exception
	{
		File sample = new File("data/testdata/android/GPS2SMSLeakViaHeap/apk/GPS2SMSLeakViaHeap.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testImplicitICCLeak() throws Exception
	{
		File sample = new File("data/testdata/android/ImplicitICCLeak/apk/ImplicitICCLeak.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testImplicitICCLeak2() throws Exception
	{
		File sample = new File("data/testdata/android/ImplicitICCLeak2/apk/ImplicitICCLeak2.apk");
		analyze(sample);
		String[] expectSigs = {};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testLeakViaBroadcast() throws Exception
	{
		File sample = new File("data/testdata/android/LeakViaBroadcast/apk/LeakViaBroadcast.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testLeakViaAsyncTask() throws Exception
	{
		File sample = new File("data/testdata/android/LeakViaAsyncTask/apk/LeakViaAsyncTask.apk");
		analyze(sample);
		String[] expectSigs = {};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testLeakViaAsyncTask2() throws Exception
	{
		mLogger.warn("Because we currently ignore the dependency via class fields of library, this test case will be skipped");
		/*
		File sample = new File("data/testdata/android/LeakViaAsyncTask2/apk/LeakViaAsyncTask2.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);*/
	}
	@Test
	public void testLeakViaFile() throws Exception
	{
		File sample = new File("data/testdata/android/LeakViaFile/apk/LeakViaFile.apk");
		analyze(sample);
		String[] expectSigs = {
				SIG_DATA_APP_ANY_FILE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_FILE,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_FILE,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testLeakViaFile2() throws Exception
	{
		File sample = new File("data/testdata/android/LeakViaFile2/apk/LeakViaFile2.apk");
		analyze(sample);
		String[] expectSigs = {
				SIG_DATA_APP_ANY_FILE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_FILE,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_FILE,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testLeakViaFile3() throws Exception
	{
		File sample = new File("data/testdata/android/LeakViaFile3/apk/LeakViaFile3.apk");
		analyze(sample);
		String[] expectSigs = {
				SIG_DATA_APP_ANY_FILE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_FILE,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_FILE
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testLeakViaFile4() throws Exception
	{
		File sample = new File("data/testdata/android/LeakViaFile4/apk/LeakViaFile4.apk");
		analyze(sample);
		String[] expectSigs = {
				SIG_DATA_APP_ANY_FILE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_FILE,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_FILE
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testLeakViaSharedPreferences() throws Exception
	{
		File sample = new File("data/testdata/android/LeakViaSharedPreferences/apk/LeakViaSharedPreferences.apk");
		analyze(sample);
		String[] expectSigs = {
				SIG_DATA_APP_ANY_SHARED_PREFERENCES_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
				SIG_DATA_SYS_ANY_ANDROID_PERMISSION_RECEIVE_BOOT_COMPLETED_TO_DATA_APP_RECEIVER_ICC_PARAM_CALLEE,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_SHARED_PREFERENCES,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_SHARED_PREFERENCES,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testLeakViaSharedPreferences2() throws Exception
	{
		File sample = new File("data/testdata/android/LeakViaSharedPreferences2/apk/LeakViaSharedPreferences2.apk");
		analyze(sample);
		String[] expectSigs = {
				SIG_DATA_SYS_ANY_ANDROID_PERMISSION_RECEIVE_BOOT_COMPLETED_TO_DATA_APP_RECEIVER_ICC_PARAM_CALLEE,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_SHARED_PREFERENCES,
				SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_SHARED_PREFERENCES,
				SIG_DATA_APP_ANY_SHARED_PREFERENCES_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testDynamicReceiverRegister() throws Exception
	{
		File sample = new File("data/testdata/android/DynamicReceiverRegister/apk/DynamicReceiverRegister.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_CONTACTS_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_CONTACTS_TO_DATA_APP_ANY_ICC_RET_CALLER
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testDynamicReceiverRegister2() throws Exception
	{
		File sample = new File("data/testdata/android/DynamicReceiverRegister2/apk/DynamicReceiverRegister2.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_CONTACTS_TO_DATA_APP_ANY_ICC_RET_CALLER
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testDynamicReceiverRegister3() throws Exception
	{
		File sample = new File("data/testdata/android/DynamicReceiverRegister3/apk/DynamicReceiverRegister3.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_SYS_PROVIDER_ANDROID_PERMISSION_READ_CONTACTS_TO_DATA_APP_ANY_ICC_RET_CALLER
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testSocketLeak() throws Exception
	{
		File sample = new File("data/testdata/android/SocketLeak/apk/SocketLeak.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET_TO_DATA_APP_ANY_FILE
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testSQLiteAccess() throws Exception
	{
		File sample = new File("data/testdata/android/SQLiteAccess/apk/SQLiteAccess.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_DATABASE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testSQLiteAccess2() throws Exception
	{
		File sample = new File("data/testdata/android/SQLiteAccess2/apk/SQLiteAccess2.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_DATABASE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_DATABASE,
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testSQLiteAccess3() throws Exception
	{
		File sample = new File("data/testdata/android/SQLiteAccess3/apk/SQLiteAccess3.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_DATABASE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testSQLiteAccess4() throws Exception
	{
		File sample = new File("data/testdata/android/SQLiteAccess4/apk/SQLiteAccess4.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_DATABASE_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_DATABASE,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_FINE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS,
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_ACCESS_COARSE_LOCATION_TO_DATA_APP_ANY_ANDROID_PERMISSION_SEND_SMS
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testURLConnectionToFile() throws Exception
	{
		File sample = new File("data/testdata/android/URLConnectionToFile/apk/URLConnectionToFile.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET_TO_DATA_APP_ANY_FILE
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testURLConnectionToFile2() throws Exception
	{
		File sample = new File("data/testdata/android/URLConnectionToFile2/apk/URLConnectionToFile2.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET_TO_DATA_APP_ANY_FILE
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testURLConnectionToFile3() throws Exception
	{
		File sample = new File("data/testdata/android/URLConnectionToFile3/apk/URLConnectionToFile3.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET_TO_DATA_APP_ANY_FILE
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testURLConnectionToFile4() throws Exception
	{
		File sample = new File("data/testdata/android/URLConnectionToFile4/apk/URLConnectionToFile4.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET_TO_DATA_APP_ANY_FILE
		};
		checkMatchedSigs(expectSigs);
	}
	@Test
	public void testURLConnectionToFile5() throws Exception
	{
		File sample = new File("data/testdata/android/URLConnectionToFile5/apk/URLConnectionToFile5.apk");
		analyze(sample);
		String[] expectSigs = {
			SIG_DATA_APP_ANY_ANDROID_PERMISSION_INTERNET_TO_DATA_APP_ANY_FILE
		};
		checkMatchedSigs(expectSigs);
	}
}
