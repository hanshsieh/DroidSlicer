package org.droidslicer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.droidslicer.analysis.FieldSpec;
import org.droidslicer.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.util.warnings.Warning;

/**
 * This class reads method summaries from an XML Stream.
 */
public class XMLBypassSummaryReader 
{
	private static Logger mLogger = LoggerFactory.getLogger(XMLBypassSummaryReader.class);
	/**
	* Governing analysis scope
	*/
	final private AnalysisScope mScope;

	/**
	* Method summaries collected for methods
	*/
	final private HashMap<MethodReference, MethodSummary> mSummaries = HashMapFactory.make();

	/**
	* Set of TypeReferences that are marked as "allocatable"
	*/
	final private HashSet<TypeReference> mAllocatableClasses = HashSetFactory.make();

	/**
	* Set of Atoms that represent packages that can be ignored
	*/
	final private HashMap<Atom, Boolean> mIgnoredPackages = new HashMap<Atom, Boolean>();
	
	final private HashMap<TypeReference, Boolean> mIgnoredClasses = new HashMap<TypeReference, Boolean>();
	
	final private Set<FieldSpec> mFields = HashSetFactory.make();
	
	protected static enum Element
	{
		CLASS_LOADER("class-loader"),
		METHOD("method"),
		CLASS("class"),
		PACKAGE("package"),
		FIELD("field"),
		CALL("call"),
		NEW("new"),
		POISON("poison"),
		SUMMARY_SPEC("summary-spec"),
		RETURN("return"),
		PUT_STATIC("putstatic"),
		AASTORE("aastore"),
		AALOAD("aaload"),
		PUT_FIELD("putfield"),
		GET_FIELD("getfield"),
		THROW("throw"),
		CONSTANT("constant"),
		GET_STATIC("getstatic"),
		PHI("phi");
		private final String mEleName;
		private Element(String eleName)
		{
			mEleName = eleName;
		}
		public String getElementName()
		{
			return mEleName;
		}
	}

	private final static String ARG_PREFIX = "arg";

	//
	// Define XML attribute names
	//
	private final static String A_NAME = "name";

	private final static String A_TYPE = "type";

	private final static String A_CLASS = "class";

	private final static String A_SIZE = "size";

	private final static String A_EXC = "exception";
		
	private final static String A_SIGNATURE = "signature";

	private final static String A_REASON = "reason";

	private final static String A_LEVEL = "level";

	private final static String A_DEF = "def";

	private final static String A_STATIC = "static";
	
	private final static String A_PUBLIC = "public";
	
	private final static String A_PRIVATE = "private";
	
	private final static String A_PROTECTED = "protected";
	
	private final static String A_FINAL = "final";
	
	private final static String A_VOLATILE = "volatile";

	private final static String A_VALUE = "value";

	private final static String A_FIELD = "field";

	private final static String A_FIELD_TYPE = "fieldType";

	private final static String A_ARG = "arg";

	private final static String A_ALLOCATABLE = "allocatable";

	private final static String A_REF = "ref";

	private final static String A_INDEX = "index";

	private final static String A_IGNORE = "ignore";

	private final static String A_FACTORY = "factory";

	private final static String A_NUM_ARGS = "numArgs";
	
	private final static String A_ELE_TYPE = "eleType";

	private final static String V_NULL = "null";

	private final static String V_TRUE = "true";
	
	private final static String V_FALSE = "false";
	
	private final static String V_INT = "int";
	
	private final static String V_SHORT = "short";
	
	private final static String V_FLOAT = "float";
	
	private final static String V_DOUBLE = "double";
	
	private final static String V_CHAR = "char";
	
	private final static String V_LONG = "long";
	
	private final static String V_BOOLEAN = "boolean";
	
	private final static String V_STRING = "string";
	
	private final static String V_VIRTUAL = "virtual";
	
	private final static String V_STATIC = "static";
	
	private final static String V_SPECIAL = "special";
	
	private final static String V_INTERFACE = "interface";
	
	private final static String V_SEVERE = "severe";
	
	private final static String V_MODERATE = "moderate";
	
	private final static String V_MILD = "mild";

	public XMLBypassSummaryReader(InputStream xmlFile, AnalysisScope scope)
			throws IOException, SAXException, ParserConfigurationException
	{
		if (xmlFile == null || scope == null)
			throw new IllegalArgumentException("null");
		mScope = scope;
		readXML(xmlFile);
	}

	private void readXML(InputStream xml) throws SAXException, IOException, ParserConfigurationException
	{
		SAXHandler handler = new SAXHandler();
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.newSAXParser().parse(new InputSource(xml), handler);
	}

