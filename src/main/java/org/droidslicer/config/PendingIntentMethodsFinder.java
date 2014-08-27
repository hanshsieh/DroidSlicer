package org.droidslicer.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.jar.JarFile;

import org.droidslicer.util.TypeId;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.StringStuff;

public class PendingIntentMethodsFinder
{
	protected static final String USAGE = "<jar_files>[...] <output_file>";
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
			while(classItr.hasNext())
			{
				IClass clazz = classItr.next();
				if(!clazz.isReferenceType() || 
					clazz.isArrayClass() ||
					!clazz.isPublic())
				{
					continue;
				}
				for(IMethod method : clazz.getAllMethods())
				{
					if(method.isStatic() || method.getNumberOfParameters() < 1 || !method.isPublic())
						continue;
					String methodName = method.getName().toString();
					if(methodName.startsWith("remove") || methodName.startsWith("unregister") || methodName.startsWith("cancel"))
						continue;						
					boolean hasPendingIntent = false;
					int nParam = method.getNumberOfParameters();
					for(int paramIdx = 1; paramIdx < nParam; ++paramIdx)
					{
						TypeReference paramType = method.getParameterType(paramIdx);
						if(paramType.getName().equals(TypeId.ANDROID_PENDING_INTENT.getTypeReference().getName()))
						{
							hasPendingIntent = true;
							break;
						}
					}
					if(!hasPendingIntent)
						continue;
					writer.write(StringStuff.jvmToBinaryName(clazz.getName().toString()));
					writer.write(' ');
					writer.write(StringStuff.jvmToBinaryName(method.getReturnType().getName().toString()));
					writer.write(' ');
					writer.write(method.getName().toString());
					writer.write('(');
					for(int i = 0; i < nParam; ++i)
					{
						if(i > 0)
							writer.write(", ");
						writer.write(StringStuff.jvmToBinaryName(method.getParameterType(i).getName().toString()));
					}
					writer.write(')');
					writer.write('\n');
				}
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
