package org.droidslicer.android.model;

import java.util.List;

import org.droidslicer.util.InstructionException;
import org.droidslicer.util.InstructionsBuilder;
import org.droidslicer.util.MethodId;
import org.droidslicer.util.TypeId;
import org.droidslicer.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

/**
 * {@code <init>(android.app.Application, android.content.pm.ApplicaitonInfo)}
 * @author someone
 *
 */
public class FakeContextImpl extends FakeClass
{
	private Logger mLogger = LoggerFactory.getLogger(FakeContextImpl.class);
	private final static String APP_CONTEXT_FIELD_NAME = "mAppContext";
	private final static String APP_INFO_FIELD_NAME = "mAppInfo";
	private final static int MAX_ALLOCATION_DEPTH = 2;
	
	public FakeContextImpl(TypeReference type, TypeReference appModelTypeRef, IClassHierarchy cha)
	{
		super(type, cha);
		
		Language lang = cha.getScope().getLanguage(ClassLoaderReference.Primordial.getLanguage());
	    SSAInstructionFactory instFactory = lang.instructionFactory();
		
		setModifiers(ClassConstants.ACC_PUBLIC);		
		
		setupSuperClass();
		
		setupFields();

		setupConstructors(instFactory);
		
		setupMethods(instFactory, appModelTypeRef);
	}
	private void setupSuperClass()
	{
		// Setup super class to be android.content.Context
		TypeReference superClazzRef = TypeId.ANDROID_CONTEXT.getTypeReference();
		IClass superClazz = getClassHierarchy().lookupClass(superClazzRef);
		if(superClazz == null)
			throw new IllegalArgumentException("android.content.Context not found in class hierarchy");
		setSuperclass(superClazz);
	}
	private void setupFields()
	{
		TypeReference clazzType = getReference();
		IClassHierarchy cha = getClassHierarchy();
		
		// Add a field of type Application, and named "mAppContext"
		FieldReference appContextFieldRef = 
				FieldReference.findOrCreate(
						clazzType, 
						Atom.findOrCreateAsciiAtom(APP_CONTEXT_FIELD_NAME), 
						TypeId.ANDROID_APPLICATION.getTypeReference());
		IField appContextField = 
				new FakeField(this, appContextFieldRef, ClassConstants.ACC_PUBLIC);
		addField(appContextField);

		// Add a field of type ApplicationInfo, and named "mAppInfo"
		FieldReference appInfoFieldRef = 
				FieldReference.findOrCreate(
						clazzType, 
						Atom.findOrCreateAsciiAtom(APP_INFO_FIELD_NAME), 
						TypeId.ANDROID_APP_INFO.getTypeReference());
		IField appInfoField = 
				new FakeField(this, appInfoFieldRef, ClassConstants.ACC_PUBLIC);
		addField(appInfoField);
		
		if(cha.lookupClass(TypeId.ANDROID_APP_INFO.getTypeReference()) == null || cha.lookupClass(TypeId.ANDROID_APPLICATION.getTypeReference()) == null)
			throw new IllegalArgumentException("android.app.Application or android.content.pm.ApplicationInfo aren't found in the class hierarchy");
	}
	private void setupConstructors(SSAInstructionFactory instFactory)
	{
		// Constructor parameters: android.app.Application, android.content.pm.ApplicationInfo
		Selector selector = new Selector(
				MethodReference.initAtom, 
				Descriptor.findOrCreate(
					new TypeName[]{
						TypeId.ANDROID_APPLICATION.getTypeReference().getName(),
						TypeId.ANDROID_APP_INFO.getTypeReference().getName()}, 
					TypeReference.VoidName));
		MethodReference constructorRef = MethodReference.findOrCreate(getReference(), selector);
		MethodSummary constructorSummary = new MethodSummary(constructorRef);
		constructorSummary.setStatic(false);
		constructorSummary.setFactory(false);
		
		// 'this' is at value number 1
		// Put parameter 1 and parameter 2 into fields mAppContext and mAppInfo, respectively
		SSAPutInstruction putAppCtx = instFactory.PutInstruction(1, 1 + 1, getField(Atom.findOrCreateAsciiAtom(APP_CONTEXT_FIELD_NAME)).getReference());
		SSAPutInstruction putAppInfo = instFactory.PutInstruction(1, 1 + 2, getField(Atom.findOrCreateAsciiAtom(APP_INFO_FIELD_NAME)).getReference());
		constructorSummary.addStatement(putAppCtx);
		constructorSummary.addStatement(putAppInfo);
		SummarizedMethod constructorMethod = new SummarizedMethod(constructorRef, constructorSummary, this);
		addMethod(constructorMethod);
	}
	private void setupMethods(SSAInstructionFactory instFactory, TypeReference appModelTypeRef)
	{
		for(IMethod superMethod : getSuperclass().getDeclaredMethods())
		{
			if(!superMethod.isAbstract())
				continue;

			// Add an implementation of the method
			Selector selector = superMethod.getSelector();
			MethodReference methodRef = MethodReference.findOrCreate(getReference(), selector);
			MethodSummary methodSummary = new MethodSummary(methodRef);
			methodSummary.setStatic(false);			
			methodSummary.setFactory(false);
			InstructionsBuilder builder = 
					new InstructionsBuilder(
							getClassHierarchy(),
							instFactory, 
							methodSummary.getConstants(), 
							methodSummary.getStatements().length, 
							superMethod.getNumberOfParameters() + 1);
			
			switch(MethodId.getMethodId(superMethod.getReference()))
			{
			case ANDROID_CONTEXT_GET_APP_CTX:
				{
					// Get mAppContext from class field
					FieldReference appCtxFieldRef = getField(Atom.findOrCreateAsciiAtom(APP_CONTEXT_FIELD_NAME)).getReference();
					
					// 'this' is at value number 1
					int appValNum = builder.addLocal();
					SSAGetInstruction getInst = instFactory.GetInstruction(appValNum, 1, appCtxFieldRef);
					builder.addInstruction(getInst);
					
					// Return the instance
					SSAReturnInstruction retInst = instFactory.ReturnInstruction(appValNum, false);
					builder.addInstruction(retInst);
					break;
				}
			case ANDROID_CONTEXT_GET_APP_INFO:
				{
					// Get mAppInfo from class field
					FieldReference appCtxFieldRef = getField(Atom.findOrCreateAsciiAtom(APP_INFO_FIELD_NAME)).getReference();
					
					int ctxValNum = builder.addLocal();
					// 'this' is at value number 1
					SSAGetInstruction getInst = instFactory.GetInstruction(ctxValNum, 1, appCtxFieldRef);
					builder.addInstruction(getInst);
					
					// Return the instance
					SSAReturnInstruction retInst = instFactory.ReturnInstruction(ctxValNum, false);
					builder.addInstruction(retInst);
					break;
				}
			case ANDROID_CONTEXT_REGISTER_RECEIVER:
			case ANDROID_CONTEXT_REGISTER_RECEIVER_PERM_HANDLER:
				{
					// Add the receiver to the list in the app model class
					assert superMethod.getParameterType(1).getName().equals(TypeId.ANDROID_RECEIVER.getTypeReference().getName()) : "Expecting the 2nd parameter to be a BroadcastReceiver"; 
					// Find the List static field in the app model class for this parameter type
					Atom fieldName = AppModelClass.getTypeListFieldName(TypeId.ANDROID_RECEIVER.getTypeReference());
					TypeReference fieldTypeRef = AppModelClass.getTypeListFieldType();
					FieldReference fieldRef = FieldReference.findOrCreate(appModelTypeRef, fieldName, fieldTypeRef);
					int fieldValNum = builder.addLocal();
					
					// Add instruction to get the field (Expecting it to be a descendant of java.util.Collection)
					SSAGetInstruction getInst = instFactory.GetInstruction(fieldValNum, fieldRef);
					builder.addInstruction(getInst);
					int paramValNum = Utils.FIRST_ARG_VAL_NUM + 1;
					
					// Add instruction to add the listener into the list
					MethodReference addMethodRef = MethodId.COLLECTION_ADD_OBJ.getMethodReference();
					builder.addInstsInvocation(addMethodRef, new int[]{fieldValNum, paramValNum}, IInvokeInstruction.Dispatch.VIRTUAL);
					break;
				}
			case ANDROID_CONTEXT_GET_SYS_SERVICE:
				{
					// Return an instance of the returned type
					TypeReference retTypeRef = superMethod.getReturnType();
					int valNum = builder.addAllocation(retTypeRef, null, MAX_ALLOCATION_DEPTH, true);
					SSAReturnInstruction retInst = instFactory.ReturnInstruction(valNum, retTypeRef.isPrimitiveType());
					builder.addInstruction(retInst);
					methodSummary.setFactory(true);
					break;
				}
			default:
				{
					// If the returned type is Context
					TypeReference retTypeRef = superMethod.getReturnType();
					if(getSuperclass().getReference().equals(retTypeRef))
					{			
						// Return this
						assert !superMethod.isStatic();
						SSAReturnInstruction retInst = instFactory.ReturnInstruction(1, false);
						builder.addInstruction(retInst);
					}
					else if(!retTypeRef.equals(TypeReference.Void))
					{
						// Return an instance of the returned type
						int valNum = builder.addAllocation(retTypeRef, null, MAX_ALLOCATION_DEPTH, true);
						SSAReturnInstruction retInst = instFactory.ReturnInstruction(valNum, retTypeRef.isPrimitiveType());
						builder.addInstruction(retInst);
					}
					else
					{
						SSAReturnInstruction retInst = instFactory.ReturnInstruction();
						builder.addInstruction(retInst);
					}
					break;
				}
			}
			List<SSAInstruction> insts = builder.getInstructions();
			for(SSAInstruction inst : insts)
				methodSummary.addStatement(inst);
			if(mLogger.isDebugEnabled())
			{
				for(InstructionException ex : builder.getSuppressedExceptions())
					mLogger.debug("Supressed warning when building context implementation: {}", ex.getMessage());
			}
			SummarizedMethod method = new SummarizedMethod(methodRef, methodSummary, this);
			addMethod(method);
		}
	}
}
