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

public class MyBroadcastReceiver2 extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
	
	}

}