	/**
	* @return Method summaries collected for methods. Mapping Object -> MethodSummary where Object is either a
	*				 <ul>
	*				 <li>MethodReference
	*				 <li>TypeReference
	*				 <li>Atom (package name)
	*				 </ul>
	*/
	public Map<MethodReference, MethodSummary> getSummaries()
	{
		return mSummaries;
	}

	/**
	* @return Set of TypeReferences marked "allocatable"
	*/
	public Set<TypeReference> getAllocatableClasses()
	{
		return mAllocatableClasses;
	}

	/**
	* @return Set of Atoms representing ignorable packages
	*/
	public Map<Atom, Boolean> getIgnoredPackages()
	{
		return mIgnoredPackages;
	}
	
	public Map<TypeReference, Boolean> getIgnoredClasses()
	{
		return mIgnoredClasses;
	}
		
	public Set<FieldSpec> getFields()
	{
		return mFields;
	}

	private class SAXHandler extends DefaultHandler
	{
		private Locator mLocator = null;
		/**
		* The class loader reference for the element being processed
		*/
		private ClassLoaderReference mGoverningLoader = null;

		/**
		* The method summary for the element being processed
		*/
		private MethodSummary mGoverningMethod = null;

		/**
		* The declaring class for the element begin processed
		*/
		private TypeReference mGoverningClass = null;

		/**
		* The package for the element being processed
		*/
		private Atom mGoverningPackage = null;

		/**
		* The next available local number for the method being processed
		*/
		private int mNextLocal = -1;

		/**
		* A mapping from String (variable name) -> Integer (local number)
		*/
		private Map<String, Integer> mSymbolTable = null;

		private final Map<String, Element> mEleMap = new HashMap<String, Element>();
		
		public SAXHandler()
		{
			for(Element ele : Element.values())
				mEleMap.put(ele.getElementName(), ele);
		}
		
		
		/*
		* @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		*/
		@Override
		public void startElement(String uri, String name, String qName, Attributes atts)
			throws SAXException
		{
			Element element = mEleMap.get(qName);
			if (element == null)
				throwException("Invalid element: " + qName);
			switch(element)
			{
			case CLASS_LOADER:
				{
					String clName = atts.getValue(A_NAME);
					mGoverningLoader = classLoaderName2Ref(clName);
				}
				break;
			case METHOD:
				startMethod(atts);
				break;
			case CLASS:
				startClass(atts);
				break;
			case PACKAGE:
				startPackage(atts);
				break;
			case CALL:
				processCallSite(atts);
				break;
			case NEW:
				processAllocation(atts);
				break;
			case PUT_STATIC:
				processPutStatic(atts);
				break;
			case GET_STATIC:
				processGetStatic(atts);
				break;
			case PUT_FIELD:
				processPutField(atts);
				break;
			case GET_FIELD:
				processGetField(atts);
				break;
			case THROW:
				processAthrow(atts);
				break;
			case AASTORE:
				processAastore(atts);
				break;
			case AALOAD:
				processAaload(atts);
				break;
			case RETURN:
				processReturn(atts);
				break;
			case POISON:
				processPoison(atts);
				break;
			case CONSTANT:
				processConstant(atts);
				break;
			case PHI:
				processPhi(atts);
				break;
			case SUMMARY_SPEC:
				break;
			case FIELD:
				processField(atts);
				break;
			default:
				throwException("Unexpected element: " + name);
			}
		}

		private int getIntegerAttribute(Attributes atts, String name)
			throws SAXException
		{
			String str = checkAndGetAttribute(atts, name);
			try
			{
				return Integer.parseInt(str);
			}
			catch(NumberFormatException ex)
			{
				throwException("Expecting integer for attribute " + name);
				assert false;
				return -1;
			}
		}
		
