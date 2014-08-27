package org.droidslicer.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.strings.StringStuff;

public class ListenerFindingHelper
{
	protected static final String USAGE = "<jar_files>[...] <output_file>";
	private static final String[] IGNORE_PACKAGES = new String[]
		{
			"javax/swing",
			"java/awt",
			"sun",
			"com/sun",
			"sunw",
			"com/android/internal",
			"com/android"
		};
	protected static boolean isIgnorePackage(String pkgName) 
	{
		for(String prefix : IGNORE_PACKAGES)
		{
			if(pkgName.equals(prefix) || (pkgName.startsWith(prefix) && pkgName.charAt(prefix.length()) == '/'))
				return true;
		}
		return false;
	}
	public static void main(String[] args)
	{
		if(args.length < 1)
		{
			System.err.println(USAGE);
			System.exit(1);
			return;
		}
		Writer writer = null;
		try
		{
			AnalysisScope analysisScope = AnalysisScope.createJavaAnalysisScope();
			String outputFileName = args[args.length - 1];
			for(int i = 0; i < args.length - 1; ++i)
			{
				String jarFileName = args[i];
				JarFile classesJar = new JarFile(new File(jarFileName));
				Module appModule = new JarFileModule(classesJar);
				analysisScope.addToScope(analysisScope.getPrimordialLoader(), appModule);
			}
			ClassHierarchy cha = ClassHierarchy.make(analysisScope);
			writer = new BufferedWriter(new FileWriter(outputFileName));
			Iterator<IClass> classItr = cha.iterator();
			List<String> listeners = new ArrayList<String>();
			while(classItr.hasNext())
			{
				IClass clazz = classItr.next();
				if(!clazz.isReferenceType() || 
					clazz.isArrayClass() ||
					!clazz.isPublic() ||
					!clazz.isInterface())
				{
					continue;
				}
				TypeName typeName = clazz.getName();
				String pkgName = typeName.getPackage().toString();
				if(isIgnorePackage(pkgName))
					continue;
				String className = typeName.getClassName().toString();
				if(!className.endsWith("Listener") && 
					!className.endsWith("Callback") && 
					!(className.endsWith("Watcher") && typeName.getPackage().toString().startsWith("android")))
					continue;
				String readableName = StringStuff.jvmToBinaryName(typeName.toString());
				listeners.add(readableName);
			}
			
			// Write comment
			writer.write("# Auto-generated file by " + ListenerFindingHelper.class.getName() + "\n");
			writer.write("# The classes are selected using the following rules: \n");
			writer.write("# 1. The classes in the following packages (including sub-packages) are ignored: \n");
			for(String prefix : IGNORE_PACKAGES)
				writer.write("#    " + prefix.replace('/', '.') + "\n");
			writer.write("# 2. It must be public class.\n");
			writer.write("# 3. The class name must end with \"Listener\"\n");
			
			// Sort the listeners to ensure having stable output among multiple runs 
			Collections.sort(listeners);
			for(String listener : listeners)
			{
				writer.write(listener);
				writer.write('\n');
			}
			writer.flush();
			System.out.println("Result has been written to file \"" + outputFileName + "\"");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
			return;
		}
		finally
		{
			if(writer != null)
			{
				try
				{
					writer.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
}
