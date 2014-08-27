package org.droidslicer.pscout;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.droidslicer.config.APIPermissionXMLElement;
import org.droidslicer.config.ListenerConfigParser;
import org.droidslicer.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;

public class PScoutAPIPermToXML
{
	private final static Logger mLogger = LoggerFactory.getLogger(PScoutAPIPermToXML.class);
	private static final String[] IGNORE_PACKAGES = new String[]
			{
				"javax/xml/bind",
				"sun",
				"com/sun",
				"sunw",
				"com/android/internal",
				"com/android"
			};
	private static final String[] ALLOWED_JAVAX_PACKAGES = new String[]
			{
				"javax/crypto",
				"javax/net",
				"javax/xml",
				"javax/sql"
			};
	private static String USAGE = "-p <pscout_api_perm_file> -o <output_file> -l <listener_file> -c [-b <lib_file1>,<lib_file2>...] [-r] [-t] [-i]";
	private static String CMD_PSCOUT = "p";
	private static String CMD_OUTPUT = "o";
	private static String CMD_LISTENER = "l";
	private static String CMD_LIB = "b";
	private static String CMD_RESOLVE_ARG = "r";
	private static String CMD_TRACK_LISTENER = "t";
	private static String CMD_POPULATE_PERMS = "c";
	private static String CMD_NO_TRACK_PRIMITIVE_RET = "i";
	private File mListenerFile;
	private File mPscoutFile;
	private File mOutputFile;
	private File[]  mLibs;
	private boolean mResolveArgs;
	private boolean mTrackListeners;
	private boolean mPopPerms;
	private boolean mIgnorePrimitiveRet;
	private ClassHierarchy mCha;
	private Set<TypeReference> mListeners;
	protected static class APIPerm implements Comparable<APIPerm>
	{
		private MethodReference mMethodRef;
		private final Set<String> mPermissions = new HashSet<String>();
		public APIPerm(MethodReference methodRef)
		{
			mMethodRef = methodRef;
		}
		public MethodReference getMethod()
		{
			return mMethodRef;
		}
 		public void addPermission(String perm)
		{
			mPermissions.add(perm);
		}
		public Set<String> getPermissions()
		{
			return mPermissions;
		}
		@Override
		public int hashCode()
		{
			return mMethodRef.hashCode() * 91;
		}
		@Override
		public boolean equals(Object other)
		{
			if(this == other)
				return true;
			if(!(other instanceof APIPerm))
				return false;
			APIPerm that = (APIPerm)other;
			return mMethodRef.equals(that.mMethodRef);
		}
		@Override
		public int compareTo(APIPerm that)
		{
			String methodStr = mMethodRef.toString();
			String oMethodStr = that.mMethodRef.toString();
			return methodStr.compareTo(oMethodStr);
		}
	}
	protected static class PermissionOfInterestPred implements Predicate<String>
	{
		@Override
		public boolean apply(String perm)
		{
			return true;
//			String name;
//			{
//				int idx = perm.lastIndexOf('.');
//				if(idx < 0)
//					name = perm;
//				else
//					name = perm.substring(idx + 1);
//			}
//			
//			if(name.equals("WAKE_LOCK") ||
//				name.equals("STATUS_BAR") ||
//				name.equals("VIBRATE") || 
//				name.equals("SYSTEM_ALERT_WINDOW") ||
//				name.equals("DIAGNOSTIC") ||
//				name.equals("BIND_WALLPAPER"))
//				return false;
		}
	}
	protected XMLStreamWriter mXmlWriter = null;
	protected int mIndent = 0;
	private Predicate<String> mPermOfInterestPred = new PermissionOfInterestPred();
	protected static boolean isIgnorePackage(String pkgName) 
	{
		for(String prefix : IGNORE_PACKAGES)
		{
			if(pkgName.equals(prefix) || (pkgName.startsWith(prefix) && pkgName.charAt(prefix.length()) == '/'))
				return true;
		}
		if(pkgName.startsWith("javax"))
		{
			boolean ignored = true;
			for(String prefix : ALLOWED_JAVAX_PACKAGES)
			{
				if(pkgName.equals(prefix) || (pkgName.startsWith(prefix) && pkgName.charAt(prefix.length()) == '/'))
				{
					ignored = false;
					break;
				}	
			}
			if(ignored)
				return true;
		}
		return false;
	}
	protected Set<TypeReference> collectListeners()
		throws IOException
	{
		final Set<TypeReference> listeners = new HashSet<TypeReference>(); 
		FileInputStream input = null;
		ListenerConfigParser parser = null;
		try
		{
			input = new FileInputStream(mListenerFile);
			parser = new ListenerConfigParser(input);
			String type;
			while((type = parser.read()) != null)
			{
				TypeReference typeRef = TypeReference.findOrCreate(ClassLoaderReference.Primordial, StringStuff.deployment2CanonicalTypeString(type));
				listeners.add(typeRef);
			}
		}
		finally
		{
			if(input != null)
			{
				try
				{
					input.close();
				}
				catch(Exception ex)
				{}
			}
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
		System.out.println("" + listeners.size() + " listeners are read from file \"" + mListenerFile.getAbsolutePath() + "\"");
		return listeners;
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
	private static Options buildOptions()
	{
		Options options = new Options();
		options.addOption(CMD_PSCOUT, true, "PScout file")
			.addOption(CMD_OUTPUT, true, "Output file")
			.addOption(CMD_LISTENER, true, "Listener file")
			.addOption(CMD_LIB, true, "Library files (Seperated by ',')")
			.addOption(CMD_RESOLVE_ARG, false, "Whether the arguments of the API should be resolved")
			.addOption(CMD_TRACK_LISTENER, false, "Whether it should track the parameters when the listener arguments are invoked")
			.addOption(CMD_POPULATE_PERMS, false, 
					"Whether it should populate the permissions of constructors and " + 
					"static method that return the class type to all the declared methods of the class")
			.addOption(CMD_NO_TRACK_PRIMITIVE_RET, false, 
					"Whether not to track primitive return type");
		return options;
	}
	protected static void printHelp(Options options)
	{
		HelpFormatter helpFormatter = new HelpFormatter();  
		helpFormatter.printHelp(USAGE, options);
	}
	public static void main(String[] args)
	{
		try
		{
			new PScoutAPIPermToXML(args);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
			return;
		}		
	}
	private static String getPackageName(Atom name)
	{
		return name.toString().replace('/', '.');
	}
	private static List<APIPerm> findOrCreateAPIPerms(Map<Atom, Map<Atom, List<APIPerm>>> mapping, Atom pkgName, Atom className)
	{
		Map<Atom, List<APIPerm>> classes = mapping.get(pkgName);
		if(classes == null)
		{
			classes = new HashMap<Atom, List<APIPerm>>();
			mapping.put(pkgName, classes);
		}
		List<APIPerm> list = classes.get(className);
		if(list == null)
		{
			list = new ArrayList<APIPerm>();
			classes.put(className, list);
		}
		return list;
	}
	private static MethodReference createMethodReference(String pkgName, String className, String retType, String methodName, String[] argTypes)
	{
		String typeStr = StringStuff.deployment2CanonicalTypeString(pkgName + '.' + className);
		TypeReference typeRef = 
				TypeReference.findOrCreate(ClassLoaderReference.Primordial, typeStr);
		return MethodReference.findOrCreate(typeRef, Atom.findOrCreateUnicodeAtom(methodName), 
				Descriptor.findOrCreateUTF8(Utils.canonicalDescriptorString(retType, argTypes)));
	}
	private void collectListenerRegisterMethods(Map<MethodReference, APIPerm> apiPerms)
		throws IOException
	{
		for(IClass clazz : mCha)
		{
			if(!clazz.isPublic())
				continue;
			if(isIgnorePackage(clazz.getName().getPackage().toString()))
				continue;
			for(IMethod method : clazz.getDeclaredMethods())
			{
				if(!method.isPublic() || method.isStatic())
					continue;
				String methodName = method.getName().toString();
				if(methodName.startsWith("remove"))
					continue;
				MethodReference methodRef = method.getReference();
				if(apiPerms.containsKey(methodRef))
					continue;
				int nParam = methodRef.getNumberOfParameters();
				boolean hasListenerParam = false;
				for(int paramIdx = 0; paramIdx < nParam; ++paramIdx)
				{
					TypeReference paramTypeRef = methodRef.getParameterType(paramIdx);
					if(mListeners.contains(paramTypeRef))
					{
						hasListenerParam = true;
						break;
					}
				}
				if(!hasListenerParam)
					continue;
				APIPerm apiPerm = new APIPerm(methodRef);
				mLogger.debug("Listener registration method: {}", methodRef);
				apiPerms.put(methodRef, apiPerm);
			}
		}
	}
	private Map<Atom, Map<Atom, List<APIPerm>>> organizeAPIPerms(Collection<APIPerm> apiPerms)
	{
		Map<Atom, Map<Atom, List<APIPerm>>> result = new HashMap<Atom, Map<Atom, List<APIPerm>>>();
		for(APIPerm apiPerm : apiPerms)
		{
			MethodReference methodRef = apiPerm.getMethod();
			TypeReference typeRef = methodRef.getDeclaringClass();
			List<APIPerm> apiPermList = findOrCreateAPIPerms(result, typeRef.getName().getPackage(), typeRef.getName().getClassName());
			apiPermList.add(apiPerm);
		}
		for(Map<Atom, List<APIPerm>> pkgs : result.values())
		{
			for(List<APIPerm> list : pkgs.values())
			{
				Collections.sort(list);
			}
		}
		return result;
	}
	private Map<MethodReference, APIPerm> collectPScoutAPIPermission()
		throws IOException
	{
		Map<MethodReference, APIPerm> apiPerms = new HashMap<MethodReference, APIPerm>();
		Reader reader = null;
		PScoutAPIPermParser parser = null;
		try
		{
			reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(mPscoutFile)));
			parser = new PScoutAPIPermParser(reader);
			PScoutAPIPerm pscoutApiPerm;
			while((pscoutApiPerm = parser.read()) != null)
			{
				String perm = pscoutApiPerm.getPermission();
				if(!mPermOfInterestPred.apply(perm))
					continue;
				String pkgName = pscoutApiPerm.getPackageName();
				String className = pscoutApiPerm.getClassName();
				String methodName = pscoutApiPerm.getMethodName();
				String retType = pscoutApiPerm.getReturnType();
				String[] argTypes = pscoutApiPerm.getArgumentTypes();
				MethodReference methodRef = createMethodReference(pkgName, className, retType, methodName, argTypes);
				IMethod method = mCha.resolveMethod(methodRef);
				if(method == null)
				{
					mLogger.warn("Method {} isn't found in class hierarchy. Ignore it", methodRef.getSignature());
					continue;
				}
				if(method.isPrivate())
					continue;
				if(methodName.contains("$"))
					continue;
				IClass clazz = method.getDeclaringClass();
				if(!clazz.isPublic())
					continue;
				APIPerm apiPerm = apiPerms.get(methodRef);
				if(apiPerm == null)
				{
					apiPerm = new APIPerm(methodRef);
					apiPerms.put(methodRef, apiPerm);
				}
				apiPerm.addPermission(perm);
			}
			return apiPerms;
		}
		catch(PScoutFormatException ex)
		{
			throw new IOException(ex);
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
			if(reader != null)
			{
				try
				{
					reader.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	private boolean parseArguments(Options opts, String[] args)
	{
		CommandLineParser cmdLineParser = new PosixParser();
		CommandLine cmdLine;
		try
		{
			cmdLine = cmdLineParser.parse(opts, args, true);
		}
		catch(ParseException ex)
		{
			return false;
		}
		
		if(!cmdLine.hasOption(CMD_LISTENER) ||
			!cmdLine.hasOption(CMD_PSCOUT) || 
			!cmdLine.hasOption(CMD_OUTPUT))
		{
			return false;
		}
		mPscoutFile = new File(cmdLine.getOptionValue(CMD_PSCOUT));
		mListenerFile = new File(cmdLine.getOptionValue(CMD_LISTENER));
		mOutputFile = new File(cmdLine.getOptionValue(CMD_OUTPUT));
		String libsStr = cmdLine.getOptionValue(CMD_LIB);
		{
			String[] libsStrArr = libsStr == null ? new String[0] : libsStr.split(",");
			mLibs = new File[libsStrArr.length];
			for(int i = 0; i < libsStrArr.length; ++i)
			{
				mLibs[i] = new File(libsStrArr[i]);
			}
		}
		mResolveArgs = cmdLine.hasOption(CMD_RESOLVE_ARG);
		mTrackListeners = cmdLine.hasOption(CMD_TRACK_LISTENER);
		mPopPerms = cmdLine.hasOption(CMD_POPULATE_PERMS);
		mIgnorePrimitiveRet = cmdLine.hasOption(CMD_NO_TRACK_PRIMITIVE_RET);
		return true;
	}
	private void populatePermissions(Map<MethodReference, APIPerm> apiPerms)
	{
		Map<MethodReference, Set<String>> newPerms = new HashMap<MethodReference, Set<String>>();
		for(APIPerm apiPerm : apiPerms.values())
		{
			MethodReference methodRef = apiPerm.getMethod();
			if(apiPerm.getPermissions().isEmpty())
				continue;
			IMethod method = mCha.resolveMethod(methodRef);
			if(method == null)
				continue;
			if(!methodRef.getName().equals(MethodReference.initAtom) && 
				!(method.isStatic() && method.getReturnType().getName().equals(method.getDeclaringClass().getName())))
				continue;
			IClass type = method.getDeclaringClass();
			for(IMethod oMethod : type.getDeclaredMethods())
			{
				if(oMethod.isStatic() || !oMethod.isPublic())
					continue;
				Atom oMethodName = oMethod.getName();
				if(oMethodName.equals(MethodReference.initAtom) || oMethodName.equals(MethodReference.clinitName))
					continue;
				MethodReference oMethodRef = oMethod.getReference();
				Set<String> perms = newPerms.get(oMethodRef);
				if(perms == null)
				{
					perms = new LinkedHashSet<String>();
					newPerms.put(oMethodRef, perms);
				}
				perms.addAll(apiPerm.getPermissions());
			}
		}
		for(Map.Entry<MethodReference, Set<String>> entry : newPerms.entrySet())
		{
			MethodReference methodRef = entry.getKey();
			Set<String> perms = entry.getValue();
			APIPerm apiPerm = apiPerms.get(methodRef);
			if(apiPerm == null)
			{
				apiPerm = new APIPerm(methodRef);
				apiPerms.put(methodRef, apiPerm);
			}
			for(String perm : perms)
			{
				apiPerm.addPermission(perm);
			}
		}
	}
	public PScoutAPIPermToXML(String[] args)
		throws Exception
	{
		Options opts = buildOptions();
		if(!parseArguments(opts, args))
		{
			printHelp(opts);
			System.exit(1);
			return;
		}		
		// Build class hierarchy
		AnalysisScope analysisScope = AnalysisScope.createJavaAnalysisScope();
		for(File libFile : mLibs)
		{
			JarFile classesJar = new JarFile(libFile);
			Module appModule = new JarFileModule(classesJar);
			analysisScope.addToScope(analysisScope.getPrimordialLoader(), appModule);
		}
		mCha = ClassHierarchy.make(analysisScope);
		mListeners = collectListeners();
		OutputStream output = null;
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		try
		{
			output = new FileOutputStream(mOutputFile);
			mXmlWriter = xmlOutputFactory.createXMLStreamWriter(output, "UTF-8");
			mXmlWriter.writeStartDocument();
			String comment;
			{
				StringBuilder builder = new StringBuilder();
				builder.append("Auto generated file by ");
				builder.append(PScoutAPIPermToXML.class.getName());
				builder.append('\n'); 
				builder.append("Generated with arguments:");
				for(int i = 0; i < args.length; ++i)
				{
					builder.append(' ');
					builder.append(args[i]);
				}
				comment = builder.toString();
			}
			writeComment(comment);
			writeStartElement(APIPermissionXMLElement.API_SPEC.getTagName());
			
			writeStartElement(APIPermissionXMLElement.CLASS_LOADER.getTagName());
			mXmlWriter.writeAttribute(APIPermissionXMLElement.A_NAME, APIPermissionXMLElement.V_PRIMORDIAL);
			
			// Collect the API-permission in the PScout file
			Map<MethodReference, APIPerm> apiPerms = collectPScoutAPIPermission();
			
			// Collect the listener registration APIs
			collectListenerRegisterMethods(apiPerms);
			
			if(mPopPerms)
				populatePermissions(apiPerms);
			
			// Generate the XML
			for(Map.Entry<Atom, Map<Atom, List<APIPerm>>> pkgEntry : organizeAPIPerms(apiPerms.values()).entrySet())
			{
				Atom pkgName = pkgEntry.getKey();
				writeStartElement(APIPermissionXMLElement.PACKAGE.getTagName());
				mXmlWriter.writeAttribute(APIPermissionXMLElement.A_NAME, pkgName.toString().replace('/', '.'));
				for(Map.Entry<Atom, List<APIPerm>> classEntry : pkgEntry.getValue().entrySet())
				{
					Atom className = classEntry.getKey();
					writeStartElement(APIPermissionXMLElement.CLASS.getTagName());
					mXmlWriter.writeAttribute(APIPermissionXMLElement.A_NAME, className.toString());
					List<APIPerm> apiList = classEntry.getValue();
					
					// Sort the api-permissions, so that the output is stable among multiple runs.
					for(APIPerm apiPerm : apiList)
					{
						MethodReference methodRef = apiPerm.getMethod();
						IMethod method = mCha.resolveMethod(methodRef);
						if(method == null)
							continue;
						String signature = Utils.deploymentMethodString(methodRef);
						writeEmptyElement(APIPermissionXMLElement.METHOD.getTagName());
						mXmlWriter.writeAttribute(APIPermissionXMLElement.A_SIGNATURE, signature);
						mXmlWriter.writeAttribute(APIPermissionXMLElement.A_STATIC, method.isStatic() ? APIPermissionXMLElement.V_TRUE : APIPermissionXMLElement.V_FALSE);
						
						if(!apiPerm.getPermissions().isEmpty())
						{
							if(TypeReference.Void.equals(methodRef.getReturnType()) || 
									(mIgnorePrimitiveRet && methodRef.getReturnType().isPrimitiveType()))
								mXmlWriter.writeAttribute(APIPermissionXMLElement.A_RETURN, "");
							else
								mXmlWriter.writeAttribute(APIPermissionXMLElement.A_RETURN, APIPermissionXMLElement.V_TRACK);
						}
						{
							for(int paramIdx = (method.isStatic() ? 0 : 1); paramIdx < method.getNumberOfParameters(); ++paramIdx)
							{
								TypeReference paramTypeRef = method.getParameterType(paramIdx);
								StringBuilder valBuilder = new StringBuilder();
								if(!apiPerm.getPermissions().isEmpty())
								{
									if(valBuilder.length() > 0)
										valBuilder.append(", ");
									valBuilder.append(APIPermissionXMLElement.V_TRACK);
								}
								if(mResolveArgs)
								{
									if(valBuilder.length() > 0)
										valBuilder.append(", ");
									valBuilder.append(APIPermissionXMLElement.V_RESOLVE);
								}
								if(mListeners.contains(paramTypeRef) && !method.getName().toString().startsWith("remove"))
								{
									if(valBuilder.length() > 0)
										valBuilder.append(", ");
									valBuilder.append(APIPermissionXMLElement.V_LISTENER);
									if(mTrackListeners && !apiPerm.getPermissions().isEmpty())
									{
										valBuilder.append(", ");
										valBuilder.append(APIPermissionXMLElement.V_TRACK_LISTENER);
									}
								}
								mXmlWriter.writeAttribute(APIPermissionXMLElement.A_PARAM + paramIdx, valBuilder.toString());
							}
						}
						
						{
							StringBuilder valBuilder = new StringBuilder();
							boolean firstPerm = true;
							for(String perm : apiPerm.getPermissions())
							{
								if(firstPerm)
									firstPerm = false;
								else
									valBuilder.append(",");
								valBuilder.append(perm);
							}
							mXmlWriter.writeAttribute(APIPermissionXMLElement.A_PERMISSION, valBuilder.toString());
						}
					}
					writeEndElement();
				}
				writeEndElement();
			}
			writeEndElement();
			writeEndElement();
			mXmlWriter.writeEndDocument();
			mXmlWriter.flush();
			mLogger.info("# API: {}", apiPerms.size());
			mLogger.info("Result has been written to \"" + mOutputFile.getAbsolutePath() + "\"");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
			return;
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
			if(mXmlWriter != null)
			{
				try
				{
					mXmlWriter.close();
				}
				catch(Exception ex)
				{}
				mXmlWriter = null;
			}
		}
	}
}