		@Override
		public void setDocumentLocator(Locator locator)
		{
			mLocator = locator;
		}
		private void throwException(String msg)
			throws SAXException
		{
			StringBuilder builder = new StringBuilder();
			if(mLocator != null)
			{
				builder.append("Line ");
				builder.append(mLocator.getLineNumber());
				builder.append(", column ");
				builder.append(mLocator.getColumnNumber());
				builder.append(": ");
			}
			builder.append(msg);
			throw new SAXException(builder.toString());
		}
		protected Boolean getBooleanAttribute(Attributes atts, String name)
			throws SAXException
		{
			String val = atts.getValue(name);
			if(val != null)
			{
				if(val.equals(V_TRUE))
					return true;
				else if(val.equals(V_FALSE))
					return false;
				else
					throwException("Attribute \"" + name + "\" must have boolean value");
			}
			return null;
		}
		protected boolean getBooleanAttribute(Attributes atts, String name, boolean defaultVal)
			throws SAXException
		{
			String val = atts.getValue(name);
			if(val != null)
			{
				if(val.equals(V_TRUE))
					return true;
				else if(val.equals(V_FALSE))
					return false;
				else
					throwException("Attribute \"" + name + "\" must have boolean value");
			}
			return defaultVal;
		}
		protected String checkAndGetAttribute(Attributes atts, String name)
			throws SAXException
		{
			String val = atts.getValue(name);
			if(val == null)
				throwException("Missing attribute " + name);
			return val;
		}
		private int getSymbolValue(String name)
			throws SAXException
		{
			Integer val = mSymbolTable.get(name);
			if(val == null)
			{
				if(name.equals(V_NULL))
				{
					int defNum = mNextLocal;
					mSymbolTable.put(V_NULL, new Integer(mNextLocal++));
					return defNum;
				}
				else
					throwException("Variable \"" + name + "\" is undefined");
			}
			return val.intValue();
		}
		private int defineSymbol(String symbolName)
			throws SAXException
		{
			if(symbolName.equals(V_NULL))
				throwException("Symbol name " + V_NULL + " is preserved");

			if (mSymbolTable.keySet().contains(symbolName))
				throwException("Cannot define a symbol twice: " + symbolName);
			int defNum = mNextLocal;
			mSymbolTable.put(symbolName, new Integer(mNextLocal++));
			return defNum;
		}
		private void startPackage(Attributes atts)
			throws SAXException
		{
			String pkgStr = checkAndGetAttribute(atts, A_NAME);
			mGoverningPackage = Atom.findOrCreateUnicodeAtom(pkgStr.replace('.', '/'));
			Boolean ignore = getBooleanAttribute(atts, A_IGNORE);
			if(ignore != null)
				mIgnoredPackages.put(mGoverningPackage, ignore);			
		}
		private void startClass(Attributes atts)
			throws SAXException
		{
			String cname = atts.getValue(A_NAME);
			String clName = "L" + getGoverningPackage() + "/" + cname;
			mGoverningClass = className2Ref(clName);
			if (getBooleanAttribute(atts, A_ALLOCATABLE, false))
				mAllocatableClasses.add(mGoverningClass);
			Boolean ignore = getBooleanAttribute(atts, A_IGNORE);
			if (ignore != null)
				mIgnoredClasses.put(mGoverningClass, ignore);
		}

		/**
		 * Begin processing of a method. 1. Set the governing method. 2. Initialize the nextLocal variable
		 * 
		 * @param atts
		 */
		private void startMethod(Attributes atts)
			throws SAXException
		{
			MethodReference ref = parseMethodSignature(getGoverningClass(), checkAndGetAttribute(atts, A_SIGNATURE));
			mGoverningMethod = new MethodSummary(ref);
			
			mGoverningMethod.setStatic(getBooleanAttribute(atts, A_STATIC, false));
			mGoverningMethod.setFactory(getBooleanAttribute(atts, A_FACTORY, false));
			
			mSummaries.put(ref, mGoverningMethod);
		
			int nParams = ref.getNumberOfParameters();
			if (!mGoverningMethod.isStatic())
				nParams += 1;
		
			// Note that symbol tables reserve v0 for "unknown", so v1 gets assigned
			// to the first parameter "arg0", and so forth.
			mNextLocal = nParams + 1;
			mSymbolTable = HashMapFactory.make(5);
			
			// Create symbols for the parameters
			// arg0 for the first argument, arg1 for the 2nd argument, etc. (Including 'this' parameter)
			// The value number for the arguments starts from 1.
			for (int i = 0; i < nParams; i++)
				mSymbolTable.put(ARG_PREFIX + i, new Integer(Utils.FIRST_ARG_VAL_NUM + i));
		}

