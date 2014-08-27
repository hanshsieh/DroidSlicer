package org.droidslicer.value;

import java.util.Iterator;

import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;


public class FileValue extends ConcreteValue
{
	// It should be a string value
	private final ConcreteValue mPath;
	public FileValue(ConcreteValue parent, ConcreteValue child)
	{
		if(parent == null || child == null)
			throw new IllegalArgumentException();
		OrValue path = new OrValue();
		ConcreteValue prefix = makePathWithoutTrailingSlash(parent);
		ConcreteValue suffix = makeAbsolutePath(child, true);
		if(NullValue.isPossibleNotNull(prefix))
			path.addValue(new ConcatValue(NullValue.excludeNullValue(prefix), suffix));
		if(NullValue.isPossibleNull(prefix))
			path.addValue(suffix);
		mPath = path.simplify();
	}
	public FileValue(ConcreteValue child)
	{
		if(child == null)
			throw new IllegalArgumentException();
		mPath = child.getStringValue();
	}
	public ConcreteValue getPath()
	{
		return mPath;
	}
	public static ConcreteValue resolvePath(ConcreteValue file)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(file);
		OrValue result = new OrValue();
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof FileValue)
				result.addValue(((FileValue)singleVal).getPath());
			else if(singleVal instanceof AbstractFileStreamValue)
				result.addValue(((AbstractFileStreamValue)singleVal).getPath());
			else
				result.addValue(UnknownValue.getInstance());
		}
		return result.simplify();
	}
	public static ConcreteValue resolveAbsolutePath(ConcreteValue file)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(file);
		OrValue result = new OrValue();
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof FileValue)
			{
				// On Android, the current directory is at root "/"
				result.addValue(makeAbsolutePath(((FileValue)singleVal).getPath(), false));
			}
			else if(singleVal instanceof AbstractFileStreamValue)
				result.addValue(((AbstractFileStreamValue)singleVal).getPath());
			else
				result.addValue(UnknownValue.getInstance());
		}
		return result.simplify();
	}
	protected static boolean isMethodRetPossibleMatch(MethodReturnValue retVal1, ConcreteValue singleVal2)
	{
		if(singleVal2 instanceof UnknownValue)
			return true;
		if(!(singleVal2 instanceof MethodReturnValue))
		{
			// TODO Maybe we can do better
			return true;
		}
		MethodReturnValue retVal2 = (MethodReturnValue)singleVal2;
		MethodReference methodRef1 = retVal1.getMethod();
		MethodReference methodRef2 = retVal2.getMethod();
		// The they are the return values of different methods, directly 
		// assume that they are different file
		// TODO More accurate
		if(!methodRef1.equals(methodRef2) || retVal1.isStatic() != retVal2.isStatic())
			return false;
		boolean isStatic = retVal1.isStatic();
		int nParam = methodRef1.getNumberOfParameters();
		for(int i = 0; i < nParam; ++i)
		{
			TypeReference paramTypeRef = methodRef1.getParameterType(i);
			if(!paramTypeRef.getName().equals(TypeReference.JavaLangString.getName()))
				continue;
			int useIdx = isStatic ? i : i + 1;
			ConcreteValue paramVal1 = retVal1.getParamValue(useIdx);
			ConcreteValue paramVal2 = retVal2.getParamValue(useIdx);
			if(paramVal1 == null || paramVal2 == null)
				continue;
			if(!ConstantStringValue.isPossibleMatched(paramVal1, paramVal2))
				return false;
		}
		return true;
	}
	public static boolean isPossibleMatched(ConcreteValue val1, ConcreteValue val2)
	{
		ConcreteValue pathVal1 = resolveAbsolutePath(val1);
		ConcreteValue pathVal2 = resolveAbsolutePath(val2);
		return ConstantStringValue.isPossibleMatched(pathVal1, pathVal2);
	}
	@Override
	public int hashCode()
	{
		return mPath.hashCode() * 53;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof FileValue))
			return false;
		FileValue that = (FileValue)other;
		return mPath.equals(that.mPath);
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[FILE path=");
		builder.append(mPath);
		builder.append("]");
		return builder.toString();
	}
	@Override
	public ConcreteValue getStringValue()
	{
		return mPath;
	}
	
	/**
	 * It argument should be a file value or string value.
	 * For each possible value of {@code path}, it is null, then preserve the value. Otherwise,
	 * append '/' if theren't already one.
	 * @param path
	 * @return a path value without trailing slash
	 */
	public static ConcreteValue makePathWithoutTrailingSlash(ConcreteValue path)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(path);
		OrValue result = new OrValue();
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof NullValue)
				result.addValue(singleVal);
			else
			{
				// TODO Do better
				ConcatValue concatVal = new ConcatValue(singleVal.getStringValue());
				ConcatValue transformVal = new ConcatValue();
				Iterator<ConcreteValue> concatItr = concatVal.iterator();
				while(concatItr.hasNext())
				{
					ConcreteValue concatEle = concatItr.next();
					
					// This element isn't the last one
					if(concatItr.hasNext())
					{
						transformVal.addValue(concatEle);
						continue;
					}
					if(!(concatEle instanceof ConstantStringValue))
					{
						transformVal.addValue(concatEle);
						continue;
					}
					ConstantStringValue strEle = (ConstantStringValue)concatEle;
					String str = strEle.getValue();
					if(str.endsWith("/"))
						transformVal.addValue(new ConstantStringValue(str.substring(0, str.length() - 1)));
					else
						transformVal.addValue(concatEle);
				}
				result.addValue(transformVal.simplify());
			}
		}
		return result.simplify();
	}
	public static ConcreteValue makeAbsolutePath(ConcreteValue path, boolean ignoreEmpty)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(path);
		OrValue result = new OrValue();
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof NullValue)
				result.addValue(singleVal);
			else
			{
				singleVal = singleVal.getStringValue();
				ConcatValue concatVal = new ConcatValue(singleVal);
				if(concatVal.isEmpty())
				{
					if(ignoreEmpty)
						result.addValue(singleVal);
					else
						result.addValue(new ConstantStringValue("/"));
					continue;
				}				
				ConcreteValue fstEle = concatVal.getFirst();
				if(ignoreEmpty && concatVal.size() == 1 && fstEle instanceof UnknownValue)
				{
					result.addValue(singleVal);
					continue;
				}
				if(fstEle instanceof ConstantStringValue)
				{
					String str = ((ConstantStringValue)fstEle).getValue();
					if(str.startsWith("/"))
						result.addValue(singleVal);
					else
						result.addValue(new ConcatValue(new ConstantStringValue("/"), singleVal).simplify());
				}
				else
					result.addValue(new ConcatValue(new ConstantStringValue("/"), singleVal).simplify());
			}
		}
		return result.simplify();
	}
}
