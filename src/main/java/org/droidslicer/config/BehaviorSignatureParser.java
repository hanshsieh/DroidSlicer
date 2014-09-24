package org.droidslicer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.droidslicer.graph.entity.ActivityUnit;
import org.droidslicer.graph.entity.ApplicationUnit;
import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.ProviderUnit;
import org.droidslicer.graph.entity.ReceiverUnit;
import org.droidslicer.graph.entity.ServiceUnit;
import org.droidslicer.signature.BehaviorSignature;
import org.droidslicer.signature.DatabaseUnitSignature;
import org.droidslicer.signature.FileUnitSignature;
import org.droidslicer.signature.FlowSignature;
import org.droidslicer.signature.ICCParamCalleeUnitSignature;
import org.droidslicer.signature.ICCParamCallerUnitSignature;
import org.droidslicer.signature.ICCRetCalleeUnitSignature;
import org.droidslicer.signature.ICCRetCallerUnitSignature;
import org.droidslicer.signature.PermUnitSignature;
import org.droidslicer.signature.SemanticSignatureContext;
import org.droidslicer.signature.SharedPreferencesUnitSignature;
import org.droidslicer.signature.SocketUnitSignature;
import org.droidslicer.signature.UnitSignaturesUnion;
import org.droidslicer.signature.UrlConnUnitSignature;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.ConstantStringValue;
import org.droidslicer.value.IntValue;
import org.droidslicer.value.UnknownValue;

