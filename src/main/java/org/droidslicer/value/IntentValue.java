package org.droidslicer.value;

import java.util.Iterator;
import java.util.Locale;

import com.ibm.wala.types.TypeName;

public class IntentValue extends ConcreteValue
{
	private final static int HASH_UNDEFINED = -1;
	private int mHash = HASH_UNDEFINED;
	private ConcreteValue mIntentAction = NullValue.getInstance();
	private ConcreteValue mIntentDataType = NullValue.getInstance();
	private ConcreteValue mIntentCompName = NullValue.getInstance();
	private ConcreteValue mIntentUri = NullValue.getInstance();
	private final OrValue mIntentCategories = new OrValue();
	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Maybe we should do better
		return this;
	}
	public ConcreteValue getPossibleUriSchemes()
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> valItr = OrValue.getSingleValueIterator(mIntentUri);
		while(valItr.hasNext())
		{
			ConcreteValue valSingle = valItr.next();
			if(valSingle instanceof UriValue)
			{
				UriValue uri = (UriValue)valSingle;
				switch(uri.getMode())
				{
				case HIERARCHICAL_SERVER_BASED:
				case HIERARCHICAL_REGISTRY_BASED:
				case OPAQUE:
					{
						ConcreteValue schemeVal = uri.getScheme();
						result.addValue(schemeVal);
					}
				default:
					result.addValue(UnknownValue.getInstance());
				}
			}
			else if(valSingle instanceof NullValue)
			{
				result.addValue(NullValue.getInstance());
			}
			else
				result.addValue(UnknownValue.getInstance());
		}
		return result.simplify();
	}
	public static boolean isPossibleExpclicitMatch(ConcreteValue val, TypeName oTypeName)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof UnknownValue)
			{
				// TODO To reduce false-positive, we use conservative approach
				continue;
			}
			else if(singleVal instanceof IntentValue)
			{
				IntentValue intentVal = (IntentValue)singleVal;
				ConcreteValue compNameVal = intentVal.getIntentComponentName();
				compNameVal = NullValue.excludeNullValue(compNameVal);
				if(ComponentNameValue.isPossibleTypeNameMatch(compNameVal, oTypeName))
					return true;
			}
		}
		return false;
	}
	public static boolean isPossibleExplicit(ConcreteValue val)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof IntentValue)
			{
				IntentValue intentVal = (IntentValue)singleVal;
				ConcreteValue intentCompName = intentVal.getIntentComponentName();
				if(NullValue.isPossibleNotNull(intentCompName))
					return true;
			}
			else
				return true;
		}
		return false;
	}
	public static boolean isPossibleImplicit(ConcreteValue val)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof IntentValue)
			{
				IntentValue intentVal = (IntentValue)singleVal;
				ConcreteValue compNameVal = intentVal.getIntentComponentName();
				if(NullValue.isPossibleNull(compNameVal))
					return true;
			}
			else
				return true;
		}
		return false;
	}
	public ConcreteValue resolveDataTypeIfNeeded()
	{
		OrValue result = new OrValue();
		if(isPossibleExplicit(this))
			result.addValue(mIntentDataType);
		if(isPossibleImplicit(this))
		{
			Iterator<ConcreteValue> typeItr = OrValue.getSingleValueIterator(mIntentDataType);
			
			// For each possible value of type 
			while(typeItr.hasNext())
			{
				ConcreteValue typeSingle = typeItr.next();
				
				// If the type is possibly null
				if(NullValue.isPossibleNull(typeSingle))
				{
					
					// If the scheme of the URI is possibly null
					Iterator<ConcreteValue> schemeItr = OrValue.getSingleValueIterator(getPossibleUriSchemes());
					while(schemeItr.hasNext())
					{
						ConcreteValue schemeSingle = schemeItr.next();
						if(schemeSingle instanceof ConstantStringValue)
						{
							String schemeStr = ((ConstantStringValue)schemeSingle).getValue();
							if(schemeStr.equals("content"))
							{
								// Resolve the type from data
								// Find the corresponding ContentProvider, and invoke its getType() to
								// obtain the type
								// TODO Resolve the type from the getType() of possible target ContentProvider
								result.addValue(UnknownValue.getInstance());
							}
							else
								result.addValue(NullValue.getInstance());
						}
						else if(schemeSingle instanceof NullValue)
							result.addValue(NullValue.getInstance());
						else
						{
							// TODO Maybe we should do better for this case
							result.addValue(UnknownValue.getInstance());
						}
					}
				}
				
				// If the type is possibly not null
				if(NullValue.isPossibleNotNull(typeSingle))
				{
					// Directly use the type as the result
					result.addValue(typeSingle);
				}
			}
		}
		return result.simplify();			
	}
	public ConcreteValue getIntentAction()
	{
		return mIntentAction;
	}
	public void setIntentAction(ConcreteValue action)
	{
		if(action == null)
			throw new IllegalArgumentException();
		mIntentAction = action;
		mHash = HASH_UNDEFINED;
	}
	public ConcreteValue getIntentDataType()
	{
		return mIntentDataType;
	}
	public void setIntentDataType(ConcreteValue val)
	{
		if(val == null)
			throw new IllegalArgumentException();
		mIntentDataType = val;
		mHash = HASH_UNDEFINED;
	}
	public OrValue getIntentCategories()
	{
		return mIntentCategories;
	}
	public void addIntentCategory(ConcreteValue cat)
	{
		mIntentCategories.addValue(cat);
		mHash = HASH_UNDEFINED;
	}
	public ConcreteValue getIntentComponentName()
	{
		return mIntentCompName;
	}
	public void setIntentComponentName(ConcreteValue intentCompName)
	{
		if(intentCompName == null)
			throw new IllegalArgumentException();
		mIntentCompName = intentCompName;
		mHash = HASH_UNDEFINED;
	}
	public ConcreteValue getIntentUri()
	{
		return mIntentUri;
	}
	public void setIntentUri(ConcreteValue val)
	{
		if(val == null)
			throw new IllegalArgumentException();
		mIntentUri = val;
		mHash = HASH_UNDEFINED;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof IntentValue))
			return false;
		IntentValue that = (IntentValue)other;
		if(mHash != HASH_UNDEFINED && that.mHash != HASH_UNDEFINED)
		{
			if(mHash != that.mHash)
				return false;
		}
		return mIntentAction.equals(that.mIntentAction) && 
				mIntentDataType.equals(that.mIntentDataType) && 
				mIntentCompName.equals(that.mIntentCompName) &&
				mIntentUri.equals(that.mIntentUri) && 
				mIntentCategories.equals(that.mIntentCategories);
	}
	@Override
	public int hashCode()
	{
		if(mHash == HASH_UNDEFINED)
		{
			mHash = mIntentAction.hashCode();
			mHash = mHash * 31 + mIntentDataType.hashCode();
			mHash = mHash * 31 + mIntentCompName.hashCode();
			mHash = mHash * 31 + mIntentUri.hashCode();
			mHash = mHash * 31 + mIntentCategories.hashCode();
			if(mHash == HASH_UNDEFINED)
				mHash = 52249;
		}
		return mHash;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[INTENT comp=");
		builder.append(mIntentCompName);
		builder.append(" action=");
		builder.append(mIntentAction);
		builder.append(" dataType=");
		builder.append(mIntentDataType);
		builder.append(" uri=");
		builder.append(mIntentUri);
		builder.append(" categories=");
		builder.append(mIntentCategories);
		builder.append(']');
		return builder.toString();
	}
	/**
	 * It corresponds to method android.content.Intent#normalizeMimeType(String).
	 * @return the normalized MIME type for Android
	 */
	public static ConcreteValue normalizeMimeTypeForAndroid(ConcreteValue val)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue valSingle = itr.next();
			if(valSingle instanceof ConstantStringValue)
			{
				ConstantStringValue strVal = (ConstantStringValue)valSingle;
				String str = strVal.getValue();
				str = str.trim().toLowerCase(Locale.ROOT);
				int semicolonIndex = str.indexOf(';');
				if(semicolonIndex != -1)
					str = str.substring(0, semicolonIndex);
				result.addValue(new ConstantStringValue(str));
			}
			else if(valSingle instanceof NullValue)
			{
				result.addValue(NullValue.getInstance());
			}
			else
			{
				// TODO Maybe we can do better
				result.addValue(UnknownValue.getInstance());
			}
		}
		return result.simplify();
	}
}
