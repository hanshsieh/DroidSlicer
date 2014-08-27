package org.droidslicer.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;

public class EntryPointFindingHelper
{
	protected static final String USAGE = "<jar_files>[...] <output_file>";
	protected XMLStreamWriter mXmlWriter = null;
	protected int mIndent = 0;
	protected static boolean isEntryMethod(IMethod method)
	{
		if(!((method.isPublic() || method.isProtected()) && !method.isStatic()))
			return false;
		//if(method.getName().equals(MethodReference.initAtom))
		//	return true;
		IClass clazz = method.getDeclaringClass();
		TypeReference typeRef = clazz.getReference();
		String methodName = method.getName().toString();
		if(methodName.startsWith("on"))
		{
			if(typeRef.equals(TypeId.ANDROID_ACTIVITY.getTypeReference()))
			{
				if(methodName.equals("onActivityResult"))
					return false;
			}
			return true;
		}
		if(typeRef.equals(TypeId.ANDROID_PROVIDER.getTypeReference()))
		{
			if(methodName.equals("query") ||
				methodName.equals("insert") ||
				methodName.equals("bulkInsert") ||
				methodName.equals("update") ||
				methodName.equals("delete") ||
				methodName.equals("getType"))
				return true;
		}
		return false;
	}
	protected void writeEntryMethods(String compType, IClass clazz)
		throws XMLStreamException
	{
		writeStartElement(compType);
		List<IMethod> methods = new ArrayList<IMethod>(clazz.getDeclaredMethods());
		
		// Sort the methods to ensure stable output among multiple runs
		Collections.sort(methods, new Comparator<IMethod>()
		{
			@Override
			public int compare(IMethod m1, IMethod m2)
			{
				String name1 = m1.toString();
				String name2 = m2.toString();
				return name1.compareTo(name2);
			}
			
		});
		for(IMethod method : methods)
		{
			if(isEntryMethod(method))
			{
				writeEmptyElement(EntryPointXMLElement.METHOD.getTagName());
				String methodSig = Utils.deploymentMethodString(method.getReference());
				mXmlWriter.writeAttribute(EntryPointXMLElement.A_SIGNATURE, methodSig);
				mXmlWriter.writeAttribute(EntryPointXMLElement.A_STATIC, method.isStatic() ? EntryPointXMLElement.V_TRUE : EntryPointXMLElement.V_FALSE);
				int nParam = method.getNumberOfParameters();
				for(int i = 1; i < nParam; ++i)
				{
					TypeReference paramType = method.getParameterType(i);
					boolean shouldTrack = false;
					if(clazz.getReference().equals(TypeId.ANDROID_PROVIDER.getTypeReference()))
					{
						String methodName = method.getName().toString();
						if(methodName.equals("insert") ||
							methodName.equals("query") ||
							methodName.equals("update") ||
							methodName.equals("delete") ||
							methodName.equals("getType"))
						{
							shouldTrack = true;
						}
					}
					if(Utils.equalIgnoreLoader(paramType, TypeId.ANDROID_INTENT.getTypeReference()))
					{
						shouldTrack = true;
					}
					if(shouldTrack)
						mXmlWriter.writeAttribute(EntryPointXMLElement.A_PARAM + i, EntryPointXMLElement.V_TRACK);
				}
			}
		}
		writeEndElement();
	}
	public void writeStartElement(String localName)
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
	public void writeEmptyElement(String localName)
			throws XMLStreamException
	{
		mXmlWriter.writeCharacters("\n");
		for(int i = 0; i < mIndent; ++i)
			mXmlWriter.writeCharacters("\t");
		mXmlWriter.writeEmptyElement(localName);
	}
	public void writeEndElement()
		throws XMLStreamException
	{
		mXmlWriter.writeCharacters("\n");
		--mIndent;
		for(int i = 0; i < mIndent; ++i)
			mXmlWriter.writeCharacters("\t");
		mXmlWriter.writeEndElement();
	}
	public EntryPointFindingHelper(String[] inputFileNames, String outputFileName)
		throws Exception
	{
		OutputStream output = null;
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		try
		{
			output = new FileOutputStream(outputFileName);
			mXmlWriter = xmlOutputFactory.createXMLStreamWriter(output, "UTF-8");
			// Load the jar files into analysis scope
			AnalysisScope analysisScope = AnalysisScope.createJavaAnalysisScope();
			for(String inputFileName : inputFileNames)
			{
				JarFile classesJar = new JarFile(new File(inputFileName));
				Module appModule = new JarFileModule(classesJar);
				analysisScope.addToScope(analysisScope.getPrimordialLoader(), appModule);
			}
			
			// Build class hierarchy
			ClassHierarchy cha = ClassHierarchy.make(analysisScope);
			
			IClass activityClass = cha.lookupClass(TypeId.ANDROID_ACTIVITY.getTypeReference());
			IClass receiverClass = cha.lookupClass(TypeId.ANDROID_RECEIVER.getTypeReference());
			IClass providerClass = cha.lookupClass(TypeId.ANDROID_PROVIDER.getTypeReference());
			IClass serviceClass = cha.lookupClass(TypeId.ANDROID_SERVICE.getTypeReference());
			IClass applicationClass = cha.lookupClass(TypeId.ANDROID_APPLICATION.getTypeReference()); 
			if(activityClass == null ||
				receiverClass == null ||
				providerClass == null ||
				serviceClass == null ||
				applicationClass == null)
			{
				throw new IOException("Fail to find some of the Android components in the class hierarachy");
			}
			mXmlWriter.writeStartDocument();
			writeStartElement(EntryPointXMLElement.ENTRY_SPEC.getTagName());
			
			writeEntryMethods(EntryPointXMLElement.ACTIVITY.getTagName(), activityClass);
			writeEntryMethods(EntryPointXMLElement.RECEIVER.getTagName(), receiverClass);
			writeEntryMethods(EntryPointXMLElement.PROVIDER.getTagName(), providerClass);
			writeEntryMethods(EntryPointXMLElement.SERVICE.getTagName(), serviceClass);
			writeEntryMethods(EntryPointXMLElement.APPLICATION.getTagName(), applicationClass);

			writeEndElement(); // </entry-spec>
			mXmlWriter.writeEndDocument();
			mXmlWriter.flush();
			System.out.println("Result has been written to file \"" + outputFileName + "\"");
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
	public static void main(String[] args)
	{
		if(args.length < 1)
		{
			System.err.println(USAGE);
			System.exit(1);
			return;
		}
		try
		{
			new EntryPointFindingHelper(Arrays.copyOfRange(args, 0, args.length - 1), args[args.length - 1]);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
			return;
		}
	}
}
