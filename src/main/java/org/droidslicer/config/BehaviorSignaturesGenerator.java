package org.droidslicer.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.droidslicer.config.APIPermissionParser.APIPermission;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class BehaviorSignaturesGenerator
{
	protected static class Permissions
	{
		private final Collection<String> mApiPerms;
		private final Collection<String> mIntentSenderPerms;
		private final Collection<String> mIntentReceiverPerms;
		private final Collection<String> mProviderReadPerms;
		private final Collection<String> mProviderWritePerms;
		public Permissions(
			Collection<String> apiPerms, 
			Collection<String> intentSenderPerms, Collection<String> intentReceiverPerms, 
			Collection<String> providerReadPerms, Collection<String> providerWritePerms)
		{
			mApiPerms = apiPerms;
			mIntentSenderPerms = intentSenderPerms;
			mIntentReceiverPerms = intentReceiverPerms;
			mProviderReadPerms = providerReadPerms;
			mProviderWritePerms = providerWritePerms;
		}
		public Collection<String> getAPIPermissions()
		{
			return mApiPerms;
		}
		public Collection<String> getIntentSenderPermissions()
		{
			return mIntentSenderPerms;
		}
		public Collection<String> getIntentReceiverPermissions()
		{
			return mIntentReceiverPerms;
		}
		public Collection<String> getProviderReadPermissions()
		{
			return mProviderReadPerms;
		}
		public Collection<String> getProviderWritePermissions()
		{
			return mProviderWritePerms;
		}
	}
	private static class ReadPermissionPredicate implements Predicate<String>
	{
		@Override
		public boolean apply(String perm)
		{
			String[] toks = perm.split("\\.");
			if(toks.length == 0)
				return false;
			String name = toks[toks.length - 1];
			if(name.startsWith("READ_") || 
				name.startsWith("RECEIVE_") ||
				name.startsWith("ACCESS_") ||
				name.startsWith("GET_") ||
				name.equals("INTERNET") || 
				name.equals("BLUETOOTH") ||
				name.equals("CAMERA") || 
				name.startsWith("RECORD_"))
				return true;
			return false;
		}
	}
	private static class WritePermissionPredicate implements Predicate<String>
	{
		@Override
		public boolean apply(String perm)
		{
			String[] toks = perm.split("\\.");
			if(toks.length == 0)
				return false;
			String name = toks[toks.length - 1];
			if(name.startsWith("WRITE_") || 
				name.startsWith("SEND_") ||
				name.equals("INTERNET") || 
				name.equals("BLUETOOTH"))
				return true;
			return false;
		}
	}
	private static class InterestedPermissionPredicate implements Predicate<String>
	{
		private final Set<String> mPerms = new HashSet<String>();
		public InterestedPermissionPredicate()
		{
			mPerms.add("android.permission.ACCESS_COARSE_LOCATION");
			mPerms.add("android.permission.ACCESS_FINE_LOCATION");
			//mPerms.add("android.permission.BLUETOOTH");
			mPerms.add("android.permission.INTERNET");
			mPerms.add("android.permission.READ_CALENDAR");
			mPerms.add("android.permission.READ_CALL_LOG");
			mPerms.add("android.permission.READ_CONTACTS");
			mPerms.add("com.android.browser.permission.READ_HISTORY_BOOKMARKS");
			mPerms.add("android.permission.READ_EXTERNAL_STORAGE");
			mPerms.add("android.permission.WRITE_EXTERNAL_STORAGE");
			//mPerms.add("android.permission.READ_LOGS");
			mPerms.add("android.permission.READ_PHONE_STATE");
			//mPerms.add("android.permission.READ_PROFILE");
			mPerms.add("android.permission.READ_SMS");
			mPerms.add("android.permission.RECEIVE_MMS");
			mPerms.add("android.permission.RECEIVE_SMS");
			mPerms.add("android.permission.SEND_SMS");
			mPerms.add("android.permission.WRITE_SMS");
			//mPerms.add("android.permission.CAMERA");
			//mPerms.add("android.permission.RECORD_AUDIO");
		}
		@Override
		public boolean apply(String perm)
		{
			return mPerms.contains(perm);
		}		
	}
	private final static String USAGE = "-p <permission_list_file> -o <output_file> [-s] [-f]";
	private final static String CMD_INTENT_PERM = "i";
	private final static String CMD_API_PERM = "a";
	private final static String CMD_PROVIDER_PERM = "p";
	private final static String CMD_OUTPUT = "o";
	private final static String CMD_TRACK_SYS_COMP_ACCESS = "s";
	private final static String CMD_FILTER_INTERESTED_PERMS = "f";
	private final static String DATA_FILE = "FILE";
	private final static String DATA_DATABASE = "DATABASE";
	private final static String DATA_SHARED_PREFERENCES = "SHARED_PREFERENCES";
	private final static String DATA_ICC_PARAM_CALLER = "ICC_PARAM_CALLER";
	private final static String DATA_ICC_PARAM_CALLEE = "ICC_PARAM_CALLEE";
	private final static String DATA_ICC_RET_CALLER = "ICC_RET_CALLER";
	private final static String PERM_INTERNET = "android.permission.INTERNET";
	private final static String PERM_SEND_SMS = "android.permission.SEND_SMS";
	private final static String PERM_WRITE_SMS = "android.permission.WRITE_SMS";
	private ArrayList<String> mFlowIds = new ArrayList<String>();
	protected XMLStreamWriter mXmlWriter = null;
	private boolean mTrackSystemCompAccess = false;
	private Predicate<String> mInterestedPermPred = new InterestedPermissionPredicate();
	private Predicate<String> mReadPermPred = new ReadPermissionPredicate();
	private Predicate<String> mWritePermPred = new WritePermissionPredicate();
	private int mIndent = 0;
	private static Options buildOptions()
	{
		Options options = new Options();
		options.addOption(CMD_INTENT_PERM, true, "Intent permission file")
			.addOption(CMD_API_PERM, true, "API permission file")
			.addOption(CMD_PROVIDER_PERM, true, "Provider permission file")
			.addOption(CMD_OUTPUT, true, "Output file")
			.addOption(CMD_TRACK_SYS_COMP_ACCESS, false, "Track access to system components")
			.addOption(CMD_FILTER_INTERESTED_PERMS, false, "Should filter interested permissions");
		return options;
	}
	protected static void printHelp(Options options)
	{
		HelpFormatter helpFormatter = new HelpFormatter();  
		helpFormatter.printHelp(USAGE, options);
	}
	public BehaviorSignaturesGenerator(
			File apiPermFile, File intentPermFile, File providerPermFile, File outputFile, boolean trackSysCompAccess, boolean filterInterestedPerms)
		throws IOException
	{
		mTrackSystemCompAccess = trackSysCompAccess;
		if(filterInterestedPerms)
			mInterestedPermPred = new InterestedPermissionPredicate();
		else
			mInterestedPermPred = Predicates.alwaysTrue();
		SortedSet<String> apiPerms = readFromAPIPerm(apiPermFile);
		SortedSet<String> intentSenderPerms;
		SortedSet<String> intentReceiverPerms;
		{
			SortedSet<String>[] intentPerms = readFromIntentPerm(intentPermFile);
			intentSenderPerms = intentPerms[0];
			intentReceiverPerms = intentPerms[1];
		}
		SortedSet<String> providerReadPerms, providerWritePerms;
		{
			SortedSet<String>[] providerPerms = readFromProviderPerm(providerPermFile);
			providerReadPerms = providerPerms[0];
			providerWritePerms = providerPerms[1];
		}
		Permissions perms = new Permissions(apiPerms, intentSenderPerms, intentReceiverPerms, providerReadPerms, providerWritePerms);
		outputSignatures(perms, outputFile);		
	}
	protected SortedSet<String> readFromAPIPerm(File apiPermFile)
			throws IOException
	{
		SortedSet<String> perms = new TreeSet<String>();
		APIPermissionParser apiPermParser = null;
		InputStream apiPermInput = null;
		try
		{
			apiPermInput = new FileInputStream(apiPermFile);
			apiPermParser = new APIPermissionParser(apiPermInput);
			APIPermission apiPerm;
			while((apiPerm = apiPermParser.read()) != null)
			{
				perms.addAll(apiPerm.getPermissions());
			}
			return perms;
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
			if(apiPermInput != null)
			{
				try
				{
					apiPermInput.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	protected SortedSet<String>[] readFromIntentPerm(File intentPermFile)
			throws IOException
	{
		@SuppressWarnings("unchecked")
		SortedSet<String>[] perms = new SortedSet[]{new TreeSet<String>(), new TreeSet<String>()};
		IntentPermissionParser intentPermParser = null;
		InputStream intentPermInput = null;
		try
		{
			intentPermInput = new FileInputStream(intentPermFile);
			intentPermParser = new IntentPermissionParser(intentPermInput);
			IntentPermission intentPerm;
			while((intentPerm = intentPermParser.read()) != null)
			{
				perms[0].addAll(intentPerm.getSenderPermissions());
				perms[1].addAll(intentPerm.getReceiverPermissions());
			}
			return perms;
		}
		finally
		{
			if(intentPermParser != null)
			{
				try
				{
					intentPermParser.close();
				}
				catch(Exception ex)
				{}
			}
			if(intentPermInput != null)
			{
				try
				{
					intentPermInput.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	protected SortedSet<String>[] readFromProviderPerm(File providerPermFile)
			throws IOException
	{
		@SuppressWarnings("unchecked")
		SortedSet<String>[] perms = new SortedSet[]{new TreeSet<String>(), new TreeSet<String>()};
		ProviderPermissionParser providerPermParser = null;
		InputStream providerPermInput = null;
		try
		{
			providerPermInput = new FileInputStream(providerPermFile);
			providerPermParser = new ProviderPermissionParser(providerPermInput);
			ProviderPermission providerPerm;
			while((providerPerm = providerPermParser.read()) != null)
			{
				String readPerm = providerPerm.getReadPermission();
				String writePerm = providerPerm.getWritePermission();
				if(readPerm != null)
					perms[0].add(readPerm);
				if(writePerm != null)
					perms[1].add(writePerm);
				for(PathPermission pathPerm : providerPerm.getPathPermissions())
				{
					readPerm = pathPerm.getReadPermission();
					writePerm = pathPerm.getWritePermission();
					if(readPerm != null)
						perms[0].add(readPerm);
					if(writePerm != null)
						perms[1].add(writePerm);
				}
			}
			return perms;
		}
		finally
		{
			if(providerPermParser != null)
			{
				try
				{
					providerPermParser.close();
				}
				catch(Exception ex)
				{}
			}
			if(providerPermInput != null)
			{
				try
				{
					providerPermInput.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	protected void writeStartElement(String localName)
			throws XMLStreamException
	{
		mXmlWriter.writeCharacters("\n");
		for(int i = 0; i < mIndent; ++i)
		{
			mXmlWriter.writeCharacters("\t");
		}
		mXmlWriter.writeStartElement(localName);
		++mIndent;
	}
	protected void writeEmptyElement(String localName)
			throws XMLStreamException
	{
		mXmlWriter.writeCharacters("\n");
		for(int i = 0; i < mIndent; ++i)
			mXmlWriter.writeCharacters("\t");
		mXmlWriter.writeEmptyElement(localName);
	}
	protected void writeEndElement()
		throws XMLStreamException
	{
		mXmlWriter.writeCharacters("\n");
		--mIndent;
		for(int i = 0; i < mIndent; ++i)
			mXmlWriter.writeCharacters("\t");
		mXmlWriter.writeEndElement();
	}
	protected String getNameForPermission(String perm)
	{
		return perm.replace('.', '_').toUpperCase();
	}
	protected String getIdForAppData(String name, String type)
	{
		return "DATA_APP_" + type.toUpperCase() + "_" + name.toUpperCase();
	}
	protected String getIdForSysPermissionData(String name, String type)
	{
		return "DATA_SYS_" + type.toUpperCase() + "_" + name.toUpperCase();
	}
	protected String getIdForPermissionFlow(String from, String to)
	{
		return "FLOW_" + from.toUpperCase() + "_TO_" + to.toUpperCase();
	}
	protected void writeDataSpec(Permissions perms)
		throws XMLStreamException
	{
		writeStartElement(BehaviorSignatureXMLElement.DATA_SPEC.getTagName());
		
		{
			writeStartElement(BehaviorSignatureXMLElement.COMPONENT.getTagName());
			mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_IS_SYSTEM, BehaviorSignatureXMLElement.V_FALSE);
			mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TYPE, BehaviorSignatureXMLElement.V_ANY);
			
			writeComment("Invocation of permission-protected API in app");
			for(String perm : perms.getAPIPermissions())
			{
				writeStartElement(BehaviorSignatureXMLElement.DATA.getTagName());
				String id = getIdForAppData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_ANY);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, id);
				writeEmptyElement(BehaviorSignatureXMLElement.PERMISSION.getTagName());
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_PERMISSIONS, perm);
				writeEndElement();
			}
			
			// File
			{
				writeStartElement(BehaviorSignatureXMLElement.DATA.getTagName());
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, getIdForAppData(DATA_FILE, BehaviorSignatureXMLElement.V_ANY));
				writeEmptyElement(BehaviorSignatureXMLElement.FILE.getTagName());
				writeEndElement();
			}
			
			// Database
			{
				writeStartElement(BehaviorSignatureXMLElement.DATA.getTagName());
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, getIdForAppData(DATA_DATABASE, BehaviorSignatureXMLElement.V_ANY));
				writeEmptyElement(BehaviorSignatureXMLElement.DATABASE.getTagName());
				writeEndElement();
			}
			
			// Shared preferences
			{
				writeStartElement(BehaviorSignatureXMLElement.DATA.getTagName());
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, getIdForAppData(DATA_SHARED_PREFERENCES, BehaviorSignatureXMLElement.V_ANY));
				writeEmptyElement(BehaviorSignatureXMLElement.SHARED_PREFERENCES.getTagName());
				writeEndElement();
			}
			
			// ICC parameter caller
			{
				writeStartElement(BehaviorSignatureXMLElement.DATA.getTagName());
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, getIdForAppData(DATA_ICC_PARAM_CALLER, BehaviorSignatureXMLElement.V_ANY));
				writeEmptyElement(BehaviorSignatureXMLElement.ICC_PARAM_CALLER.getTagName());
				writeEndElement();
			}
				
			// ICC return caller
			{
				writeStartElement(BehaviorSignatureXMLElement.DATA.getTagName());
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, getIdForAppData(DATA_ICC_RET_CALLER, BehaviorSignatureXMLElement.V_ANY));
				writeEmptyElement(BehaviorSignatureXMLElement.ICC_RET_CALLER.getTagName());
				writeEndElement();
			}
			
			writeEndElement();
		}
		
		{
			writeStartElement(BehaviorSignatureXMLElement.COMPONENT.getTagName());
			mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_IS_SYSTEM, BehaviorSignatureXMLElement.V_FALSE);
			mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TYPE, BehaviorSignatureXMLElement.V_RECEIVER);
			// ICC parameter callee of receiver
			{
				writeStartElement(BehaviorSignatureXMLElement.DATA.getTagName());
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, getIdForAppData(DATA_ICC_PARAM_CALLEE, BehaviorSignatureXMLElement.V_RECEIVER));
				writeEmptyElement(BehaviorSignatureXMLElement.ICC_PARAM_CALLEE.getTagName());
				writeEndElement();
			}
			writeEndElement();
		}
		
		{
			writeComment("Sensitive-data-use point flowing out sensitive data in a system component");
			writeStartElement(BehaviorSignatureXMLElement.COMPONENT.getTagName());
			mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_IS_SYSTEM, BehaviorSignatureXMLElement.V_TRUE);
			mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TYPE, BehaviorSignatureXMLElement.V_ANY);
			for(String perm : perms.getIntentReceiverPermissions())
			{
				writeStartElement(BehaviorSignatureXMLElement.DATA.getTagName());
				String id = getIdForSysPermissionData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_ANY);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, id);
				writeEmptyElement(BehaviorSignatureXMLElement.PERMISSION.getTagName());
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_PERMISSIONS, perm);
				writeEndElement();
			}
			writeEndElement();
		}
		
		{
			writeComment("Sensitive-data-use point accepting data in a system receiver");
			writeStartElement(BehaviorSignatureXMLElement.COMPONENT.getTagName());
			mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_IS_SYSTEM, BehaviorSignatureXMLElement.V_TRUE);
			mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TYPE, BehaviorSignatureXMLElement.V_RECEIVER);
			for(String perm : perms.getIntentSenderPermissions())
			{
				writeStartElement(BehaviorSignatureXMLElement.DATA.getTagName());
				String id = getIdForSysPermissionData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_RECEIVER);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, id);
				writeEmptyElement(BehaviorSignatureXMLElement.PERMISSION.getTagName());
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_PERMISSIONS, perm);
				writeEndElement();
			}
			writeEndElement();
		}
		
		{
			writeComment("Sensitive-data-use point flowing in or out sensitive data in a system provider");
			writeStartElement(BehaviorSignatureXMLElement.COMPONENT.getTagName());
			mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_IS_SYSTEM, BehaviorSignatureXMLElement.V_TRUE);
			mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TYPE, BehaviorSignatureXMLElement.V_PROVIDER);
			SortedSet<String> providerPerms = new TreeSet<String>();
			providerPerms.addAll(perms.getProviderReadPermissions());
			providerPerms.addAll(perms.getProviderWritePermissions());
			for(String perm : providerPerms)
			{
				writeStartElement(BehaviorSignatureXMLElement.DATA.getTagName());
				String id = getIdForSysPermissionData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_PROVIDER);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, id);
				writeEmptyElement(BehaviorSignatureXMLElement.PERMISSION.getTagName());
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_PERMISSIONS, perm);
				writeEndElement();
			}
			writeEndElement();
		}
		
		writeEndElement();
	}
	protected void writeFlowSpec(Permissions perms)
		throws XMLStreamException
	{
		mFlowIds.clear();
		writeStartElement(BehaviorSignatureXMLElement.FLOW_SPEC.getTagName());
		{
			writeFlows(perms, getIdForAppData(DATA_FILE, BehaviorSignatureXMLElement.V_ANY), true);
			writeFlows(perms, getIdForAppData(DATA_FILE, BehaviorSignatureXMLElement.V_ANY), false);
		}
		{
			writeFlows(perms, getIdForAppData(DATA_DATABASE, BehaviorSignatureXMLElement.V_ANY), true);
			writeFlows(perms, getIdForAppData(DATA_DATABASE, BehaviorSignatureXMLElement.V_ANY), false);
		}
		{
			writeFlows(perms, getIdForAppData(DATA_SHARED_PREFERENCES, BehaviorSignatureXMLElement.V_ANY), true);
			writeFlows(perms, getIdForAppData(DATA_SHARED_PREFERENCES, BehaviorSignatureXMLElement.V_ANY), false);
		}
		{
			writeFlows(perms, getIdForAppData(getNameForPermission(PERM_INTERNET), BehaviorSignatureXMLElement.V_ANY), true);
		}
		{
			writeFlows(perms, getIdForAppData(getNameForPermission(PERM_SEND_SMS), BehaviorSignatureXMLElement.V_ANY), true);
			writeFlows(perms, getIdForAppData(getNameForPermission(PERM_WRITE_SMS), BehaviorSignatureXMLElement.V_ANY), true);
			writeFlows(perms, getIdForSysPermissionData(getNameForPermission(PERM_WRITE_SMS), BehaviorSignatureXMLElement.V_PROVIDER), true);
		}
		
		if(mTrackSystemCompAccess)
		{
			// Receiving system broadcast
			for(String perm : perms.getIntentReceiverPermissions())
			{
				writeEmptyElement(BehaviorSignatureXMLElement.FLOW.getTagName());
				String from = getIdForSysPermissionData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_ANY);
				String to = getIdForAppData(DATA_ICC_PARAM_CALLEE, BehaviorSignatureXMLElement.V_RECEIVER);
				String flowId = getIdForPermissionFlow(from, to);
				mFlowIds.add(flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_FROM, from);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TO, to);			
			}
			
			// Sending broadcast to system receiver
			for(String perm : perms.getIntentSenderPermissions())
			{
				writeEmptyElement(BehaviorSignatureXMLElement.FLOW.getTagName());
				String from = getIdForAppData(DATA_ICC_PARAM_CALLER, BehaviorSignatureXMLElement.V_ANY);
				String to = getIdForSysPermissionData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_RECEIVER);
				String flowId = getIdForPermissionFlow(from, to);
				mFlowIds.add(flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_FROM, from);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TO, to);
			}
			
			// Reading system provider
			for(String perm : perms.getProviderReadPermissions())
			{
				writeEmptyElement(BehaviorSignatureXMLElement.FLOW.getTagName());
				String from = getIdForSysPermissionData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_PROVIDER);
				String to = getIdForAppData(DATA_ICC_RET_CALLER, BehaviorSignatureXMLElement.V_ANY);
				String flowId = getIdForPermissionFlow(from, to);
				mFlowIds.add(flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_FROM, from);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TO, to);
			}
			
			// Writing system provider
			for(String perm : perms.getProviderWritePermissions())
			{
				writeEmptyElement(BehaviorSignatureXMLElement.FLOW.getTagName());
				String from = getIdForAppData(DATA_ICC_PARAM_CALLER, BehaviorSignatureXMLElement.V_ANY);
				String to = getIdForSysPermissionData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_PROVIDER);
				String flowId = getIdForPermissionFlow(from, to);
				mFlowIds.add(flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_FROM, from);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TO, to);
			}
		}
		writeEndElement();
	}
	protected void writeFlows(Permissions perms, String to, boolean forward)
		throws XMLStreamException
	{
		for(String perm : perms.getAPIPermissions())
		{
			if(!mInterestedPermPred.apply(perm))
				continue;
			if(!((forward && mReadPermPred.apply(perm)) || (!forward && mWritePermPred.apply(perm))))
				continue;
			String from = getIdForAppData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_ANY);
			writeEmptyElement(BehaviorSignatureXMLElement.FLOW.getTagName());
			if(forward)
			{
				String flowId = getIdForPermissionFlow(from, to);
				mFlowIds.add(flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_FROM, from);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TO, to);
			}
			else
			{
				String flowId = getIdForPermissionFlow(to, from);
				mFlowIds.add(flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_FROM, to);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TO, from);	
			}
		}
		
		if(forward)
		{
			for(String perm : perms.getIntentReceiverPermissions())
			{
				if(!mInterestedPermPred.apply(perm))
					continue;
				if(!mReadPermPred.apply(perm))
					continue;
				String from = getIdForSysPermissionData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_ANY);
				writeEmptyElement(BehaviorSignatureXMLElement.FLOW.getTagName());
				String flowId = getIdForPermissionFlow(from, to);
				mFlowIds.add(flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_FROM, from);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TO, to);
			}
		}
		else
		{
			for(String perm : perms.getIntentSenderPermissions())
			{
				if(!mInterestedPermPred.apply(perm))
					continue;
				if(!mWritePermPred.apply(perm))
					continue;
				String from = getIdForSysPermissionData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_RECEIVER);
				writeEmptyElement(BehaviorSignatureXMLElement.FLOW.getTagName());
				String flowId = getIdForPermissionFlow(to, from);
				mFlowIds.add(flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_FROM, to);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TO, from);
			}
		}
		
		if(forward)
		{
			for(String perm : perms.getProviderReadPermissions())
			{
				if(!mInterestedPermPred.apply(perm))
					continue;
				String from = getIdForSysPermissionData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_PROVIDER);
				writeEmptyElement(BehaviorSignatureXMLElement.FLOW.getTagName());
				String flowId = getIdForPermissionFlow(from, to);
				mFlowIds.add(flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_FROM, from);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TO, to);
			}
		}
		else
		{
			for(String perm : perms.getProviderWritePermissions())
			{
				if(!mInterestedPermPred.apply(perm))
					continue;
				String from = getIdForSysPermissionData(getNameForPermission(perm), BehaviorSignatureXMLElement.V_PROVIDER);
				writeEmptyElement(BehaviorSignatureXMLElement.FLOW.getTagName());
				String flowId = getIdForPermissionFlow(to, from);
				mFlowIds.add(flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, flowId);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_FROM, to);
				mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_TO, from);
			}
		}
	}
	protected void writeComment(String comment)
			throws XMLStreamException
	{
		mXmlWriter.writeCharacters("\n");
		
		String[] toks = comment.split("\\r?\\n");
		StringBuilder builder = new StringBuilder();
		builder.append('\n');
		for(String tok : toks)
		{
			for(int i = 0; i < mIndent; ++i)
			{
				builder.append('\t');
			}
			builder.append(tok);
			builder.append('\n');
		}
		mXmlWriter.writeComment(builder.toString());
	}
	protected void writeSignatures()
			throws XMLStreamException
	{
		writeStartElement(BehaviorSignatureXMLElement.SIGNATURES.getTagName());
		writeComment("Signatures permission-protected-data to file");
		for(String flowId : mFlowIds)
		{
			if(!flowId.startsWith("FLOW_"))
				throw new RuntimeException();
			writeEmptyElement(BehaviorSignatureXMLElement.SIGNATURE.getTagName());
			mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_ID, "SIG_" + flowId.replaceFirst("FLOW_", ""));
			mXmlWriter.writeAttribute(BehaviorSignatureXMLElement.A_DEFINITION, flowId);
		}
		writeEndElement();
	}

	protected void outputSignatures(
			Permissions perms, 
			File outputFile)
		throws IOException
	{
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		OutputStream output = null;
		mIndent = 0;
		try
		{
			output = new FileOutputStream(outputFile);
			mXmlWriter = xmlOutputFactory.createXMLStreamWriter(output, "UTF-8");
			mXmlWriter.writeStartDocument();
			writeComment("Auto-generated file by " + getClass().getName());
			writeStartElement(BehaviorSignatureXMLElement.SPEC.getTagName());
			writeDataSpec(perms);
			writeFlowSpec(perms);
			writeSignatures();
			writeEndElement();
			mXmlWriter.writeEndDocument();
			mXmlWriter.flush();
		}
		catch(XMLStreamException ex)
		{
			throw new IOException(ex);
		}
		finally
		{
			if(output != null)
			{
				try
				{
					output.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	public static void main(String[] args)
		throws Exception
	{
		String intentPermFile, apiPermFile, providerPermFile, outputFile;
		boolean trackSysCompAccess, filterInterestedPerms;
		{
			CommandLineParser cmdLineParser = new PosixParser();
			Options opts = buildOptions();
			CommandLine cmdLine;
			try
			{
				cmdLine = cmdLineParser.parse(opts, args, true);
			}
			catch(ParseException ex)
			{
				printHelp(opts);
				System.exit(1);
				return;
			}
			if(!cmdLine.hasOption(CMD_INTENT_PERM) ||
					!cmdLine.hasOption(CMD_API_PERM) ||
					!cmdLine.hasOption(CMD_PROVIDER_PERM) ||
					!cmdLine.hasOption(CMD_OUTPUT))
			{
				printHelp(opts);
				System.exit(1);
				return;
			}
			intentPermFile = cmdLine.getOptionValue(CMD_INTENT_PERM);
			apiPermFile = cmdLine.getOptionValue(CMD_API_PERM);
			providerPermFile = cmdLine.getOptionValue(CMD_PROVIDER_PERM);
			outputFile = cmdLine.getOptionValue(CMD_OUTPUT);
			trackSysCompAccess = cmdLine.hasOption(CMD_TRACK_SYS_COMP_ACCESS);
			filterInterestedPerms = cmdLine.hasOption(CMD_FILTER_INTERESTED_PERMS);
		}
		new BehaviorSignaturesGenerator(new File(apiPermFile), new File(intentPermFile), new File(providerPermFile), new File(outputFile), trackSysCompAccess, filterInterestedPerms);
	}
}

