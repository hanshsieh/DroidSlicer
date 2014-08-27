package com.example.receiverregister;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Contacts.People;
import android.provider.Contacts.People.Phones;
import android.telephony.SmsManager;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver
{
	private final String TAG = MyBroadcastReceiver.class.getName();
	@Override
	public void onReceive(Context context, Intent intent)
	{
		try
		{
			Log.i(TAG, "Broadcast is received");
			SmsManager smsManager = SmsManager.getDefault();
			String contact = intent.getStringExtra("contact");
			Log.i(TAG, "Receive contact: " + contact);
			smsManager.sendTextMessage("0900000000", null, contact, null, null);
			Log.i(TAG, "SMS is sent with content: " + contact);
		}
		catch(Exception ex)
		{
			Log.e(TAG, ex.toString());
		}
	}

}