		/*
		 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String name, String qName)
			throws SAXException
		{
			Element element = mEleMap.get(qName);
			if(element == null)
				throwException("Unknown element: " + qName);
			switch (element)
			{
			case CLASS_LOADER:
				mGoverningLoader = null;
				break;
			case METHOD:
				{
					checkReturnValue(getGoverningMethod());
					mGoverningMethod = null;
					mSymbolTable = null;
					break;
				}
			case CLASS:
				mGoverningClass = null;
				break;
			case PACKAGE:
				mGoverningPackage = null;
				break;
			default:
				break;
			}
		}

		/**
		 * If a method is declared to return a value, be sure the method summary includes a return statement. 
		 * Throw an exception if not.
		 * 
		 * @param governingMethod
		 */
		private void checkReturnValue(MethodSummary governingMethod)
			throws SAXException
		{
			assert governingMethod != null && governingMethod.getReturnType() != null;
			SSAInstruction[] statements = governingMethod.getStatements();
			for(int i = 0; i < statements.length; ++i)
			{
				SSAInstruction inst = statements[i];
				if(inst instanceof SSAReturnInstruction)
				{
					SSAReturnInstruction retInst = (SSAReturnInstruction)inst;
					if(retInst.returnsVoid() != governingMethod.getReturnType().equals(TypeReference.Void))
						throwException("Invalid return type");
				}
			}
			if (!governingMethod.getReturnType().equals(TypeReference.Void))
			{
				if(statements.length == 0)
					throwException("Method summary must have a return value");
				SSAInstruction lastInst = statements[statements.length - 1];
				if(lastInst instanceof SSAReturnInstruction)
				{
					SSAReturnInstruction retInst = (SSAReturnInstruction)lastInst;
					if(retInst.returnsVoid())
						throwException("Method summary must have a return value");
				}
				else if(lastInst instanceof SSAThrowInstruction)
				{}
				else
					throwException("Method summary must have a return value");
			}
		}
		
		private TypeReference getGoverningClass()
			throws SAXException
		{
			if(mGoverningClass == null)
				throwException("This element should be under class element");
			return mGoverningClass;
		}
		
		private ClassLoaderReference getGoverningClassLoader()
				throws SAXException
		{
			if(mGoverningLoader == null)
				throwException("This element should be under classloader element");
			return mGoverningLoader;
		}
		
		private MethodSummary getGoverningMethod()
				throws SAXException
		{
			if(mGoverningMethod == null)
				throwException("This element should be under method element");
			return mGoverningMethod;
		}
		private Atom getGoverningPackage()
				throws SAXException
		{
			if(mGoverningPackage == null)
				throwException("This element should be under package element");
			return mGoverningPackage;
		}
		
		private void addClassField(FieldReference field, int accessFlags)
		{
			mFields.add(new FieldSpec(field, accessFlags));
		}

		private void processField(Attributes atts)
			throws SAXException
		{	
			
			// deduce the field written
			String nameString = checkAndGetAttribute(atts, A_NAME);
			Atom fieldName = Atom.findOrCreateUnicodeAtom(nameString);

			String ftString = checkAndGetAttribute(atts, A_TYPE);
			TypeReference fieldType = TypeReference.findOrCreate(getGoverningClassLoader(), StringStuff.deployment2CanonicalTypeString(ftString));

			FieldReference field = FieldReference.findOrCreate(getGoverningClass(), fieldName, fieldType);
			
			int accessFlags = 0;
			if(getBooleanAttribute(atts, A_FINAL, false))
				accessFlags |= ClassConstants.ACC_FINAL;
			if(getBooleanAttribute(atts, A_PUBLIC, false))
				accessFlags |= ClassConstants.ACC_PUBLIC;
			if(getBooleanAttribute(atts, A_PRIVATE, false))
				accessFlags |= ClassConstants.ACC_PRIVATE;
			if(getBooleanAttribute(atts, A_PROTECTED, false))
				accessFlags |= ClassConstants.ACC_PROTECTED;
			if(getBooleanAttribute(atts, A_VOLATILE, false))
				accessFlags |= ClassConstants.ACC_VOLATILE;
			if(getBooleanAttribute(atts, A_STATIC, false))
				accessFlags |= ClassConstants.ACC_STATIC;
			addClassField(field, accessFlags);
		}
		/**
		 * Process an element indicating a call instruction
		 * 
		 * @param atts
		 */
		private void processCallSite(Attributes atts)
			throws SAXException
		{
			String typeString = checkAndGetAttribute(atts, A_TYPE);
			String sigString = checkAndGetAttribute(atts, A_SIGNATURE);
			String classString = checkAndGetAttribute(atts, A_CLASS);
			String excString = checkAndGetAttribute(atts, A_EXC);
				
			ClassLoaderReference classLoader = getGoverningClassLoader();
			TypeReference type = TypeReference.findOrCreate(classLoader, StringStuff.deployment2CanonicalTypeString(classString));
			Language lang = mScope.getLanguage(classLoader.getLanguage());
			SSAInstructionFactory insts = lang.instructionFactory();
			MethodReference methodRef = parseMethodSignature(type, sigString);
			CallSiteReference site = null;
			int nParams = methodRef.getNumberOfParameters();
			MethodSummary governingMethod = getGoverningMethod();
			if (typeString.equals(V_VIRTUAL))
			{
				site = CallSiteReference.make(governingMethod.getNextProgramCounter(), methodRef, IInvokeInstruction.Dispatch.VIRTUAL);
				nParams++;
			} 
			else if (typeString.equals(V_SPECIAL))
			{
				site = CallSiteReference.make(governingMethod.getNextProgramCounter(), methodRef, IInvokeInstruction.Dispatch.SPECIAL);
				nParams++;
			} 
			else if (typeString.equals(V_INTERFACE))
			{
				site = CallSiteReference.make(governingMethod.getNextProgramCounter(), methodRef, IInvokeInstruction.Dispatch.INTERFACE);
				nParams++;
			} 
			else if (typeString.equals(V_STATIC))
			{
				site = CallSiteReference.make(governingMethod.getNextProgramCounter(), methodRef, IInvokeInstruction.Dispatch.STATIC);
				// static call has no implicit 'this' parameter
			}
			else
			{
				throwException("Invalid call type \"" + typeString + "\"");
			}
			
			// Array of parameters including implicit 'this'
			int[] params = new int[nParams];

			for (int i = 0; i < params.length; i++)
			{
				String argString = atts.getValue(A_ARG + i);
				if(argString == null)
					throwException("Expecting attribute " + A_ARG + i);
				params[i] = getSymbolValue(argString);
			}

			// allocate local for exceptions
			int exceptionValue = defineSymbol(excString);

			// register the local variable defined by this call, if appropriate
			String defVar = atts.getValue(A_DEF);
			if (defVar != null)
			{
				if(methodRef.getReturnType().equals(TypeReference.Void))
					throwException("Cannot get the return value of a method returns void");
				int defNum = defineSymbol(defVar);				
				governingMethod.addStatement(insts.InvokeInstruction(defNum, params, exceptionValue, site));
			}
			else
			{
				// ignore return value, if any
				governingMethod.addStatement(insts.InvokeInstruction(params, exceptionValue, site));
			}
		}

