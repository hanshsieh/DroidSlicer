package org.droidslicer.value;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.util.collections.Pair;

public class IntentFilterValue extends ConcreteValue
{
	private final static int HASH_UNDEFINED = -1;
	public static final int PATTERN_LITERAL = 0;
	public static final int PATTERN_PREFIX = 1;
	public static final int PATTERN_SIMPLE_GLOB = 2;
	private int mHash = HASH_UNDEFINED;
	private final OrValue mActions = new OrValue();
	private final OrValue mCategories = new OrValue();
	private final OrValue mDataAuthorities = new OrValue();
	private final Set<Pair<ConcreteValue, ConcreteValue>> mDataPaths = new HashSet<Pair<ConcreteValue, ConcreteValue>>();
	private final OrValue mDataSchemes = new OrValue();
	private final Set<Pair<ConcreteValue, ConcreteValue>> mDataSsps = new HashSet<Pair<ConcreteValue, ConcreteValue>>();
	private final OrValue mDataMimeTypes = new OrValue();
	public void addAction(ConcreteValue val)
	{
		mActions.addValue(val);
		mHash = HASH_UNDEFINED;
	}
	public void addCategory(ConcreteValue val)
	{
		mCategories.addValue(val);
		mHash = HASH_UNDEFINED;
	}
	public void addDataAuthority(ConcreteValue val)
	{
		mDataAuthorities.addValue(val);
		mHash = HASH_UNDEFINED;
	}
	public void addDataPath(ConcreteValue path, ConcreteValue type)
	{
		mDataPaths.add(Pair.make(path, type));
		mHash = HASH_UNDEFINED;
	}
	public void addDataScheme(ConcreteValue val)
	{
		mDataSchemes.addValue(val);
		mHash = HASH_UNDEFINED;
	}
	public void addDataSchemeSpecificPart(ConcreteValue ssp, ConcreteValue type)
	{
		mDataSsps.add(Pair.make(ssp, type));
		mHash = HASH_UNDEFINED;
	}
	public void addDataMimeType(ConcreteValue val)
	{
		mDataMimeTypes.addValue(val);
		mHash = HASH_UNDEFINED;
	}
	public OrValue getActions()
	{
		return mActions;
	}
	public OrValue getCategories()
	{
		return mCategories;
	}
	public OrValue getDataAuthorities()
	{
		return mDataAuthorities;
	}
	public Collection<Pair<ConcreteValue, ConcreteValue>> getDataPaths()
	{
		return mDataPaths;
	}
	public OrValue getDataSchemes()
	{
		return mDataSchemes;
	}
	public Collection<Pair<ConcreteValue, ConcreteValue>> getSchemeSpecificParts()
	{
		return mDataSsps;
	}
	public OrValue getDataMimeTypes()
	{
		return mDataMimeTypes;
	}
	
	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Maybe we can do better
		return this;
	}
	/**
	 * See android.content.IntentFilter#match(String, String, String, android.net.Uri, java.util.Set, String)
	 * @param intentAction
	 * @return whether the action of the Intent is possibly matched with this intent value
	 */
	public boolean isActionPossibleMatched(ConcreteValue intentAction)
	{
		// TODO To reduce false-positive, we use conservative approach
		intentAction = UnknownValue.excludeUnknownValue(intentAction);
		if(intentAction instanceof UnknownValue)
			return false;
		// If no action is specified in the Intent, then pass the action test
		if(NullValue.isPossibleNull(intentAction))
			return true;
		
		// Otherwise, the filter must specify a same action as the Intent's
		Iterator<ConcreteValue> filterActionsItr = mActions.iterator();
		while(filterActionsItr.hasNext())
		{
			ConcreteValue filterActionSingle = filterActionsItr.next();
			if(ConstantStringValue.isPossibleMatched(intentAction, filterActionSingle))
				return true;
		}
		return false;
	}
	private static boolean isPossibleEmpty(OrValue val)
	{
		return val.isEmpty() || val.contains(UnknownValue.getInstance());
	}
	private static boolean isPossibleNonEmpty(OrValue val)
	{
		return !val.isEmpty();
	}
	/**
	 * See android.content.IntentFilter#matchData(String, String, android.net.Uri)
	 * @param uriVal
	 * @param typeVal
	 * @return whether the data of the Intent is possibly matched
	 */
	public boolean isDataPossibleMatched(ConcreteValue uriVal, ConcreteValue typeVal)
	{
		if(isPossibleEmpty(mDataSchemes) && isPossibleEmpty(mDataMimeTypes))
		{
			if(NullValue.isImpossibleNull(uriVal) || NullValue.isImpossibleNull(typeVal))
				return false;
		}
		ConcreteValue schemeVals = UriValue.getPossibleSchemes(uriVal);
		if(isPossibleNonEmpty(mDataSchemes))
		{
			// Match scheme
			{
				Iterator<ConcreteValue> schemeItr = OrValue.getSingleValueIterator(schemeVals);
				boolean schemePass = false;
				while(schemeItr.hasNext())
				{
					ConcreteValue schemeSingle = schemeItr.next();
					if(schemeSingle instanceof NullValue)
					{
						if(ConstantStringValue.isPossibleMatched(mDataSchemes, ConstantStringValue.getEmptyString()))
						{
							schemePass = true;
							break;
						}
					}
					else
					{
						if(ConstantStringValue.isPossibleMatched(mDataSchemes, schemeSingle))
						{
							schemePass = true;
							break;
						}
					}
				}
				if(!schemePass)
					return false;
			}
			// TODO Match ssp, authority, path
		}
		if(isPossibleEmpty(mDataSchemes))
		{
			if(NullValue.isImpossibleNull(schemeVals))
			{
				OrValue defaultScheme = new OrValue();
				defaultScheme.addValue(new ConstantStringValue(""));
				defaultScheme.addValue(new ConstantStringValue("content"));
				defaultScheme.addValue(new ConstantStringValue("file"));
				if(!ConstantStringValue.isPossibleMatched(defaultScheme, schemeVals))
					return false;
			}
		}
		if(isPossibleNonEmpty(mDataMimeTypes))
		{
			if(typeVal instanceof NullValue)
				return false;
			// TODO Handle the MIME type matching
		}
		if(isPossibleEmpty(mDataMimeTypes))
		{
			if(NullValue.isImpossibleNull(typeVal))
				return false;
		}
		return true;
	}
	
	public boolean isCategoriesPossibleMatched(OrValue categories)
	{
		if(isPossibleEmpty(categories))
			return true;
		return ConstantStringValue.isPossibleMatched(mCategories, categories);
	}
	protected boolean isPossibleMatchedSingle(IntentValue intentVal)
	{
		ConcreteValue intentAction = intentVal.getIntentAction();
		
		// Match action
		if(!isActionPossibleMatched(intentAction))
			return false;
		ConcreteValue uriVal = intentVal.getIntentUri();
		ConcreteValue typeVal = intentVal.resolveDataTypeIfNeeded();
		
		// Match data
		if(!isDataPossibleMatched(uriVal, typeVal))
			return false;
		
		// Match categories
		OrValue categories = intentVal.getIntentCategories();
		if(!isCategoriesPossibleMatched(categories))
			return false;
		return true;
	}
	/**
	 * See android.content.IntentFilter#match(String, String, String, android.net.Uri, java.util.Set, String)
	 * @param intentFilterVal the intent filter value
	 * @param intentVal the intent value
	 * @return whether the intent value possibly matches the intent filter value
	 */
	public static boolean isPossibleMatched(ConcreteValue intentFilterVal, ConcreteValue intentVal)
	{
		Iterator<ConcreteValue> intentFilterItr = OrValue.getSingleValueIterator(intentFilterVal);
		while(intentFilterItr.hasNext())
		{
			ConcreteValue sVal1 = intentFilterItr.next();
			if(sVal1 instanceof IntentFilterValue)
			{
				IntentFilterValue sFilterVal = (IntentFilterValue)sVal1;
				
				Iterator<ConcreteValue> intentValItr = OrValue.getSingleValueIterator(intentVal);
				while(intentValItr.hasNext())
				{
					ConcreteValue sVal2 = intentValItr.next();
					if(sVal2 instanceof IntentValue)
					{
						IntentValue sIntentVal = (IntentValue)sVal2;
						if(sFilterVal.isPossibleMatchedSingle(sIntentVal))
							return true;
					}
					// TODO To reduce false-positive, we ignore unknown value
				}				
			}
			// TODO To reduce false-positive, we ignore unknown value
		}
		return false;
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof IntentFilterValue))
			return false;
		IntentFilterValue that = (IntentFilterValue)other;
		if(mHash != HASH_UNDEFINED && that.mHash != HASH_UNDEFINED)
		{
			if(mHash != that.mHash)
				return false;
		}
		if(!mActions.equals(that.mActions) ||
			!mCategories.equals(that.mCategories) ||
			!mDataAuthorities.equals(that.mDataAuthorities) ||
			!mDataSchemes.equals(that.mDataSchemes) ||
			!mDataMimeTypes.equals(that.mDataMimeTypes) || 
			!mDataPaths.equals(that.mDataPaths) ||
			!mDataSsps.equals(that.mDataSsps))
		{
			return false;
		}
		return true;
	}
	@Override
	public int hashCode() 
	{
		if(mHash == HASH_UNDEFINED)
		{
			mHash = mActions.hashCode();
			mHash = mHash * 31 + mCategories.hashCode();
			mHash = mHash * 31 + mDataAuthorities.hashCode();
			mHash = mHash * 31 + mDataSchemes.hashCode();
			mHash = mHash * 31 + mDataMimeTypes.hashCode();
			mHash = mHash * 31 + mDataPaths.hashCode();
			mHash = mHash * 31 + mDataSsps.hashCode();
			if(mHash == HASH_UNDEFINED)
				mHash = 94331;
		}
		return mHash;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[INTENT_FILTER actions=");
		builder.append(mActions);
		builder.append(", categories=");
		builder.append(mCategories);
		builder.append(", dataSchemes=");
		builder.append(mDataSchemes);
		builder.append(", dataAuthorities=");
		builder.append(mDataAuthorities);
		builder.append(", dataPaths=");
		builder.append(mDataPaths);
		builder.append(", dataSsps=");
		builder.append(mDataSsps);
		builder.append(", dataMimeTypes=");
		builder.append(mDataMimeTypes);
		builder.append("]");
		return builder.toString();
	}
}