import com.google.common.base.CharMatcher;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class BehaviorSignatureParser
{
	protected static class FlowSpec
	{
		private final String mFromId;
		private final String mToId;
		public FlowSpec(String fromId, String toId)
		{
			mFromId = fromId;
			mToId = toId;
		}
		public String getFromId()
		{
			return mFromId;
		}
		public String getToId()
		{
			return mToId;
		}
	}
	protected static enum SignatureOperator
	{
		OR(0, true, new String[]{"OR", "or", "||"}),
		AND(1, true, new String[]{"AND", "and", "&&"}),
		NOT(2, false, new String[]{"NOT", "not", "!"}),
		LEFT_PARANTHESIS(3, false, new String[]{"("});
		private final int mPrecedence;
		private final String[] mStrs;
		private final boolean mLeftAssociative;
		private SignatureOperator(int precedence, boolean leftAssociative, String[] strs)
		{
			mPrecedence = precedence;
			mStrs = strs;
			mLeftAssociative = leftAssociative;
		}
		public boolean isLeftAssociative()
		{
			return mLeftAssociative;
		}
		public int getPrecedence()
		{
			return mPrecedence;
		}
		public String[] getStrings()
		{
			return mStrs;
		}
	}
	protected class SignatureTokenizer implements Iterator<String>
	{
		private final String mStr; 
		private String mNextTok = null;
		private int mStartIdx = 0;
		public SignatureTokenizer(String str)
		{
			mStr = str;
			emit();
		}
		@Override
		public boolean hasNext()
		{
			return mNextTok != null;
		}

		protected boolean checkDelim(int idx)
		{
			for(SignatureOperator op : SignatureOperator.values())
			{
				for(String str : op.getStrings())
				{
					if(mStr.startsWith(str, idx))
					{
						mNextTok = str;
						mStartIdx = idx + str.length();
						return true;
					}
				}
			}
			return false;
		}
		protected void emit()
		{
			mNextTok = null;
			int endIdx;
			for(endIdx = mStartIdx; endIdx < mStr.length(); ++endIdx)
			{
				char ch = mStr.charAt(endIdx);
				if(!mIdAllowedChars.matches(ch))
				{
					if(mStartIdx < endIdx)
					{
						mNextTok = mStr.substring(mStartIdx, endIdx);
						mStartIdx = endIdx;
						return;
					}
					else if(Character.isWhitespace(ch))
					{
						mStartIdx = endIdx + 1;
					}
					else
					{
						if(checkDelim(endIdx))
							return;
						throw new IllegalArgumentException("Illegal character: " + ch);
					}
				}
			}
			if(mStartIdx < endIdx)
				mNextTok = mStr.substring(mStartIdx, endIdx);
			mStartIdx = endIdx;
		}
		@Override
		public String next()
		{
			if(mNextTok == null)
				throw new IllegalStateException("No more token");
			String result = mNextTok;
			emit();
			return result;
		}

		@Override
		public void remove()
		{
			throw new RuntimeException("Unsupported");
		}
	}
	private final Map<String, BehaviorSignatureXMLElement> mEleMap = new HashMap<String, BehaviorSignatureXMLElement>();
	private XMLStreamReader mXmlReader;
	private final Collection<BehaviorSignature> mSemSigs = new ArrayList<BehaviorSignature>();
	private final CharMatcher mIdAllowedChars;
	
	// Component
	private Class<? extends ComponentUnit> mCompType = null;
	private Boolean mIsSystemComp = null;
	
	// Data
	private String mDataId;
	
	// Flow spec
	private String mFlowId;
	private final Map<String, Object> mSigSpecs = new HashMap<String, Object>();
	
	// Semantic signatures
	private final Map<String, String> mSemSigsDesc = new HashMap<String, String>();
	
	public BehaviorSignatureParser(InputStream input)
		throws IOException
	{
		{
			mIdAllowedChars = CharMatcher.inRange('A', 'Z')
				.or(CharMatcher.inRange('a', 'z'))
				.or(CharMatcher.is('_'))
				.or(CharMatcher.inRange('0', '9'));
		}
		XMLInputFactory factory = XMLInputFactory.newInstance();
		try
		{
			for(BehaviorSignatureXMLElement ele : BehaviorSignatureXMLElement.values())
			{
				mEleMap.put(ele.getTagName(), ele);
			}
			mXmlReader = factory.createXMLStreamReader(input);
			while(mXmlReader.hasNext())
			{
				switch(mXmlReader.next())
				{
				case XMLStreamReader.START_ELEMENT:
					onStartElement();
					break;
				case XMLStreamReader.END_ELEMENT:
					onEndElement();
					break;
				}
			}
			buildSemanticSignatures();
		}
		catch(XMLStreamException ex)
		{
			throw new IOException(ex);
		}
		finally
		{
			if(mXmlReader != null)
			{
				try
				{
					mXmlReader.close();
				}
				catch(XMLStreamException ex)
				{
					throw new IOException(ex);
				}
				mXmlReader = null;
			}
			mSemSigsDesc.clear();
			mSigSpecs.clear();
			mEleMap.clear();
		}
	}
	public Collection<BehaviorSignature> getSemanticSignatures()
	{
		return mSemSigs;
	}
	protected void buildSemanticSignatures()
		throws XMLStreamException
	{
		for(Map.Entry<String, String> entry : mSemSigsDesc.entrySet())
		{
			String name = entry.getKey();
			String desc = entry.getValue();
			Predicate<SemanticSignatureContext> sig = parseSignatureString(desc);
			mSemSigs.add(new BehaviorSignature(name, sig));
		}
	}
	protected SignatureOperator getOperator(String tok)
	{
		for(SignatureOperator op : SignatureOperator.values())
		{
			for(String str : op.getStrings())
			{
				if(str.equals(tok))
				{
					return op;
				}
			}
		}
		return null;
	}
	protected void evaluateOperator(SignatureOperator op, Stack<Predicate<SemanticSignatureContext>> operands)
		throws IllegalArgumentException
	{
		switch(op)
		{
		case AND:
			{
				if(operands.size() < 2)
					throw new IllegalArgumentException("Missing matching operands for \"" + op.name() + "\"");
				Predicate<SemanticSignatureContext> rightOperand = operands.pop();
				Predicate<SemanticSignatureContext> leftOperand = operands.pop();
				operands.push(Predicates.and(leftOperand, rightOperand));
				break;
			}
		case OR:
			{
				if(operands.size() < 2)
					throw new IllegalArgumentException("Missing matching operands for \"" + op.name() + "\"");
				Predicate<SemanticSignatureContext> rightOperand = operands.pop();
				Predicate<SemanticSignatureContext> leftOperand = operands.pop();
				operands.push(Predicates.or(leftOperand, rightOperand));
				break;
			}
		case NOT:
			{
				if(operands.size() < 1)
					throw new IllegalArgumentException("Missing matching operand for " + op.name());
				Predicate<SemanticSignatureContext> operand = operands.pop();
				operands.push(Predicates.not(operand));
				break;
			}
		default:
			throw new IllegalArgumentException("Operator " + op + " cannot be evaluated");
		}
	}
	protected Predicate<SemanticSignatureContext> makeOperand(String id)
	{
		Object sigSpec = mSigSpecs.get(id);
		if(sigSpec == null)
			throw new IllegalArgumentException("Fail to find a flow signature or unit signature with ID \"" + id + "\"");
		if(sigSpec instanceof UnitSignaturesUnion)
		{
			final UnitSignaturesUnion unitSig = (UnitSignaturesUnion)sigSpec;
			return new Predicate<SemanticSignatureContext>()
					{
						@Override
						public boolean apply(SemanticSignatureContext ctx)
						{
							return ctx.evaluate(unitSig);
						}
					};
		}
		else if(sigSpec instanceof FlowSpec)
		{
			FlowSpec flowSpec = (FlowSpec)sigSpec;
			Object fromSigSpec = mSigSpecs.get(flowSpec.getFromId());
			Object toSigSpec = mSigSpecs.get(flowSpec.getToId());
			if(!(fromSigSpec instanceof UnitSignaturesUnion) || 
				!(toSigSpec instanceof UnitSignaturesUnion))
			{
				throw new IllegalArgumentException(
					"Expecting the attribute value of " + BehaviorSignatureXMLElement.A_FROM + " and " + 
					BehaviorSignatureXMLElement.A_TO + " to be ID of unit signature");
			}
			UnitSignaturesUnion fromSig = (UnitSignaturesUnion)fromSigSpec;
			UnitSignaturesUnion toSig = (UnitSignaturesUnion)toSigSpec;
			final FlowSignature flowSig = new FlowSignature(fromSig, toSig);
			return new Predicate<SemanticSignatureContext>()
					{
						@Override
						public boolean apply(SemanticSignatureContext ctx)
						{
							return ctx.evaluate(flowSig);
						}
					};
		}
		else
			throw new IllegalArgumentException("Unsupported type of signature: " + sigSpec.getClass().getName());
	}
	protected Predicate<SemanticSignatureContext> parseSignatureString(String desc)
		throws XMLStreamException
	{
		try
		{
			Stack<Predicate<SemanticSignatureContext>> operands = new Stack<Predicate<SemanticSignatureContext>>();
			Stack<SignatureOperator> operators = new Stack<SignatureOperator>();
			SignatureTokenizer toks = new SignatureTokenizer(desc);
			while(toks.hasNext())
			{
				String tok = toks.next();
				if(tok.equals(")"))
				{
					while(!operators.isEmpty())
					{
						SignatureOperator lastOp = operators.pop();
						if(lastOp.equals(SignatureOperator.LEFT_PARANTHESIS))
							break;
						evaluateOperator(lastOp, operands);
					}
					continue;
				}
				SignatureOperator op = getOperator(tok);
				if(op != null)
				{
					int prec = op.getPrecedence();
					while(!operators.isEmpty())
					{
						SignatureOperator lastOp = operators.peek();
						int lastPrec = lastOp.getPrecedence();
						if(lastPrec < prec || (lastPrec == prec && !op.isLeftAssociative()))
							break;
						operators.pop();
						evaluateOperator(lastOp, operands);
					}
					operators.add(op);
				}
				else
				{
					operands.push(makeOperand(tok));
				}
			}
			while(!operators.isEmpty())
			{
				SignatureOperator op = operators.pop();
				evaluateOperator(op, operands);
			}
			assert operands.size() == 1;
			return operands.pop();
		}
		catch(IllegalArgumentException ex)
		{
			throw new XMLStreamException(ex);
		}
	}
	protected String getMessagePrefix()
	{
		Location loc = mXmlReader.getLocation();
		int lineNum = loc.getLineNumber();
		int colNum = loc.getColumnNumber();
		StringBuilder builder = new StringBuilder("Line ");
		builder.append(lineNum);
		builder.append(", column ");
		builder.append(colNum);
		builder.append(": ");
		return builder.toString();
	}
	protected void onEleComponent()
		throws XMLStreamException
	{
		int nAttr = mXmlReader.getAttributeCount();
		mCompType = null;
		mIsSystemComp = null;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);
			if(attrName.equals(BehaviorSignatureXMLElement.A_TYPE))
			{
				if(attrVal.equals(BehaviorSignatureXMLElement.V_ACTIVITY))
					mCompType = ActivityUnit.class;
				else if(attrVal.equals(BehaviorSignatureXMLElement.V_RECEIVER))
					mCompType = ReceiverUnit.class;
				else if(attrVal.equals(BehaviorSignatureXMLElement.V_SERVICE))
					mCompType = ServiceUnit.class;
				else if(attrVal.equals(BehaviorSignatureXMLElement.V_PROVIDER))
					mCompType = ProviderUnit.class;
				else if(attrVal.equals(BehaviorSignatureXMLElement.V_APPLICATION))
					mCompType = ApplicationUnit.class;
				else if(attrVal.equals(BehaviorSignatureXMLElement.V_ANY))
					mCompType = null;
				else
					throw new XMLStreamException(getMessagePrefix() + "Illegal value for attribute " + BehaviorSignatureXMLElement.A_TYPE);		
			}
			else if(attrName.equals(BehaviorSignatureXMLElement.A_IS_SYSTEM))
			{
				if(attrVal.equals(BehaviorSignatureXMLElement.V_TRUE))
					mIsSystemComp = true;
				else if(attrVal.equals(BehaviorSignatureXMLElement.V_FALSE))
					mIsSystemComp = false;
				else if(attrVal.equals(BehaviorSignatureXMLElement.V_ANY))
					mIsSystemComp = null;
				else
					throw new XMLStreamException(getMessagePrefix() + "Illegal value for attribute " + BehaviorSignatureXMLElement.A_IS_SYSTEM);					
			}
			else 
				throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
	}
	protected void onEleData()
		throws XMLStreamException
	{
		int nAttr = mXmlReader.getAttributeCount();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);
			if(attrName.equals(BehaviorSignatureXMLElement.A_ID))
			{
				mDataId = attrVal;
				if(!mIdAllowedChars.matchesAllOf(mDataId))
					throw new XMLStreamException(getMessagePrefix() + "Illegal characters in attribute " + BehaviorSignatureXMLElement.A_ID);
			}
			else 
				throw new XMLStreamException(getMessagePrefix() + "Illegal attribute \"" + attrName + "\"");
		}
		if(mDataId == null)
			throw new XMLStreamException(getMessagePrefix() + "Expecting attribute " + BehaviorSignatureXMLElement.A_ID);
		UnitSignaturesUnion sigs = new UnitSignaturesUnion();
		if(mSigSpecs.containsKey(mDataId))
			throw new XMLStreamException(getMessagePrefix() + "Duplication ID for <" + BehaviorSignatureXMLElement.DATA.getTagName() + ">");
		mSigSpecs.put(mDataId, sigs);
	}
	protected void onElePermission()	
		throws XMLStreamException
	{
		if(mDataId == null)
			throw new XMLStreamException(getMessagePrefix() + "Illegal position of <" + BehaviorSignatureXMLElement.PERMISSION.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		PermUnitSignature sig = new PermUnitSignature(mCompType, mIsSystemComp);
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);
			if(attrName.equals(BehaviorSignatureXMLElement.A_PERMISSIONS))
			{
				String[] toks = attrVal.split(";");
				for(String tok : toks)
				{
					tok = tok.trim();
					if(tok.isEmpty())
						continue;
					sig.addPermission(tok);
				}
			}
			else 
				throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		UnitSignaturesUnion sigs = getUnitSignatureUnion();
		sigs.addSigature(sig);
	}
	protected UnitSignaturesUnion getUnitSignatureUnion()
	{
		Object sigSpec = mSigSpecs.get(mDataId);
		if(!(sigSpec instanceof UnitSignaturesUnion))
		{
			throw new RuntimeException("Expecting data with ID \"" + mDataId + "\"");
		}
		return (UnitSignaturesUnion)sigSpec;
	}
	protected void onEleFile()
		throws XMLStreamException
	{
		if(mDataId == null)
			throw new XMLStreamException(getMessagePrefix() + "Illegal position of <" + BehaviorSignatureXMLElement.FILE.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		ConcreteValue pathVal = UnknownValue.getInstance();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);
			if(attrName.equals(BehaviorSignatureXMLElement.A_PATH))
			{
				pathVal = ConstantStringValue.fromAndroidSimpleGlob(attrVal);
			}
			else 
				throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		UnitSignaturesUnion sigs = getUnitSignatureUnion();
		FileUnitSignature sig = new FileUnitSignature(mCompType, mIsSystemComp, pathVal);
		sigs.addSigature(sig);
	}
	protected void onEleDatabase()
			throws XMLStreamException
	{
		if(mDataId == null)
			throw new XMLStreamException(getMessagePrefix() + "Illegal position of <" + BehaviorSignatureXMLElement.DATABASE.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		if(nAttr > 0)
		{
			String attrName = mXmlReader.getAttributeLocalName(0);
			throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		UnitSignaturesUnion sigs = getUnitSignatureUnion();
		DatabaseUnitSignature sig = new DatabaseUnitSignature(mCompType, mIsSystemComp);
		sigs.addSigature(sig);
	}
	protected void onEleSharedPreferences()
			throws XMLStreamException
	{
		if(mDataId == null)
			throw new XMLStreamException(getMessagePrefix() + "Illegal position of <" + BehaviorSignatureXMLElement.SHARED_PREFERENCES.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		if(nAttr > 0)
		{
			String attrName = mXmlReader.getAttributeLocalName(0);
			throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		UnitSignaturesUnion sigs = getUnitSignatureUnion();
		SharedPreferencesUnitSignature sig = new SharedPreferencesUnitSignature(mCompType, mIsSystemComp);
		sigs.addSigature(sig);
	}
	protected void onEleSocket()
			throws XMLStreamException
	{
		if(mDataId == null)
			throw new XMLStreamException(getMessagePrefix() + "Illegal position of <" + BehaviorSignatureXMLElement.SOCKET.getTagName() + ">");
		ConcreteValue addrVal = UnknownValue.getInstance();
		int port = -1;
		int nAttr = mXmlReader.getAttributeCount();
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);
			if(attrName.equals(BehaviorSignatureXMLElement.A_ADDR))
			{
				addrVal = ConstantStringValue.fromAndroidSimpleGlob(attrVal);
			}
			else if(attrName.equals(BehaviorSignatureXMLElement.A_PORT))
			{
				try
				{
					port = Integer.parseInt(attrVal);
				}
				catch(NumberFormatException ex)
				{
					throw new XMLStreamException("Attribute value of \"" + BehaviorSignatureXMLElement.A_PORT + "\" must be integer");
				}
			}
			else 
				throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		UnitSignaturesUnion sigs = getUnitSignatureUnion();
		SocketUnitSignature sig = new SocketUnitSignature(mCompType, mIsSystemComp, addrVal, 
				port < 0 ? UnknownValue.getInstance() : new IntValue(port));
		sigs.addSigature(sig);
	}
	protected void onEleUrlConn()
			throws XMLStreamException
	{
		if(mDataId == null)
			throw new XMLStreamException(getMessagePrefix() + "Illegal position of <" + BehaviorSignatureXMLElement.URL_CONN.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		if(nAttr > 0)
		{
			String attrName = mXmlReader.getAttributeLocalName(0);
			throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		UnitSignaturesUnion sigs = getUnitSignatureUnion();
		UrlConnUnitSignature sig = new UrlConnUnitSignature(mCompType, mIsSystemComp);
		sigs.addSigature(sig);
	}
	protected void onEleICCParamCaller()
		throws XMLStreamException
	{
		if(mDataId == null)
			throw new XMLStreamException(getMessagePrefix() + "Illegal position of <" + BehaviorSignatureXMLElement.ICC_PARAM_CALLER.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		if(nAttr > 0)
		{
			String attrName = mXmlReader.getAttributeLocalName(0);
			throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		UnitSignaturesUnion sigs = getUnitSignatureUnion();
		ICCParamCallerUnitSignature sig = new ICCParamCallerUnitSignature(mCompType, mIsSystemComp);
		sigs.addSigature(sig);
	}
	protected void onEleICCParamCallee()
			throws XMLStreamException
	{
		if(mDataId == null)
			throw new XMLStreamException(getMessagePrefix() + "Illegal position of <" + BehaviorSignatureXMLElement.ICC_PARAM_CALLEE.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		if(nAttr > 0)
		{
			String attrName = mXmlReader.getAttributeLocalName(0);
			throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		UnitSignaturesUnion sigs = getUnitSignatureUnion();
		ICCParamCalleeUnitSignature sig = new ICCParamCalleeUnitSignature(mCompType, mIsSystemComp);
		sigs.addSigature(sig);
	}
	protected void onEleICCRetCaller()
			throws XMLStreamException
	{
		if(mDataId == null)
			throw new XMLStreamException(getMessagePrefix() + "Illegal position of <" + BehaviorSignatureXMLElement.ICC_RET_CALLER.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		if(nAttr > 0)
		{
			String attrName = mXmlReader.getAttributeLocalName(0);
			throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		UnitSignaturesUnion sigs = getUnitSignatureUnion();
		ICCRetCallerUnitSignature sig = new ICCRetCallerUnitSignature(mCompType, mIsSystemComp);
		sigs.addSigature(sig);
	}
	protected void onEleICCRetCallee()
			throws XMLStreamException
	{
		if(mDataId == null)
			throw new XMLStreamException(getMessagePrefix() + "Illegal position of <" + BehaviorSignatureXMLElement.ICC_RET_CALLEE.getTagName() + ">");
		int nAttr = mXmlReader.getAttributeCount();
		if(nAttr > 0)
		{
			String attrName = mXmlReader.getAttributeLocalName(0);
			throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		UnitSignaturesUnion sigs = getUnitSignatureUnion();
		ICCRetCalleeUnitSignature sig = new ICCRetCalleeUnitSignature(mCompType, mIsSystemComp);
		sigs.addSigature(sig);
	}
	protected void onEleFlow()
		throws XMLStreamException
	{
		int nAttr = mXmlReader.getAttributeCount();
		String id = null, from = null, to = null;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);
			if(attrName.equals(BehaviorSignatureXMLElement.A_ID))
			{
				id = attrVal;
				if(!mIdAllowedChars.matchesAllOf(id))
					throw new XMLStreamException(getMessagePrefix() + "Illegal characters in attribute " + BehaviorSignatureXMLElement.A_ID);
			}
			else if(attrName.equals(BehaviorSignatureXMLElement.A_FROM))
				from = attrVal;
			else if(attrName.equals(BehaviorSignatureXMLElement.A_TO))
				to = attrVal;
			else
				throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		if(id == null || from == null)
		{
			throw new XMLStreamException(getMessagePrefix() + 
				"Attributes " + BehaviorSignatureXMLElement.A_ID + ", " + 
				BehaviorSignatureXMLElement.A_FROM + " are necessary");
		}
		if(mSigSpecs.containsKey(id))
			throw new XMLStreamException(getMessagePrefix() + "Duplicate ID of <" + BehaviorSignatureXMLElement.FLOW.getTagName() + ">");
		mSigSpecs.put(id, new FlowSpec(from, to));
	}
	protected void onEleSignature()
		throws XMLStreamException
	{
		int nAttr = mXmlReader.getAttributeCount();
		String id = null, desc = null;
		for(int i = 0; i < nAttr; ++i)
		{
			String attrName = mXmlReader.getAttributeLocalName(i);
			String attrVal = mXmlReader.getAttributeValue(i);
			if(attrName.equals(BehaviorSignatureXMLElement.A_ID))
			{
				id = attrVal;
				if(!mIdAllowedChars.matchesAllOf(id))
					throw new XMLStreamException(getMessagePrefix() + "Illegal characters in attribute " + BehaviorSignatureXMLElement.A_ID);
			}
			else if(attrName.equals(BehaviorSignatureXMLElement.A_DEFINITION))
				desc = attrVal;
			else
				throw new XMLStreamException(getMessagePrefix() + "Illegal attribute " + attrName);
		}
		if(id == null || desc == null)
		{
			throw new XMLStreamException(getMessagePrefix() + 
				"Attributes " + BehaviorSignatureXMLElement.A_ID + " and " + 
				BehaviorSignatureXMLElement.A_DEFINITION  + " are necessary");
		}
		if(mSemSigsDesc.containsKey(id))
			throw new XMLStreamException(getMessagePrefix() + "Duplicate ID of <" + BehaviorSignatureXMLElement.SIGNATURE.getTagName() + ">");
		mSemSigsDesc.put(id, desc);
	}
	protected void onStartElement()
		throws XMLStreamException
	{
		String eleName = mXmlReader.getLocalName();
		BehaviorSignatureXMLElement ele = mEleMap.get(eleName);
		if(ele == null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid element name " + eleName);
		switch(ele)
		{
		case SPEC:
			break;
		case DATA_SPEC:
			break;
		case COMPONENT:
			onEleComponent();
			break;
		case DATA:
			onEleData();
			break;
		case FLOW_SPEC:
			break;
		case FLOW:
			onEleFlow();
			break;
		case SIGNATURES:
			break;
		case SIGNATURE:
			onEleSignature();
			break;
		case PERMISSION:
			onElePermission();
			break;
		case FILE:
			onEleFile();
			break;
		case DATABASE:
			onEleDatabase();
			break;
		case SHARED_PREFERENCES:
			onEleSharedPreferences();
			break;
		case SOCKET:
			onEleSocket();
			break;
		case URL_CONN:
			onEleUrlConn();
			break;
		case ICC_PARAM_CALLER:
			onEleICCParamCaller();
			break;
		case ICC_PARAM_CALLEE:
			onEleICCParamCallee();
			break;
		case ICC_RET_CALLER:
			onEleICCRetCaller();
			break;
		case ICC_RET_CALLEE:
			onEleICCRetCallee();
			break;
		default:
			throw new RuntimeException("Unreachable");
		}
	}
	protected void onEndElement()
		throws XMLStreamException
	{
		String eleName = mXmlReader.getLocalName();
		BehaviorSignatureXMLElement ele = mEleMap.get(eleName);
		if(ele == null)
			throw new XMLStreamException(getMessagePrefix() + "Invalid element name " + eleName);
		switch(ele)
		{
		case SPEC:
			break;
		case DATA_SPEC:
			break;
		case COMPONENT:
			mCompType = null;
			mIsSystemComp = null;
			break;
		case DATA:
			mDataId = null;
			break;
		case PERMISSION:
		case FILE:
		case DATABASE:
		case SOCKET:
		case SHARED_PREFERENCES:
		case URL_CONN:
		case FLOW_SPEC:
		case FLOW:
		case SIGNATURES:
		case SIGNATURE:
		case ICC_PARAM_CALLER:
		case ICC_PARAM_CALLEE:
		case ICC_RET_CALLER:
		case ICC_RET_CALLEE:
			break;
		default:
			throw new RuntimeException("Unreachable");
		}
	}
}
