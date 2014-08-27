package org.droidslicer.value;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;



public class UriEncodedValue extends ConcreteValue 
{
	private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
	private final ConcreteValue mVal;
	@Override
	public boolean equals(Object other)
	{
		if(this == other)
			return true;
		if(!(other instanceof UriEncodedValue))
			return false;
		UriEncodedValue that = (UriEncodedValue)other;
		return mVal.equals(that.mVal);
	}
	@Override
	public int hashCode()
	{
		return mVal.hashCode() * 9397;
	}
	private UriEncodedValue(ConcreteValue val)
	{
		if(val == null)
			throw new IllegalArgumentException();
		mVal = val;
	}
	public static String encodeForAndroidPath(String path)
	{
		String[] toks = path.split("/");
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for(String tok : toks)
		{
			if(first)
				first = false;
			else
				builder.append('/');
			builder.append(UriEncodedValue.encodeForAndroid(tok));
		}
		return builder.toString();
	}
	protected static ConcreteValue makeForAndroidPathSingle(ConcreteValue val, boolean ignoreNullVal)
	{
		if(ignoreNullVal && val instanceof NullValue)
			return NullValue.getInstance();
		if(val instanceof ConstantStringValue)
		{
			ConstantStringValue conStrVal = (ConstantStringValue)val;
			String path = conStrVal.getValue();
			return new ConstantStringValue(encodeForAndroidPath(path));
		}
		else
			return UnknownValue.getInstance();
		// TODO Handle more cases
	}
	public static ConcreteValue makeForAndroidPath(ConcreteValue val, boolean ignoreNullVal)
	{
		if(val == null)
			throw new IllegalArgumentException();
		if(val instanceof OrValue)
		{
			OrValue orVal = (OrValue)val;
			OrValue result = new OrValue();
			Iterator<ConcreteValue> itr = orVal.iterator();
			while(itr.hasNext())
			{
				ConcreteValue subVal = itr.next();
				result.addValue(makeForAndroidPathSingle(subVal, ignoreNullVal));
			}
			return result.simplify();
		}
		else
			return makeForAndroidPathSingle(val, ignoreNullVal);
	}
	private static ConcreteValue makeForAndroidSingle(ConcreteValue val, boolean ignoreNullVal)
	{
		if(ignoreNullVal && val instanceof NullValue)
			return NullValue.getInstance();
		if(val instanceof UriEncodedValue)
			return new UriEncodedValue(((UriEncodedValue) val).mVal);
		else if(val instanceof ConstantStringValue)
		{
			ConstantStringValue strVal = (ConstantStringValue)val;
			String str = strVal.getValue();
			
			return new ConstantStringValue(encodeForAndroid(str, null));
		}
		else
			return new UriEncodedValue(val);
	}
    private static boolean isAllowedForAndroid(char c, String allow)
    {
        return (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || "_-!.~'()*".indexOf(c) != -1
                || (allow != null && allow.indexOf(c) != -1);
    }
    public static String encodeForAndroid(String s)
    {
    	return encodeForAndroid(s, null);
    }
    public static String encodeForAndroid(String s, String allow)
    {
        if (s == null)
            return null;

        // Lazily-initialized buffers.
        StringBuilder encoded = null;

        int oldLength = s.length();

        // This loop alternates between copying over allowed characters and
        // encoding in chunks. This results in fewer method calls and
        // allocations than encoding one character at a time.
        int current = 0;
        while (current < oldLength)
        {
            // Start in "copying" mode where we copy over allowed chars.

            // Find the next character which needs to be encoded.
            int nextToEncode = current;
            while (nextToEncode < oldLength
                    && isAllowedForAndroid(s.charAt(nextToEncode), allow))
            {
                nextToEncode++;
            }

            // If there's nothing more to encode...
            if (nextToEncode == oldLength) {
                if (current == 0) {
                    // We didn't need to encode anything!
                    return s;
                } else {
                    // Presumably, we've already done some encoding.
                    encoded.append(s, current, oldLength);
                    return encoded.toString();
                }
            }

            if (encoded == null) {
                encoded = new StringBuilder();
            }

            if (nextToEncode > current) {
                // Append allowed characters leading up to this point.
                encoded.append(s, current, nextToEncode);
            } else {
                // assert nextToEncode == current
            }

            // Switch to "encoding" mode.

            // Find the next allowed character.
            current = nextToEncode;
            int nextAllowed = current + 1;
            while (nextAllowed < oldLength
                    && !isAllowedForAndroid(s.charAt(nextAllowed), allow)) {
                nextAllowed++;
            }

            // Convert the substring to bytes and encode the bytes as
            // '%'-escaped octets.
            String toEncode = s.substring(current, nextAllowed);
            try {
                byte[] bytes = toEncode.getBytes("UTF-8");
                int bytesLength = bytes.length;
                for (int i = 0; i < bytesLength; i++) {
                    encoded.append('%');
                    encoded.append(HEX_DIGITS[(bytes[i] & 0xf0) >> 4]);
                    encoded.append(HEX_DIGITS[bytes[i] & 0xf]);
                }
            } catch (UnsupportedEncodingException e)
            {
                throw new AssertionError(e);
            }

            current = nextAllowed;
        }

        // Encoded could still be null at this point if s is empty.
        return encoded == null ? s : encoded.toString();
    }
	public static ConcreteValue makeForAndroid(ConcreteValue val, boolean ignoreNullVal)
	{
		if(val instanceof OrValue)
		{
			OrValue result = new OrValue();
			Iterator<ConcreteValue> itr = result.iterator();
			while(itr.hasNext())
			{
				ConcreteValue subVal = itr.next();
				result.addValue(makeForAndroidSingle(subVal, ignoreNullVal));
			}
			return result.simplify();
		}
		else
			return makeForAndroidSingle(val, ignoreNullVal);
	}
	public ConcreteValue getValue()
	{
		return mVal;
	}
	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Maybe we should do better
		return this;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[URI_ENCODED val=");
		builder.append(mVal);
		builder.append(']');
		return builder.toString();
	}
}
