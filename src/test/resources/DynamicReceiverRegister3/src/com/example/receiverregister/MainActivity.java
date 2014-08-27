package com.example.receiverregister;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.provider.Contacts.People.Phones;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity
{
	private final String TAG = MainActivity.class.getName();
	private final static String ACTION = MainActivity.class.getPackage().getName() + ".MY_BROADCAST_RECEIVER";
	private BroadcastReceiver mReceiver;
	private IntentFilter mFilter;
	private void buildIntentFilter()
	{
		mFilter = new IntentFilter();
		mFilter.addAction("Hello");
		mFilter.addAction(ACTION);
		mFilter.addAction("World");
		mFilter.addCategory("HI");
	}
	private IntentFilter buildIntentFilter2()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION);
		return filter;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState)
	
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		buildIntentFilter();
		mReceiver = new MyBroadcastReceiver();
		registerReceiver(mReceiver, mFilter);
		registerReceiver(new MyBroadcastReceiver2(), buildIntentFilter2());
		Log.i(TAG, "Receiver is registered with action: " + ACTION);
	}
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if(mReceiver != null)
			unregisterReceiver(mReceiver);
	}
	@Override
	protected void onResume()
	{
		super.onResume();
		new Thread()
		{
			public void run()
			{
				try
				{
					//Uri phonesUri = Uri.withAppendedPath(People.CONTENT_URI, People.Phones.CONTENT_DIRECTORY);
					String[] proj = new String[] {ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME};
					ContentResolver contentResolver = getContentResolver();
					Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, proj, null, null, null);
					StringBuilder builder = new StringBuilder();
					builder.append("Contacts: ");
					while(cursor.moveToNext())
					{
						String id = cursor.getString(0);
						String name = cursor.getString(1);
						builder.append("id=");
						builder.append(id);
						builder.append(", name=");
						builder.append(name);
					}
					Log.i(TAG, "Contact: " + builder.toString());
					Intent intent = new Intent(ACTION);
					intent.putExtra("contact", builder.toString());
					sendBroadcast(intent);
					Log.i(TAG, "Broadcast is sent");
				}
				catch(Exception ex)
				{
					Log.e(TAG, ex.toString());
				}
			}
		}.start();
		
	}
}
