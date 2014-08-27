package org.droidslicer.value;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

public class UriValue extends ConcreteValue
{
	public enum Mode
	{
		// Reference: http://docs.oracle.com/javase/7/docs/api/java/net/URI.html
		HIERARCHICAL_SERVER_BASED, // [<scheme>:][//[<user-info>@]<host>[:<port>]][<path>][?<query>][#<fragment>] 
		HIERARCHICAL_REGISTRY_BASED, // [<scheme>:][//<authority>][<path>][?<query>][#<fragment>] 
		OPAQUE, // [<scheme>:]<scheme-specific-part>[#<fragment>]
		WHOLE_URI // <uri>
	}
	private static final int MAX_RESOLVE_SIZE = 50;
	private static final int MAX_DEPTH_AUTH_FROM_URI = 6;
	private static final int MAX_DEPTH_AUTH_FROM_SSP = 4;
	private int mHash = -1;
	private final Mode mMode;
	private ConcreteValue mScheme;
	private ConcreteValue mUserInfo;
	private ConcreteValue mHost;
	private ConcreteValue mPort;
	private ConcreteValue mAuthority;
	private ConcreteValue mPath;
	private ConcreteValue mQuery;
	private ConcreteValue mSsp;
	private ConcreteValue mFragment;
	private ConcreteValue mUri;
	
	public UriValue(
			ConcreteValue scheme, 
			ConcreteValue userInfo,
			ConcreteValue host,
			ConcreteValue port,
			ConcreteValue path, 
			ConcreteValue query, 
			ConcreteValue fragment)
	{
		mMode = Mode.HIERARCHICAL_SERVER_BASED;
		mScheme = scheme == null ? NullValue.getInstance() : scheme;
		mUserInfo = userInfo == null ? NullValue.getInstance() : userInfo;
		mHost = host == null ? NullValue.getInstance() : host;
		mPort = port == null ? new IntValue(-1) : port;
		mPath = path == null ? NullValue.getInstance() : path;
		mQuery = query == null ? NullValue.getInstance() : query;
		mFragment = fragment == null ? NullValue.getInstance() : fragment;
		
		// If scheme, userInfo, host, or port is defined, then
		// we should prepend '/' to the path if the path is defined and not empty.
		if(NullValue.isImpossibleNull(mScheme) || 
			NullValue.isImpossibleNull(mUserInfo) ||
			NullValue.isImpossibleNull(mHost) || 
			IntValue.isImpossibleNegative(mPort))
		{
			mPath = FileValue.makeAbsolutePath(mPath, true);
		}
		else
		{
			// TODO Do it better
			mPath = new ConcatValue(UnknownValue.getInstance(), mPath).simplify();
		}
	}
	