		/**
		 * Process an element indicating a new allocation site.
		 * 
		 * @param atts
		 */
		private void processAllocation(Attributes atts)
			throws SAXException
		{
			String classString = checkAndGetAttribute(atts, A_CLASS);
			String defVar = checkAndGetAttribute(atts, A_DEF);
			
			ClassLoaderReference classLoader = getGoverningClassLoader();
			MethodSummary governingMethod = getGoverningMethod();
			Language lang = mScope.getLanguage(classLoader.getLanguage());
			SSAInstructionFactory insts = lang.instructionFactory();

			// deduce the concrete type allocated
			final TypeReference type = TypeReference.findOrCreate(classLoader, StringStuff.deployment2CanonicalTypeString(classString));

			// register the local variable defined by this allocation
			int defNum = defineSymbol(defVar);

			// create the allocation statement and add it to the method summary
			NewSiteReference ref = NewSiteReference.make(governingMethod.getNextProgramCounter(), type);

			SSANewInstruction a = null;
			if (type.isArrayType())
			{
				String sizeStr = checkAndGetAttribute(atts, A_SIZE);
				a = insts.NewInstruction(defNum, ref, new int[] { getSymbolValue(sizeStr) });
			}
			else
				a = insts.NewInstruction(defNum, ref);
			governingMethod.addStatement(a);
		}

		/**
		 * Process an element indicating a athrow
		 * 
		 * @param atts
		 */
		private void processAthrow(Attributes atts)
			throws SAXException
		{
			Language lang = mScope.getLanguage(getGoverningClassLoader().getLanguage());
			SSAInstructionFactory insts = lang.instructionFactory();

			// get the value thrown
			String valStr = checkAndGetAttribute(atts, A_VALUE);

			SSAThrowInstruction throwInst = insts.ThrowInstruction(getSymbolValue(valStr));
			getGoverningMethod().addStatement(throwInst);
		}

		/**
		 * Process an element indicating a putfield.
		 * 
		 * @param atts
		 */
		private void processGetField(Attributes atts)
			throws SAXException 
		{
			Language lang = mScope.getLanguage(mGoverningLoader.getLanguage());
			SSAInstructionFactory insts = lang.instructionFactory();

			ClassLoaderReference classLoader = getGoverningClassLoader();
			// Deduce the field written
			String classString = checkAndGetAttribute(atts, A_CLASS);
			TypeReference type = TypeReference.findOrCreate(classLoader, StringStuff.deployment2CanonicalTypeString(classString));

			String fieldString = checkAndGetAttribute(atts, A_FIELD);
			Atom fieldName = Atom.findOrCreateUnicodeAtom(fieldString);

			String ftString = checkAndGetAttribute(atts, A_FIELD_TYPE);
			TypeReference fieldType = TypeReference.findOrCreate(classLoader, StringStuff.deployment2CanonicalTypeString(ftString));

			FieldReference field = FieldReference.findOrCreate(type, fieldName, fieldType);

			// get the value def'fed
			String defVar = checkAndGetAttribute(atts, A_DEF);
			int defNum = defineSymbol(defVar);

			// get the ref read from
			String refStr = checkAndGetAttribute(atts, A_REF);
			
			int ref = getSymbolValue(refStr);

			SSAGetInstruction getInst = insts.GetInstruction(defNum, ref, field);
			getGoverningMethod().addStatement(getInst);
		}

