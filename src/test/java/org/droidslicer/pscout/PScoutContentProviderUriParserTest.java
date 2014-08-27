package org.droidslicer.pscout;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PScoutContentProviderUriParserTest
{
	private Logger mLogger = LoggerFactory.getLogger(PScoutContentProviderUriParserTest.class);
	@Test
	public void test() throws Exception
	{
		String input = 
			"content://com.android.contacts/contacts/.*/photo R android.permission.GLOBAL_SEARCH pathPattern\n" +
			"content://com.android.contacts.* grant-uri-permission pathPattern\n" +
			"content://com.android.contacts R android.permission.READ_CONTACTS\n" +
			"content://com.android.contacts/search_suggest_query R android.permission.GLOBAL_SEARCH pathPrefix\n" +
			"content://com.android.contacts/search_suggest_shortcut R android.permission.GLOBAL_SEARCH pathPrefix\n" +
			"content://com.android.bluetooth.opp/btopp W android.permission.ACCESS_BLUETOOTH_SHARE path";
		PScoutContentProviderUriParser parser = new PScoutContentProviderUriParser(input);
		PScoutContentProviderUri data = null;
		while((data = parser.read()) != null)
		{
			mLogger.debug(data.toString());
		}
	}

}
