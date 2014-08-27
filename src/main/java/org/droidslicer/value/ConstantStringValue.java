package org.droidslicer.value;

import java.util.Iterator;


public class ConstantStringValue extends ConcreteValue
{
	private final static int MAX_MATCH_DEPTH = 5;
	private final static int MAX_MATCH_SIZE = 50;
	private static final ConstantStringValue EMPTY_STR = new ConstantStringValue("");
	private final String mVal;
	public ConstantStringValue(String val)
	{
		if(val == null)
			throw new IllegalArgumentException("Value cannot be null");
		mVal = val;
	}
	public static ConstantStringValue getEmptyString()
	{
		return EMPTY_STR;
	}
	public String getValue()
	{
		return mVal;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof ConstantStringValue))
			return false;
		ConstantStringValue that = (ConstantStringValue)other;
		return mVal.equals(that.mVal);
	}
	@Override
	public int hashCode()
	{
		return mVal.hashCode();
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('"');
		builder.append(mVal);
		builder.append('"');
		return builder.toString();
	}
	@Override
	public ConcreteValue getStringValue()
	{
		return this;
	}
	private static boolean isPrefixPossibleMatched(ConcreteValue val1, ConcreteValue val2)
	{
		ConcatValue concatVal1 = new ConcatValue(val1);
		ConcatValue concatVal2 = new ConcatValue(val2);
		if(concatVal1.isEmpty() || concatVal2.isEmpty())
		{
			if(concatVal1.isEmpty() && concatVal2.isEmpty())
				return true;
			if((!concatVal1.isEmpty() && concatVal1.getFirst() instanceof UnknownValue) ||
				(!concatVal2.isEmpty() && concatVal2.getFirst() instanceof UnknownValue))
				return true;
			return false;
		}
		ConcreteValue lastSeg1 = concatVal1.getFirst();
		ConcreteValue lastSeg2 = concatVal2.getFirst();
		if(lastSeg1 instanceof UnknownValue || lastSeg2 instanceof UnknownValue)
			return true;
		if(lastSeg1 instanceof ConstantStringValue && lastSeg2 instanceof ConstantStringValue)
		{
			String str1 = ((ConstantStringValue)lastSeg1).getValue();
			String str2 = ((ConstantStringValue)lastSeg2).getValue();
			return str1.startsWith(str2) || str2.startsWith(str1);
		}
		else
			return false;
	}
	private static boolean isSuffixPossibleMatched(ConcreteValue val1, ConcreteValue val2)
	{
		ConcatValue concatVal1 = new ConcatValue(val1);
		ConcatValue concatVal2 = new ConcatValue(val2);
		if(concatVal1.isEmpty() || concatVal2.isEmpty())
		{
			if(concatVal1.isEmpty() && concatVal2.isEmpty())
				return true;
			if((!concatVal1.isEmpty() && concatVal1.getLast() instanceof UnknownValue) ||
				(!concatVal2.isEmpty() && concatVal2.getLast() instanceof UnknownValue))
				return true;
			return false;
		}
		ConcreteValue lastSeg1 = concatVal1.getLast();
		ConcreteValue lastSeg2 = concatVal2.getLast();
		if(lastSeg1 instanceof UnknownValue || lastSeg2 instanceof UnknownValue)
			return true;
		if(lastSeg1 instanceof ConstantStringValue && lastSeg2 instanceof ConstantStringValue)
		{
			String str1 = ((ConstantStringValue)lastSeg1).getValue();
			String str2 = ((ConstantStringValue)lastSeg2).getValue();
			return str1.endsWith(str2) || str2.endsWith(str1);
		}
		else
			return false;
	}
	private static ConcreteValue makePrefixes(ConcreteValue val)
	{
		final OrValue result = new OrValue();
		ConcatValue.walkPrefix(val, new ConcatValue.ConcatEntryWalker()
		{
			private int mCount = 0;
			@Override
			public boolean visit(ConcreteValue val)
			{
				++mCount;
				result.addValue(val);
				if(mCount >= MAX_MATCH_SIZE)
					return false;
				else
					return true;
			}			
			@Override
			public boolean shouldWalk(ConcreteValue val, int depth)
			{
				if(depth > MAX_MATCH_DEPTH)
					return false;
				ConcreteValue lastSeg;
				if(val instanceof ConcatValue)
				{
					ConcatValue concatVal = (ConcatValue)val;
					if(concatVal.isEmpty())
						return true;
					lastSeg = concatVal.getLast();
				}
				else
					lastSeg = val;
				if(!(lastSeg instanceof ConstantStringValue))
					return false;
				else
					return true;
			}
		});
		return result.simplify();
	}
	private static ConcreteValue makeSuffixes(ConcreteValue val)
	{
		final OrValue result = new OrValue();
		ConcatValue.walkSuffix(val, new ConcatValue.ConcatEntryWalker()
		{
			private int mCount = 0;
			@Override
			public boolean visit(ConcreteValue val)
			{
				++mCount;
				result.addValue(val);
				if(mCount >= MAX_MATCH_SIZE)
					return false;
				else
					return true;
			}			
			@Override
			public boolean shouldWalk(ConcreteValue val, int depth)
			{
				ConcreteValue fstSeg;
				if(val instanceof ConcatValue)
				{
					ConcatValue concatVal = (ConcatValue)val;
					if(concatVal.isEmpty())
						return true;
					fstSeg = concatVal.getFirst();
				}
				else
					fstSeg = val;
				if(!(fstSeg instanceof ConstantStringValue))
					return false;
				else
					return true;
			}
		});
		return result.simplify();
	}
	public static boolean isPossibleMatched(ConcreteValue val1, ConcreteValue val2)
	{
		if(UnknownValue.isPossiblelUnknown(val1) || UnknownValue.isPossiblelUnknown(val2))
			return true;
		{
			ConcreteValue prefix1 = makePrefixes(val1);
			ConcreteValue prefix2 = makePrefixes(val2);
			Iterator<ConcreteValue> prefix1Itr = OrValue.getSingleValueIterator(prefix1);
			boolean possibleMatched = false; 
			while(prefix1Itr.hasNext() && !possibleMatched)
			{
				ConcreteValue prefix1Single = prefix1Itr.next();
				Iterator<ConcreteValue> prefix2Itr = OrValue.getSingleValueIterator(prefix2);
				while(prefix2Itr.hasNext() && !possibleMatched)
				{
					ConcreteValue prefix2Single = prefix2Itr.next();
					if(isPrefixPossibleMatched(prefix1Single, prefix2Single))
						possibleMatched = true;
				}
			}
			if(!possibleMatched)
				return false;
		}
		{
			ConcreteValue suffix1 = makeSuffixes(val1);
			ConcreteValue suffix2 = makeSuffixes(val2);
			Iterator<ConcreteValue> suffix1Itr = OrValue.getSingleValueIterator(suffix1);
			boolean possibleMatched = false;
			while(suffix1Itr.hasNext() && !possibleMatched)
			{
				ConcreteValue suffix1Single = suffix1Itr.next();
				Iterator<ConcreteValue> suffix2Itr = OrValue.getSingleValueIterator(suffix2);
				while(suffix2Itr.hasNext() && !possibleMatched)
				{
					ConcreteValue prefix2Single = suffix2Itr.next();
					if(isSuffixPossibleMatched(suffix1Single, prefix2Single))
						possibleMatched = true;
				}
			}
			if(!possibleMatched)
				return false;
		}
		return true;
	}
	public static ConcreteValue fromAndroidSimpleGlob(String str)
	{
		ConcatValue result = new ConcatValue();
		StringBuilder builder = new StringBuilder();
		for(int idx = 0; idx < str.length(); ++idx)
		{
			char ch = str.charAt(idx);
			switch(ch)
			{
			case '\\':
				if(idx + 1 < str.length())
				{
					builder.append(str.charAt(idx + 1));
					++idx;
				}
				break;
			case '*':
				
				// TODO What will Android library for the case like ".**", and 
				// a leading "*" in the string?
				if(builder.length() > 0)
				{
					// Remove the last character if there's one
					builder.setLength(builder.length() - 1);
					result.addValue(new ConstantStringValue(builder.toString()));
					result.addValue(UnknownValue.getInstance());
					builder.setLength(0);
				}
				else
				{
					builder.append(ch);
				}
				break;
			default:
				builder.append(ch);
				break;
			}
		}
		if(builder.length() > 0)
			result.addValue(new ConstantStringValue(builder.toString()));
		return result.simplify();
	}
	public static ConcreteValue toLowerCase(ConcreteValue val)
	{
		if(!(val instanceof OrValue) &&
			!(val instanceof ConcatValue))
		{
			if(val instanceof ConstantStringValue)
			{
				ConstantStringValue constVal = (ConstantStringValue)val;
				return new ConstantStringValue(constVal.getValue().toLowerCase());
			}
			else
				return UnknownValue.getInstance();
		}
		OrValue result = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcatValue concatVal = new ConcatValue(itr.next());
			ConcatValue concatResult = new ConcatValue();
			Iterator<ConcreteValue> conItr = concatVal.iterator();
			while(conItr.hasNext())
			{
				concatResult.addValue(toLowerCase(conItr.next()));
			}
			result.addValue(concatResult);
		}
		return result.simplify();
	}
}