		/**
		 * Process an element indicating a putfield.
		 * 
		 * @param atts
		 */
		private void processPutField(Attributes atts)
			throws SAXException
		{
			Language lang = mScope.getLanguage(mGoverningLoader.getLanguage());
			SSAInstructionFactory insts = lang.instructionFactory();

			ClassLoaderReference classLoader = getGoverningClassLoader();
			
			// deduce the field written
			String classString = checkAndGetAttribute(atts, A_CLASS);
			TypeReference type = TypeReference.findOrCreate(classLoader, StringStuff.deployment2CanonicalTypeString(classString));

			String fieldString = checkAndGetAttribute(atts, A_FIELD);
			Atom fieldName = Atom.findOrCreateUnicodeAtom(fieldString);

			String ftString = checkAndGetAttribute(atts, A_FIELD_TYPE);
			TypeReference fieldType = TypeReference.findOrCreate(classLoader, StringStuff.deployment2CanonicalTypeString(ftString));

			FieldReference field = FieldReference.findOrCreate(type, fieldName, fieldType);

			// get the value stored
			String valStr = checkAndGetAttribute(atts, A_VALUE);
			int valueNumber = getSymbolValue(valStr);

			// get the ref stored to
			String refStr = checkAndGetAttribute(atts, A_REF);
			int refNumber = getSymbolValue(refStr);

			SSAPutInstruction putInst = insts.PutInstruction(refNumber, valueNumber, field);
			getGoverningMethod().addStatement(putInst);
		}

		/**
		 * Process an element indicating a putstatic.
		 * 
		 * @param atts
		 */
		private void processPutStatic(Attributes atts)
			throws SAXException
		{
			Language lang = mScope.getLanguage(mGoverningLoader.getLanguage());
			SSAInstructionFactory insts = lang.instructionFactory();

			ClassLoaderReference classLoader = getGoverningClassLoader();
			
			// deduce the field written
			String classString = checkAndGetAttribute(atts, A_CLASS);
			TypeReference type = TypeReference.findOrCreate(classLoader, StringStuff.deployment2CanonicalTypeString(classString));

			String fieldString = checkAndGetAttribute(atts, A_FIELD);
			Atom fieldName = Atom.findOrCreateUnicodeAtom(fieldString);

			String ftString = checkAndGetAttribute(atts, A_FIELD_TYPE);
			TypeReference fieldType = TypeReference.findOrCreate(classLoader, StringStuff.deployment2CanonicalTypeString(ftString));

			FieldReference field = FieldReference.findOrCreate(type, fieldName, fieldType);

			// get the value stored
			String valStr = checkAndGetAttribute(atts, A_VALUE);
			int valueNumber = getSymbolValue(valStr);
			SSAPutInstruction putInst = insts.PutInstruction(valueNumber, field);
			getGoverningMethod().addStatement(putInst);
		}
		
		/**
		 * Process an element indicating a getstatic.
		 * 
		 * @param atts
		 */
		private void processGetStatic(Attributes atts)
			throws SAXException
		{
			Language lang = mScope.getLanguage(mGoverningLoader.getLanguage());
			SSAInstructionFactory insts = lang.instructionFactory();

			ClassLoaderReference classLoader = getGoverningClassLoader();
			
			// deduce the field written
			String classString = checkAndGetAttribute(atts, A_CLASS);
			TypeReference type = TypeReference.findOrCreate(classLoader, StringStuff.deployment2CanonicalTypeString(classString));

			String fieldString = checkAndGetAttribute(atts, A_FIELD);
			Atom fieldName = Atom.findOrCreateUnicodeAtom(fieldString);

			String ftString = checkAndGetAttribute(atts, A_FIELD_TYPE);
			TypeReference fieldType = TypeReference.findOrCreate(classLoader, StringStuff.deployment2CanonicalTypeString(ftString));

			FieldReference field = FieldReference.findOrCreate(type, fieldName, fieldType);

			// get the value def'fed
			String defVar = checkAndGetAttribute(atts, A_DEF);
			int defNum = defineSymbol(defVar);
						
			SSAGetInstruction getInst = insts.GetInstruction(defNum, field);
			getGoverningMethod().addStatement(getInst);
		}

