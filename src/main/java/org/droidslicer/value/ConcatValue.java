package org.droidslicer.value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

public class ConcatValue extends ConcreteValue 
{
	public static interface ConcatEntryWalker
	{
		public boolean shouldWalk(ConcreteValue val, int depth);
		public boolean visit(ConcreteValue val);
	}
	private static class ListEntry
	{
		private final ConcreteValue mVal;
		private ListEntry mNext = null;
		public ListEntry(ConcreteValue val)
		{
			mVal = val;
		}
		public ListEntry getNext()
		{
			return mNext;
		}
		public void setNext(ListEntry next)
		{
			mNext = next;
		}
		public ConcreteValue getValue()
		{
			return mVal;
		}
		public static ListEntry make(Iterator<ConcreteValue> valsItr, ListEntry tail)
		{
			ListEntry head = null;
			ListEntry now = null;
			while(valsItr.hasNext())
			{
				ConcreteValue val = valsItr.next();
				ListEntry next = new ListEntry(val);
				if(now != null)
					now.setNext(next);
				else
					head = next;
				now = next;
			}
			if(now != null)
			{
				now.setNext(tail);
				assert head != null;
				return head;
			}
			else
				return tail;
		}
	}
	private final ArrayList<ConcreteValue> mValues = new ArrayList<ConcreteValue>(2);
	public ConcatValue(ConcreteValue ...vals)
	{
		for(ConcreteValue val : vals)
		{
			addValue(val);
		}
	}
	public ConcreteValue getFirst()
	{
		if(mValues.isEmpty())
			return ConstantStringValue.getEmptyString();
		else
			return mValues.get(0);
	}
	public ConcreteValue getLast()
	{
		if(mValues.isEmpty())
			return ConstantStringValue.getEmptyString();
		else
			return mValues.get(mValues.size() - 1);
	}
	public void addValue(ConcreteValue val)
	{
		if(val == null)
			throw new IllegalArgumentException("Value cannot be null");
		if(val instanceof ConstantStringValue && ((ConstantStringValue)val).getValue().isEmpty())
			return;
		if(val instanceof OrValue && ((OrValue)val).isEmpty())
			return;
		if(!mValues.isEmpty())
		{
			ConcreteValue lastVal = getLast();
			if(val instanceof UnknownValue && lastVal instanceof UnknownValue)
				return;
			else if(val instanceof ConstantStringValue && lastVal instanceof ConstantStringValue)
			{
				ConstantStringValue constStrVal = (ConstantStringValue)val;
				ConstantStringValue lastStr = (ConstantStringValue)lastVal;
				mValues.set(mValues.size() - 1, new ConstantStringValue(lastStr.getValue() + constStrVal.getValue()));
				return;
			}
		}
		if(val instanceof ConcatValue)
		{
			ConcatValue that = (ConcatValue)val;
			mValues.ensureCapacity(mValues.size() + that.size());
			for(ConcreteValue ele : that.mValues)
				addValue(ele);
		}
		else
			mValues.add(val);
	}
	public Iterator<ConcreteValue> iterator()
	{
		return mValues.iterator();
	}
	public List<ConcreteValue> getValues()
	{
		return mValues;
	}
	public boolean isEmpty()
	{
		return mValues.isEmpty();
	}
	public int size()
	{
		return mValues.size();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof ConcatValue))
			return false;
		ConcatValue that = (ConcatValue)other;
		return mValues.equals(that.mValues);
	}
	@Override
	public int hashCode()
	{
		return mValues.hashCode();
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < mValues.size(); ++i)
		{
			if(i > 0)
				builder.append('|');
			builder.append(mValues.get(i).toString());
		}
		return builder.toString();
	}
	@Override
	public ConcreteValue getStringValue()
	{
		return this;
	}
	public ConcreteValue simplify()
	{
		switch(mValues.size())
		{
		case 0:
			return ConstantStringValue.getEmptyString();
		case 1:
			return mValues.get(0);
		default:
			return this;
		}
	}
	private static boolean walkPrefix(ConcreteValue prefix, ListEntry remain, int depth, ConcatEntryWalker walker)
	{
		if(remain == null)
			return walker.visit(prefix);
		if(!walker.shouldWalk(prefix, depth))
			return walker.visit(new ConcatValue(prefix, UnknownValue.getInstance()).simplify());
		ConcreteValue val = remain.getValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof ConcatValue)
			{
				ConcatValue concatVal = (ConcatValue)singleVal;
				ListEntry newRemain = ListEntry.make(concatVal.iterator(), remain.getNext());
				if(!walkPrefix(prefix, newRemain, depth, walker))
					return false;
			}
			else
			{
				if(!walkPrefix(
					new ConcatValue(prefix, singleVal).simplify(), remain.getNext(), 
					depth + 1, 
					walker))
				{
					return false;
				}
			}
		}
		return true;
	}
	private static boolean walkSuffix(ListEntry remain, ConcreteValue suffix, int depth, ConcatEntryWalker walker)
	{
		if(remain == null)
			return walker.visit(suffix);
		if(!walker.shouldWalk(suffix, depth))
			return walker.visit(new ConcatValue(UnknownValue.getInstance(), suffix).simplify());
		ConcreteValue remainVal = remain.getValue();
		Iterator<ConcreteValue> remainItr = OrValue.getSingleValueIterator(remainVal);
		while(remainItr.hasNext())
		{
			ConcreteValue remainSingleVal = remainItr.next();
			if(remainSingleVal instanceof ConcatValue)
			{
				ConcatValue remainConcatVal = (ConcatValue)remainSingleVal;
				ListEntry newRemain = ListEntry.make(Lists.reverse(remainConcatVal.getValues()).iterator(), remain.getNext());
				if(!walkSuffix(newRemain, suffix, depth, walker))
					return false;
			}
			else
			{
				if(!walkSuffix(
					remain.getNext(),
					new ConcatValue(remainSingleVal, suffix).simplify(), 
					depth, 
					walker))
				{
					return false;
				}
			}
		}
		return true;
	}
	public static void walkPrefix(ConcreteValue val, ConcatEntryWalker walker)
	{
		walkPrefix(ConstantStringValue.getEmptyString(), new ListEntry(val), 0, walker);	
	}
	public static void walkSuffix(ConcreteValue val, ConcatEntryWalker walker)
	{
		walkSuffix(new ListEntry(val), ConstantStringValue.getEmptyString(), 0, walker);	
	}
}