	public UriValue(
			ConcreteValue scheme, 
			ConcreteValue authority, 
			ConcreteValue path, 
			ConcreteValue query, 
			ConcreteValue fragment)
	{
		mMode = Mode.HIERARCHICAL_REGISTRY_BASED;
		mScheme = scheme == null ? NullValue.getInstance() : scheme;
		mAuthority = authority == null ? NullValue.getInstance() : authority;
		mPath = path == null ? NullValue.getInstance() : path;
		mQuery = query == null ? NullValue.getInstance() : query;
		mFragment = fragment == null ? NullValue.getInstance() : fragment;

		// If scheme, or authority is defined, then
		// we should prepend '/' to the path if the path is defined and not empty.
		if(NullValue.isImpossibleNull(mScheme) || 
			NullValue.isImpossibleNull(mAuthority))
		{
			mPath = FileValue.makeAbsolutePath(mPath, true);
		}
		else
		{
			// TODO Do it better
			mPath = new ConcatValue(UnknownValue.getInstance(), mPath).simplify();
		}
	}
	public UriValue(
			ConcreteValue scheme, 
			ConcreteValue ssp, 
			ConcreteValue fragment)
	{
		mMode = Mode.OPAQUE;
		mScheme = scheme == null ? NullValue.getInstance() : scheme;
		mSsp = ssp == null ? NullValue.getInstance() : ssp;
		mFragment = fragment == null ? NullValue.getInstance() : fragment;
	}
	public UriValue(
			ConcreteValue uriStrVal)
	{
		if(uriStrVal == null)
			throw new IllegalArgumentException();
		if(uriStrVal instanceof ConstantStringValue)
		{
			String uriStr = ((ConstantStringValue) uriStrVal).getValue();
			URI uri = null;
			try
			{
				uri = new URI(uriStr);
			}
			catch(URISyntaxException ex)
			{}
			if(uri != null)
			{
				String scheme = uri.getScheme();
				String fragment = uri.getRawFragment();
				if(uri.isOpaque())
				{
					mMode = Mode.OPAQUE;
					mScheme = scheme == null ? NullValue.getInstance() : new ConstantStringValue(scheme);
					mSsp = new ConstantStringValue(uri.getSchemeSpecificPart());
					mFragment = fragment == null ? NullValue.getInstance() : new ConstantStringValue(fragment);
				}
				else if(uri.getHost() == null)
				{
					mMode = Mode.HIERARCHICAL_REGISTRY_BASED;
					String authority = uri.getRawAuthority();
					String path = uri.getRawPath();
					String query = uri.getRawQuery();
					mScheme = scheme == null ? NullValue.getInstance() : new ConstantStringValue(scheme);
					mAuthority = authority == null ? NullValue.getInstance() : new ConstantStringValue(authority);
					mPath = path == null ? NullValue.getInstance() : new ConstantStringValue(path);
					mQuery = query == null ? NullValue.getInstance() : new ConstantStringValue(query);
					mFragment = fragment == null ? NullValue.getInstance() : new ConstantStringValue(fragment);
				}
				else
				{
					mMode = Mode.HIERARCHICAL_SERVER_BASED;
					String userInfo = uri.getUserInfo();
					String host = uri.getHost();
					int port = uri.getPort();
					String path = uri.getRawPath();
					String query = uri.getRawQuery();
					mScheme = scheme == null ? NullValue.getInstance() : new ConstantStringValue(scheme);
					mUserInfo = userInfo == null ? NullValue.getInstance() : new ConstantStringValue(userInfo);
					mHost = host == null ? NullValue.getInstance() : new ConstantStringValue(host);
					mPort = port < 0 ? new IntValue(-1) : new IntValue(port);
					mPath = path == null ? NullValue.getInstance() : new ConstantStringValue(path);
					mQuery = query == null ? NullValue.getInstance() : new ConstantStringValue(query);
					mFragment = fragment == null ? NullValue.getInstance() : new ConstantStringValue(fragment);
				}
				return;
			}
		}
		mMode = Mode.WHOLE_URI;
		mUri = uriStrVal;
	}
	public Mode getMode()
	{
		return mMode;
	}
	public static ConcreteValue fromFile(ConcreteValue file)
	{
		if(file == null)
			throw new IllegalArgumentException("File cannot be null");
		ConcreteValue scheme = new ConstantStringValue("file");
		ConcreteValue path = FileValue.resolvePath(file);
		return new UriValue(scheme, null, path, null, null);
	}
	private static ConcreteValue getPathWithAppendedEncodedSingle(ConcreteValue oriPath, ConcreteValue newPath)
	{
		if(oriPath == null || newPath == null)
			throw new IllegalArgumentException();

		// If the original path doesn't end with '/', then a '/' should be inserted in between.
		if(oriPath instanceof NullValue)
			return new ConcatValue(new ConstantStringValue("/"), newPath);
		ConcreteValue lastSeg;
		if(oriPath instanceof ConcatValue)
		{
			ConcatValue conPathVal = (ConcatValue)oriPath;
			if(conPathVal.isEmpty())
				return new ConcatValue(new ConstantStringValue("/"), newPath);
			lastSeg = conPathVal.getLast();
		}
		else
			lastSeg = oriPath;
		if(lastSeg instanceof ConstantStringValue)
		{
			ConstantStringValue lastSegStrVal = (ConstantStringValue)lastSeg;
			String lastSegStr = lastSegStrVal.getValue();
			
			if(lastSegStr.endsWith("/"))
				return new ConcatValue(oriPath, newPath);
			else
				return new ConcatValue(oriPath, new ConstantStringValue("/"), newPath);
		}
		else
			return new ConcatValue(oriPath, UnknownValue.getInstance(), newPath);
	}
	public static ConcreteValue makePathWithAppendedEncoded(ConcreteValue oriPath, ConcreteValue newPath)
	{
		if(oriPath instanceof OrValue)
		{
			OrValue orVal = (OrValue)oriPath;
			Iterator<ConcreteValue> itr = orVal.iterator();
			OrValue result = new OrValue();
			while(itr.hasNext())
			{
				ConcreteValue orOriPathVal = itr.next();
				result.addValue(getPathWithAppendedEncodedSingle(orOriPathVal, newPath));
			}
			return result.simplify();
		}
		else
			return getPathWithAppendedEncodedSingle(oriPath, newPath);
	}
	private static ConcreteValue withAppendedEncodedPathSingle(ConcreteValue baseUriVal, ConcreteValue path)
	{
		if(baseUriVal instanceof UnknownValue)
		{
			return UnknownValue.getInstance();
		}
		if(baseUriVal instanceof UriValue)
		{
			UriValue baseUri = (UriValue)baseUriVal;
			switch(baseUri.getMode())
			{
			case OPAQUE:
				{
					// The original ssp will be removed, and the URI will be turned into
					// a hierarchical URI with no authority, and the path is the given path.
					// Because after the ssp is removed, there's no path part, thus the original 
					// path part doesn't end with '/', thus '/' is always prepended to the new path
					// even if the new path already begins with a '/'.
					ConcreteValue scheme = baseUri.getScheme();
					ConcreteValue newPath = new ConcatValue(new ConstantStringValue("/"), path);
					return new UriValue(scheme, null, newPath, null, null);
				}
			case HIERARCHICAL_REGISTRY_BASED:
				{
					ConcreteValue oriPath = baseUri.getPath();
					ConcreteValue newPath = makePathWithAppendedEncoded(oriPath, path);
					return new UriValue(
							baseUri.getScheme(), 
							baseUri.getAuthority(), 
							newPath, 
							baseUri.getQuery(), 
							baseUri.getFragment());
				}
			case HIERARCHICAL_SERVER_BASED:
				{
					ConcreteValue oriPath = baseUri.getPath();
					ConcreteValue newPath = makePathWithAppendedEncoded(oriPath, path);
					return new UriValue(
							baseUri.getScheme(), 
							baseUri.getUserInfo(),
							baseUri.getHost(),
							baseUri.getPort(),
							newPath, 
							baseUri.getQuery(), 
							baseUri.getFragment());
				}
			case WHOLE_URI:
				return UnknownValue.getInstance();
			default:
				throw new RuntimeException("Unreachable");
			}
		}
		else
			return UnknownValue.getInstance();
	}
	public static ConcreteValue withAppendedEncodedPath(ConcreteValue baseUriVal, ConcreteValue path)
	{
		if(baseUriVal instanceof OrValue)
		{
			OrValue orVal = (OrValue)baseUriVal;
			OrValue result = new OrValue();
			Iterator<ConcreteValue> itr = orVal.iterator();
			while(itr.hasNext())
			{
				ConcreteValue val = itr.next();
				result.addValue(withAppendedEncodedPathSingle(val, path));
			}
			return result.simplify();
		}
		else
		{
			return withAppendedEncodedPathSingle(baseUriVal, path);
		}
	}
	public ConcreteValue getScheme()
	{
		return mScheme == null ? UnknownValue.getInstance() : mScheme;
	}
	public ConcreteValue getUserInfo()
	{
		return mUserInfo == null ? UnknownValue.getInstance() : mUserInfo;
	}
	public ConcreteValue getHost()
	{
		return mHost == null ? UnknownValue.getInstance() : mHost;
	}
	public ConcreteValue getPort()
	{
		return mPort == null ? UnknownValue.getInstance() : mPort;
	}
	public ConcreteValue getAuthority()
	{
		return mAuthority == null ? UnknownValue.getInstance() : mAuthority;
	}
	public ConcreteValue getPath()
	{
		return mPath == null ? UnknownValue.getInstance() : mPath;
	}
	public ConcreteValue getFragment()
	{
		return mFragment == null ? UnknownValue.getInstance() : mFragment;
	}
	public ConcreteValue getQuery()
	{
		return mQuery == null ? UnknownValue.getInstance() : mQuery;
	}
	public ConcreteValue getUri()
	{
		return mUri == null ? UnknownValue.getInstance() : mUri;
	}
	public ConcreteValue getSchemeSpecificPart()
	{
		if(mSsp != null)
			return mSsp;
		if(mAuthority == null || mPath == null || mQuery == null)
			return UnknownValue.getInstance();
		ConcatValue result = new ConcatValue();
		if(!(mAuthority instanceof NullValue))
		{
			result.addValue(new ConstantStringValue("//"));
			result.addValue(mAuthority);
		}
		if(!(mPath instanceof NullValue))
			result.addValue(mPath);
		if(!(mQuery instanceof NullValue))
		{
			result.addValue(new ConstantStringValue("?"));
			result.addValue(mQuery);
		}
		if(result.isEmpty())
			return ConstantStringValue.getEmptyString();
		else
			return result;
	}
	protected void appendSchemeString(ConcatValue result)
	{
		OrValue schemeVal = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(mScheme);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof UnknownValue)
			{
				schemeVal.addValue(singleVal);
			}
			else if(!(singleVal instanceof NullValue))
			{
				ConcatValue val = new ConcatValue(singleVal.getStringValue(), new ConstantStringValue(":"));
				schemeVal.addValue(val.simplify());
			}
		}
		if(!schemeVal.isEmpty())
			result.addValue(schemeVal.simplify());
	}
	protected void appendAuthorityString(ConcatValue result)
	{
		OrValue authVal = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(mAuthority);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			ConcatValue newAuth = new ConcatValue();
			if(singleVal instanceof UnknownValue)
				newAuth.addValue(singleVal);
			else if(!(singleVal instanceof NullValue))
			{
				newAuth.addValue(new ConstantStringValue("//"));
				newAuth.addValue(singleVal.getStringValue());
			}
			authVal.addValue(newAuth);
		}
		if(!authVal.isEmpty())
			result.addValue(authVal.simplify());
	}
	protected void appendPathString(ConcatValue result)
	{
		OrValue pathVal = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(mPath);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(!(singleVal instanceof NullValue))
			{
				pathVal.addValue(singleVal.getStringValue());
			}
		}
		if(!pathVal.isEmpty())
			result.addValue(pathVal.simplify());
	}
	protected void appendQueryString(ConcatValue result)
	{
		OrValue queryVal = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(mQuery);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof UnknownValue)
			{
				queryVal.addValue(singleVal);
			}
			else if(!(singleVal instanceof NullValue))
			{
				ConcatValue val = new ConcatValue(new ConstantStringValue("?"), singleVal.getStringValue());
				queryVal.addValue(val.simplify());
			}
		}
		if(!queryVal.isEmpty())
			result.addValue(queryVal.simplify());
	}
	protected void appendSspString(ConcatValue result)
	{
		OrValue sspVal = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(mSsp);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(!(singleVal instanceof NullValue))
			{
				sspVal.addValue(singleVal.getStringValue());
			}
		}
		if(!sspVal.isEmpty())
			result.addValue(sspVal.simplify());
	}
	protected void appendFragmentString(ConcatValue result)
	{
		OrValue fragVal = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(mFragment);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof UnknownValue)
			{
				fragVal.addValue(singleVal);
			}
			else if(!(singleVal instanceof NullValue))
			{
				ConcatValue val = new ConcatValue(new ConstantStringValue("#"), singleVal.getStringValue());
				fragVal.addValue(val.simplify());
			}
		}
		if(!fragVal.isEmpty())
			result.addValue(fragVal.simplify());
	}
	/**
	 * Convert the Uri value into its string representation.
	 * TODO Maybe we can do it better.
	 */
	@Override
	public ConcreteValue getStringValue()
	{
		switch(mMode)
		{
		case OPAQUE:
			{
				// Reference http://docs.oracle.com/javase/7/docs/api/java/net/URI.html#URI%28java.lang.String,%20java.lang.String,%20java.lang.String%29
				ConcatValue result = new ConcatValue();
				assert mScheme != null && mSsp != null && mFragment != null;
				appendSchemeString(result);
				appendSspString(result);
				appendFragmentString(result);
				return result.simplify();
			}
		case HIERARCHICAL_REGISTRY_BASED:
			{
				// Reference http://docs.oracle.com/javase/7/docs/api/java/net/URI.html#URI%28java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String%29
				// TODO The way the string is constructed here isn't correct. 
				// E.g. If authority is OrValue, and it has two possibilities, null or some string, then
				// in the 1st case, '//' shouldn't be prepended, but it should be in the 2nd case.
				ConcatValue result = new ConcatValue();
				assert mScheme != null && mAuthority != null && mPath != null && 
						mQuery != null && mFragment != null;
				
				appendSchemeString(result);			
				appendAuthorityString(result);
				appendPathString(result);
				appendQueryString(result);
				appendFragmentString(result);
				return result.simplify();
			}
		case HIERARCHICAL_SERVER_BASED:
			{
				ConcatValue result = new ConcatValue();
				assert mScheme != null && mUserInfo != null && mHost != null &&
						mPort != null && mPath != null && mQuery != null && mFragment != null;
				
				appendSchemeString(result);
				result.addValue(resolveAuthorityForServerBased(true));
				appendPathString(result);
				appendQueryString(result);
				appendFragmentString(result);
				return result.simplify();
			}
		case WHOLE_URI:
			return mUri.getStringValue();
		default:
			throw new RuntimeException();
		}
	}
	@Override
	public String toString()
	{
		ConcreteValue strVal = getStringValue();
		return strVal.toString();
	}
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof UriValue))
			return false;
		UriValue that = (UriValue)other;
		if(mMode != that.mMode)
			return false;
		switch(mMode)
		{
		case OPAQUE:
			assert that.mScheme != null && that.mSsp != null && that.mFragment != null;
			return mScheme.equals(that.mScheme) && mSsp.equals(that.mSsp) && mFragment.equals(that.mFragment);
		case HIERARCHICAL_REGISTRY_BASED:
			assert that.mScheme != null && that.mAuthority != null && that.mPath != null && that.mQuery != null && that.mFragment != null;
			return mScheme.equals(that.mScheme) && mAuthority.equals(that.mAuthority) &&
					mPath.equals(that.mPath) && mQuery.equals(that.mQuery) && mFragment.equals(that.mFragment);
		case HIERARCHICAL_SERVER_BASED:
			assert that.mScheme != null && that.mUserInfo != null && that.mHost != null && that.mPort != null 
				&& that.mPath != null && that.mQuery != null && that.mFragment != null;
			return mScheme.equals(that.mScheme) && mUserInfo.equals(that.mUserInfo) && mHost.equals(that.mHost) && 
					mPort.equals(that.mPort) && mPath.equals(that.mPath) && mQuery.equals(that.mQuery) && mFragment.equals(that.mFragment);
		case WHOLE_URI:
			assert that.mUri != null;
			return mUri.equals(that.mUri);
		default:
			throw new RuntimeException();
		}
	}
	public static ConcreteValue getPossibleSchemes(ConcreteValue uriVal)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(uriVal);
		while(itr.hasNext())
		{
			ConcreteValue valSingle = itr.next();
			if(valSingle instanceof UriValue)
			{
				UriValue uriSingle = (UriValue)valSingle;
				switch(uriSingle.getMode())
				{
				case HIERARCHICAL_SERVER_BASED:
				case HIERARCHICAL_REGISTRY_BASED:
				case OPAQUE:
					result.addValue(uriSingle.getScheme());
					break;
				default:
					result.addValue(UnknownValue.getInstance());
					break;
				}
			}
			else
				result.addValue(UnknownValue.getInstance());
		}
		return result.simplify();
	}
	public static ConcreteValue normalizeSchemeForAndroid(ConcreteValue val)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue valSingle = itr.next();
			if(valSingle instanceof UriValue)
			{
				UriValue uriVal = (UriValue)valSingle;
				switch(uriVal.getMode())
				{
				case HIERARCHICAL_SERVER_BASED:
					result.addValue(
							new UriValue(ConstantStringValue.toLowerCase(uriVal.getScheme()), 
							uriVal.getUserInfo(), 
							uriVal.getHost(), 
							uriVal.getPort(), 
							uriVal.getPath(), 
							uriVal.getQuery(), 
							uriVal.getFragment()));
					break;
				case HIERARCHICAL_REGISTRY_BASED:
					result.addValue(
							new UriValue(
								ConstantStringValue.toLowerCase(uriVal.getScheme()), 
								uriVal.getAuthority(), 
								uriVal.getPath(), 
								uriVal.getQuery(), 
								uriVal.getFragment()));
					break;
				case OPAQUE:
					result.addValue(
							new UriValue(
								ConstantStringValue.toLowerCase(uriVal.getScheme()), 
								uriVal.getSchemeSpecificPart(), 
								uriVal.getFragment()));
					break;
				default:
					// TODO Maybe we should do better for whole URI case
					result.addValue(UnknownValue.getInstance());
					break;
				}
			}
			else
				result.addValue(UnknownValue.getInstance());
		}
		return result.simplify();
	}
	@Override
	public int hashCode()
	{
		if(mHash != -1)
			return mHash;
		int hash = 0;
		switch(mMode)
		{
		case OPAQUE:
			hash = mScheme.hashCode();
			hash = hash * 31 + mSsp.hashCode();
			hash = hash * 31 + mFragment.hashCode();
			break;
		case HIERARCHICAL_REGISTRY_BASED:
			hash = mScheme.hashCode();
			hash = hash * 31 + mAuthority.hashCode();
			hash = hash * 31 + mPath.hashCode();
			hash = hash * 31 + mQuery.hashCode();
			hash = hash * 31 + mFragment.hashCode();
			break;
		case HIERARCHICAL_SERVER_BASED:
			hash = mScheme.hashCode();
			hash = hash * 31 + mUserInfo.hashCode();
			hash = hash * 31 + mHost.hashCode();
			hash = hash * 31 + mPort.hashCode();			
			hash = hash * 31 + mPath.hashCode();
			hash = hash * 31 + mQuery.hashCode();
			hash = hash * 31 + mFragment.hashCode();
			break;
		case WHOLE_URI:
			hash = mUri.hashCode() * 3187;
			break;
		default:
			throw new RuntimeException();
		}
		mHash = hash == -1 ? 0 : hash;
		return mHash;
	}
	protected static void resolveAuthorityFromWholeUriStr(final OrValue result, ConcreteValue val)
	{
		ConcatValue.walkPrefix(val, new ConcatValue.ConcatEntryWalker()
		{
			private int mCount = 0;
			@Override
			public boolean shouldWalk(ConcreteValue val, int depth)
			{
				if(depth > MAX_DEPTH_AUTH_FROM_URI)
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
			@Override
			public boolean visit(ConcreteValue val)
			{
				++mCount;
				if(val instanceof ConcatValue)
				{
					val = ((ConcatValue)val).simplify();
				}
				if(val instanceof ConstantStringValue || val instanceof ConcatValue)
				{
					boolean isPrefix;
					String str;
					if(val instanceof ConcatValue)
					{
						ConcatValue concatVal = (ConcatValue)val;
						assert concatVal.size() > 1;
						isPrefix = true;
						ConcreteValue fstVal = concatVal.getFirst();
						if(fstVal instanceof ConstantStringValue)
						{
							ConstantStringValue fstStrVal = (ConstantStringValue)fstVal;
							str = fstStrVal.getValue();
						}
						else
							str = "";
					}
					else
					{
						isPrefix = false;
						str = ((ConstantStringValue)val).getValue();
					}
					int idx1 = str.indexOf("//");
					if(idx1 < 0)
					{
						if(isPrefix)
							result.addValue(UnknownValue.getInstance());
						else
							result.addValue(NullValue.getInstance());
						return mCount < MAX_RESOLVE_SIZE;
					}
					int idx2 = str.indexOf('/', idx1 + 2);
					if(idx2 < 0)
					{
						if(isPrefix)
						{
							result.addValue(new ConcatValue(new ConstantStringValue(str.substring(idx1 + 2)), UnknownValue.getInstance()).simplify());
							return mCount < MAX_RESOLVE_SIZE;
						}
						idx2 = str.length();
					}
					result.addValue(new ConstantStringValue(str.substring(idx1 + 2, idx2)));
				}
				else
				{
					result.addValue(UnknownValue.getInstance());
				}
				return mCount < MAX_RESOLVE_SIZE;
			}			
		});
	}
	protected static void resolveAuthorityFromSspStr(final OrValue result, ConcreteValue ssp)
	{
		ConcatValue.walkPrefix(ssp, new ConcatValue.ConcatEntryWalker()
		{
			private int mCount = 0;
			@Override
			public boolean shouldWalk(ConcreteValue val, int depth)
			{
				if(depth > MAX_DEPTH_AUTH_FROM_SSP)
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
			@Override
			public boolean visit(ConcreteValue val)
			{
				++mCount;
				if(val instanceof ConcatValue)
					val = ((ConcatValue)val).simplify();
				if(val instanceof ConstantStringValue || val instanceof ConcatValue)
				{
					boolean isPrefix;
					String str;
					if(val instanceof ConcatValue)
					{
						ConcatValue concatVal = (ConcatValue)val;
						assert concatVal.size() > 1;
						ConcreteValue fstVal = concatVal.getFirst();
						ConstantStringValue fstStrVal = (ConstantStringValue)fstVal;
						str = fstStrVal.getValue();
						isPrefix = true;
					}
					else
					{
						isPrefix = false;
						str = ((ConstantStringValue)val).getValue();
					}
					if(!str.startsWith("//"))
					{
						if(isPrefix)
						{
							if(str.length() >= 2)
								result.addValue(NullValue.getInstance());		
							else
								result.addValue(UnknownValue.getInstance());
						}
						else
							result.addValue(NullValue.getInstance());
						return mCount < MAX_RESOLVE_SIZE;
					}
					int idx = str.indexOf('/', 2);
					if(idx < 0)
					{
						if(isPrefix)
						{
							result.addValue(new ConcatValue(new ConstantStringValue(str.substring(2)), UnknownValue.getInstance()));
							return true;
						}
						idx = str.length();
					}
					result.addValue(new ConstantStringValue(str.substring(2, idx)));
				}
				else
					result.addValue(UnknownValue.getInstance());
				return mCount < MAX_RESOLVE_SIZE;
			}
		});
	}
	protected static void resolvePathFromSspStr(OrValue result, ConcreteValue ssp)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(ssp);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof ConstantStringValue || singleVal instanceof ConcatValue)
			{
				boolean isPrefix;
				String str;
				if(singleVal instanceof ConcatValue)
				{
					ConcatValue concatVal = (ConcatValue)singleVal;
					isPrefix = concatVal.size() > 1;
					ConcreteValue fstVal = concatVal.getFirst();
					ConstantStringValue fstStrVal = (ConstantStringValue)fstVal;
					str = fstStrVal.getValue();
				}
				else
				{
					isPrefix = false;
					str = ((ConstantStringValue)singleVal).getValue();
				}
				if(!str.startsWith("//"))
				{
					if(isPrefix)
					{
						if(str.length() >= 2)
							result.addValue(NullValue.getInstance());
						else
							result.addValue(UnknownValue.getInstance());
					}
					else
						result.addValue(NullValue.getInstance());
					continue;
				}
				int slashIdx = str.indexOf('/', 2);
				if(slashIdx < 0)
				{
					if(isPrefix)
						result.addValue(UnknownValue.getInstance());
					else
						result.addValue(ConstantStringValue.getEmptyString());
					continue;
				}
				int qIdx = str.indexOf('?', slashIdx + 1);
				if(qIdx < 0)
				{
					if(isPrefix)
					{
						result.addValue(new ConcatValue(new ConstantStringValue(str.substring(slashIdx)), UnknownValue.getInstance()));
						continue;
					}
					qIdx = str.length();
				}
				result.addValue(new ConstantStringValue(str.substring(slashIdx, qIdx)));
			}
			else
				result.addValue(UnknownValue.getInstance());
		}
	}
	protected static void resolvePathFromWholeUriStr(OrValue result, ConcreteValue uri)
	{
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(uri);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof ConstantStringValue || singleVal instanceof ConcatValue)
			{
				if(singleVal instanceof ConcatValue)
					singleVal = ((ConcatValue)singleVal).simplify();
				boolean isPrefix;
				String str;
				if(singleVal instanceof ConcatValue)
				{
					ConcatValue concatVal = (ConcatValue)singleVal;
					assert concatVal.size() > 1;
					isPrefix = true;
					ConcreteValue fstVal = concatVal.getFirst();
					if(fstVal instanceof ConstantStringValue)
					{
						ConstantStringValue fstStrVal = (ConstantStringValue)fstVal;
						str = fstStrVal.getValue();
					}
					else
						str = "";
				}
				else
				{
					isPrefix = false;
					str = ((ConstantStringValue)singleVal).getValue();
				}
				int dslashIdx = str.indexOf("//");
				if(dslashIdx < 0)
				{
					if(isPrefix)
						result.addValue(UnknownValue.getInstance());
					else
						result.addValue(ConstantStringValue.getEmptyString());
					continue;
				}
				int slashIdx = str.indexOf('/', dslashIdx + 2);
				if(slashIdx < 0)
				{
					if(isPrefix)
						result.addValue(UnknownValue.getInstance());
					else
						result.addValue(ConstantStringValue.getEmptyString());
					continue;
				}
				int qIdx = str.indexOf('?', slashIdx + 1);
				if(qIdx >= 0)
				{
					result.addValue(new ConstantStringValue(str.substring(slashIdx, qIdx)));
					continue;
				}
				int fragIdx = str.indexOf('#', slashIdx + 1);
				if(fragIdx < 0)
				{
					if(isPrefix)
						result.addValue(new ConcatValue(new ConstantStringValue(str.substring(slashIdx)), UnknownValue.getInstance()));
					else
						result.addValue(new ConstantStringValue(str.substring(slashIdx)));		
				}
				else
					result.addValue(new ConstantStringValue(str.substring(slashIdx, fragIdx)));
			}
			else
				result.addValue(UnknownValue.getInstance());
		}
	}
	
	/**
	 * Resolve that path of the URI.
	 * For URI like http://www.examle.com/dir/test, it should return /dir/test.
	 * @param val
	 * @return the resolved path of the URI
	 */
	public static ConcreteValue resolvePath(ConcreteValue val)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof UriValue)
			{
				UriValue uriVal = (UriValue)singleVal;
				switch(uriVal.getMode())
				{
				case HIERARCHICAL_SERVER_BASED:
				case HIERARCHICAL_REGISTRY_BASED:
					result.addValue(uriVal.getPath());
					break;
				case OPAQUE:
					{
						ConcreteValue ssp = uriVal.getSchemeSpecificPart();
						resolvePathFromSspStr(result, ssp);
						break;
					}
				case WHOLE_URI:
					{
						ConcreteValue wholeUriVal = uriVal.getUri();
						resolvePathFromWholeUriStr(result, wholeUriVal);
						break;
					}
				default:
					throw new RuntimeException("Unknown mode of URI value: " + uriVal.getMode());
				}
			}
			else
				result.addValue(UnknownValue.getInstance());
		}
		return result.simplify();
	}
	protected ConcreteValue resolveAuthorityForServerBased(boolean withLeadingSlash)
	{
		if(!mMode.equals(Mode.HIERARCHICAL_SERVER_BASED))
			throw new IllegalStateException();
		ConcatValue result = new ConcatValue();
		
		// user-info, host, or port are possibly defined
		if(NullValue.isPossibleNotNull(mUserInfo) || NullValue.isPossibleNotNull(mHost) || IntValue.isPossibleNotNegative(mPort))
		{
			if(withLeadingSlash)
				result.addValue(new ConstantStringValue("//"));
			{
				OrValue userInfoVal = new OrValue();
				Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(mUserInfo);
				while(itr.hasNext())
				{
					ConcreteValue singleVal = itr.next();
					if(!(singleVal instanceof NullValue))
					{
						ConcatValue val = new ConcatValue(singleVal.getStringValue(), new ConstantStringValue("@"));
						userInfoVal.addValue(val.simplify());
					}
				}
				if(NullValue.isPossibleNull(mUserInfo))
					userInfoVal.addValue(ConstantStringValue.getEmptyString());
				if(!userInfoVal.isEmpty())
					result.addValue(userInfoVal.simplify());
			}
			
			{
				OrValue hostVal = new OrValue();
				Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(mHost);
				while(itr.hasNext())
				{
					ConcreteValue singleVal = itr.next();
					if(!(singleVal instanceof NullValue))
					{
						hostVal.addValue(singleVal.getStringValue());
					}
				}
				if(NullValue.isPossibleNull(mHost))
					hostVal.addValue(ConstantStringValue.getEmptyString());
				if(!hostVal.isEmpty())
					result.addValue(hostVal.simplify());
			}
			
			{
				OrValue portVal = new OrValue();
				Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(mPort);
				while(itr.hasNext())
				{
					ConcreteValue singleVal = itr.next();
					if(singleVal instanceof IntValue)
					{
						int port = ((IntValue)singleVal).getValue();
						if(port < 0)
							portVal.addValue(ConstantStringValue.getEmptyString());
						else
						{
							result.addValue(new ConstantStringValue(":"));
							result.addValue(new ConstantStringValue(Integer.toString(port)));	
						}
					}
				}
				if(IntValue.isPossibleNegative(mPort))
					portVal.addValue(ConstantStringValue.getEmptyString());
				if(!portVal.isEmpty())
					result.addValue(portVal.simplify());
			}
		}
		return result.simplify();
	}
	public static ConcreteValue resolveAuthority(ConcreteValue val)
	{
		OrValue result = new OrValue();
		Iterator<ConcreteValue> itr = OrValue.getSingleValueIterator(val);
		while(itr.hasNext())
		{
			ConcreteValue singleVal = itr.next();
			if(singleVal instanceof UriValue)
			{
				UriValue uriVal = (UriValue)singleVal;
				switch(uriVal.getMode())
				{
				case HIERARCHICAL_SERVER_BASED:
					result.addValue(uriVal.resolveAuthorityForServerBased(false));
					break;
				case HIERARCHICAL_REGISTRY_BASED:
					result.addValue(uriVal.getAuthority());
					break;
				case OPAQUE:
					{
						ConcreteValue ssp = uriVal.getSchemeSpecificPart();
						resolveAuthorityFromSspStr(result, ssp);
						break;
					}
				case WHOLE_URI:
					{
						ConcreteValue wholeUriVal = uriVal.getUri();
						resolveAuthorityFromWholeUriStr(result, wholeUriVal);
						break;
					}
				default:
					throw new RuntimeException("Unknown mode of URI value: " + uriVal.getMode());
				}
			}
			else
			{
				result.addValue(UnknownValue.getInstance());
			}
		}
		return result.simplify();
	}
}