		/**
		 * Process an element indicating an aastore
		 * 
		 * @param atts
		 */
		private void processAastore(Attributes atts)
			throws SAXException
		{
			Language lang = mScope.getLanguage(mGoverningLoader.getLanguage());
			SSAInstructionFactory insts = lang.instructionFactory();

			ClassLoaderReference classLoader = getGoverningClassLoader();
			
			String refStr = checkAndGetAttribute(atts, A_REF);
			
			int refNumber = getSymbolValue(refStr);
			
			String arrIdxStr = checkAndGetAttribute(atts, A_INDEX);
			int arrIdxValNum = getSymbolValue(arrIdxStr);

			String eleString = checkAndGetAttribute(atts, A_ELE_TYPE);
			TypeReference eleType = TypeReference.findOrCreate(classLoader, StringStuff.deployment2CanonicalTypeString(eleString));
			
			String valStr = checkAndGetAttribute(atts, A_VALUE);
			int valueNumber = getSymbolValue(valStr);
			
			SSAArrayStoreInstruction S = insts.ArrayStoreInstruction(refNumber, arrIdxValNum, valueNumber,
					eleType);
			getGoverningMethod().addStatement(S);
		}
		
		/**
		 * Process an element indicating an Aaload
		 * 
		 * @param atts
		 */
		private void processAaload(Attributes atts)
			throws SAXException
		{
			Language lang = mScope.getLanguage(mGoverningLoader.getLanguage());
			SSAInstructionFactory insts = lang.instructionFactory();

			ClassLoaderReference classLoader = getGoverningClassLoader();
			
			String refStr = checkAndGetAttribute(atts, A_REF);
			int refNumber = getSymbolValue(refStr);
			String arrIdxStr = checkAndGetAttribute(atts, A_INDEX);
			int arrIdxValNum = getSymbolValue(arrIdxStr);
			
			String eleString = checkAndGetAttribute(atts, A_ELE_TYPE);
			TypeReference eleType = TypeReference.findOrCreate(classLoader, StringStuff.deployment2CanonicalTypeString(eleString));
			
			// get the value def'fed
			String defVar = checkAndGetAttribute(atts, A_DEF);
			int defNum = defineSymbol(defVar);
			
			SSAArrayLoadInstruction S = 
					insts.ArrayLoadInstruction(defNum, refNumber, arrIdxValNum,
					eleType);
			getGoverningMethod().addStatement(S);
		}

		/**
		 * Process an element indicating a return statement.
		 * 
		 * @param atts
		 */
		private void processReturn(Attributes atts)
			throws SAXException
		{
			ClassLoaderReference classLoader = getGoverningClassLoader();
			MethodSummary governingMethod = getGoverningMethod();
			Language lang = mScope.getLanguage(classLoader.getLanguage());
			SSAInstructionFactory insts = lang.instructionFactory();
			String retValStr = atts.getValue(A_VALUE);
			if (retValStr == null)
			{
				if(!mGoverningMethod.getReturnType().equals(TypeReference.Void))
					throwException("Missing return value for method " + mGoverningMethod);
				SSAReturnInstruction retInst = insts.ReturnInstruction();
				governingMethod.addStatement(retInst);
			}
			else
			{
				if(mGoverningMethod.getReturnType().equals(TypeReference.Void))
					throwException("Cannot return a value for method with return type void");
				int valueNumber = getSymbolValue(retValStr);
				boolean isPrimitive = mGoverningMethod.getReturnType().isPrimitiveType();
				SSAReturnInstruction retInst = insts.ReturnInstruction(valueNumber, isPrimitive);
				governingMethod.addStatement(retInst);
			}
		}
		
		/**
		 * Process an element indicating a phi statement.
		 * 
		 * @param atts
		 */
		private void processPhi(Attributes atts)
			throws SAXException
		{
			Language lang = mScope.getLanguage(getGoverningClassLoader().getLanguage());
			SSAInstructionFactory insts = lang.instructionFactory();

			String defVar = checkAndGetAttribute(atts, A_DEF);
			int defNum = defineSymbol(defVar);
						
			int nParams = getIntegerAttribute(atts, A_NUM_ARGS);
			
			int[] params = new int[nParams];

			for (int i = 0; i < params.length; i++)
			{
				String argString = checkAndGetAttribute(atts, A_ARG + i);
				params[i] = getSymbolValue(argString);
			}
			
			SSAPhiInstruction phiInst = insts.PhiInstruction(defNum, params);
			getGoverningMethod().addStatement(phiInst);
		}
		

