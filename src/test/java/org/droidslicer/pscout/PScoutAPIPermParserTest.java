package org.droidslicer.pscout;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PScoutAPIPermParserTest
{
	private final static Logger mLogger = LoggerFactory.getLogger(PScoutAPIPermParserTest.class);
	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void test() throws Exception
	{
		String input = 
				"Permission:android.permission.CHANGE_WIFI_STATE\n" +
				"2 Callers:\n" +
				"<com.android.server.WifiService: void enforceChangePermission()> (20)\n" +
				"<com.android.server.WifiService: android.os.Messenger getWifiServiceMessenger()> (2)\n" +
				"Permission:android.permission.READ_SMS\n" +
				"1 Callers:\n" +
				"<com.android.mms.data.WorkingMessage: void access$700(com.android.mms.data.WorkingMessage,com.android.mms.data.Conversation,android.net.Uri,com.google.android.mms.pdu.PduPersister,com.android.mms.model.SlideshowModel,com.google.android.mms.pdu.SendReq)> (1)\n" +
				"Permission:android.permission.NFC\n" +
 				"0 Callers:\n" +
				"Permission:android.permission.GET_TASKS\n" +
				"2 Callers:\n" +
				"<android.net.wifi.p2p.WifiP2pService$P2pStateMachine: boolean access$5300(android.net.wifi.p2p.WifiP2pService$P2pStateMachine,android.net.wifi.p2p.WifiP2pDevice,android.net.wifi.p2p.WifiP2pConfig)> (3)\n" +
				"<android.net.wifi.p2p.WifiP2pService$P2pStateMachine: boolean access$6900(android.net.wifi.p2p.WifiP2pService$P2pStateMachine,java.lang.String)> (2)";
		PScoutAPIPermParser parser = new PScoutAPIPermParser(input);
		PScoutAPIPerm apiPerm;
		int count = 0;
		while((apiPerm = parser.read()) != null)
		{
			mLogger.debug(apiPerm.toString());
			++count;
		}
		assertEquals(count, 5);
	}

}