		/**
		 * @param atts
		 */
		private void processConstant(Attributes atts)
			throws SAXException
		{
			String name = checkAndGetAttribute(atts, A_NAME);
			String typeString = checkAndGetAttribute(atts, A_TYPE);
			String valueString = checkAndGetAttribute(atts, A_VALUE);
			int valueNumber = defineSymbol(name);
			
			ConstantValue val = null;
			
			try
			{
				// For the constant types supported by WALA, see 
				// com.ibm.wala.classLoader.JavaLanguage#getConstantType
				if(typeString.equals(V_INT))
				{
					if(valueString.startsWith("0x"))
						val = new ConstantValue(new Integer(Integer.parseInt(valueString.substring(2), 16)));
					else
						val = new ConstantValue(new Integer(valueString));					
				}
				else if(typeString.equals(V_SHORT))
				{
					// Type "short" isn't supported, wrap it in "int"
					if(valueString.startsWith("0x"))
						val = new ConstantValue(new Integer(Integer.parseInt(valueString.substring(2), 16)));
					else
						val = new ConstantValue(new Integer(valueString));
				}
				else if(typeString.equals(V_LONG))
				{
					if(valueString.startsWith("0x"))
						val = new ConstantValue(new Long(Long.parseLong(valueString.substring(2), 16)));
					else
						val = new ConstantValue(new Long(valueString));
				}
				else if(typeString.equals(V_FLOAT))
					val = new ConstantValue(new Float(valueString));
				else if(typeString.equals(V_DOUBLE))
					val = new ConstantValue(new Double(valueString));
				else if(typeString.equals(V_CHAR))
				{
					if(valueString.length() != 1)
						throwException("The value of char must has only one character");
					// Type "char" isn't supported, wrap it in "int"
					val = new ConstantValue(new Integer(valueString.codePointAt(0)));
				}
				else if(typeString.equals(V_STRING))
					val = new ConstantValue(valueString);
				else if(typeString.equals(V_BOOLEAN))
				{
					if(!valueString.equals(V_TRUE) && !valueString.equals(V_FALSE))
						throwException("Invalid boolean value: " + valueString);
					val = new ConstantValue(new Boolean(valueString));
				}
				else
					throwException("Illegal type \"" + typeString + "\"");
			}
			catch(NumberFormatException ex)
			{
				throwException("Illegal number format: \"" + valueString + "\"");
			}
			getGoverningMethod().addConstant(valueNumber, val);
		}

		/**
		 * Process an element which indicates this method is "poison"
		 * 
		 * @param atts
		 */
		private void processPoison(Attributes atts)
			throws SAXException
		{
			MethodSummary governingMethod = getGoverningMethod();
			String reason = atts.getValue(A_REASON);
			if(reason != null)
				governingMethod.addPoison(reason);
			String level = checkAndGetAttribute(atts, A_LEVEL);
			if (level.equals(V_SEVERE))
				governingMethod.setPoisonLevel(Warning.SEVERE);
			else if (level.equals(V_MODERATE))
				governingMethod.setPoisonLevel(Warning.MODERATE);
			else if (level.equals(V_MILD))
				governingMethod.setPoisonLevel(Warning.MILD);
			else
				throwException("Unexpected level: " + level);
		}

		/**
		 * Method classLoaderName2Ref.
		 * 
		 * @param clName
		 * @return ClassLoaderReference
		 */
		protected ClassLoaderReference classLoaderName2Ref(String clName)
			throws SAXException
		{
			ClassLoaderReference loader = mScope.getLoader(Atom.findOrCreateUnicodeAtom(clName));
			if(loader == null)
				throwException("Invalid loader name: " + clName);
			return loader;
		}

		/**
		 * Method classLoaderName2Ref.
		 * 
		 * @param clName
		 * @return ClassLoaderReference
		 */
		protected TypeReference className2Ref(String clName)
			throws SAXException
		{
			return TypeReference.findOrCreate(getGoverningClassLoader(), clName);
		}
		
		/**
		 * Parse a method signature like "java.lang.String read(java.io.InputStream, int)".
		 * Leading and trailing whitespace characters will be ignored.
		 * @param clazzType
		 * @param sigStr
		 * @return
		 * @throws SAXException
		 */
		protected MethodReference parseMethodSignature(TypeReference typeRef, String sigStr)
			throws SAXException
		{
			ClassLoaderReference classLoader = getGoverningClassLoader();
			Language lang = mScope.getLanguage(classLoader.getLanguage());
			return Utils.parseMethodSignature(lang, typeRef, sigStr);
		}
	}

}
