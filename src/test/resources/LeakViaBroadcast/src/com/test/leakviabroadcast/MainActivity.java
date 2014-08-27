package com.test.leakviabroadcast;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity
{
	private final static String LOG_TAG = MainActivity.class.getName();
	private final static String ACTION = "MyAction";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		LocationManager locMgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		// Request location update using listener
		locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, 10, new LocationListener()
		{
			@Override
			public void onLocationChanged(Location location)
			{
				// Send an implicit broadcast
				{
					Intent intent = new Intent();
					Bundle bundle = new Bundle();
					bundle.putParcelable(LocationManager.KEY_LOCATION_CHANGED, location);
					bundle.putString("type", getResources().getString(R.string.implicit));
					intent.putExtras(bundle).setAction(ACTION);
					MainActivity.this.sendBroadcast(intent);
				}
				
				// Send an explicit broadcast
				/*
				{
					Intent intent = new Intent();
					intent.setClass(MainActivity.this, MyBroadcastReceiver.class);
					Bundle bundle = new Bundle();
					bundle.putParcelable(LocationManager.KEY_LOCATION_CHANGED, location);
					bundle.putString("type", getResources().getString(R.string.explicit));
					intent.putExtras(bundle);
					MainActivity.this.sendBroadcast(intent);
				}*/
				/*
				{
					Intent intent = new Intent();
					intent.setClassName(MainActivity.this, "com.test.leakviabroadcast.MyBroadcastReceiver");
					Bundle bundle = new Bundle();
					bundle.putParcelable(LocationManager.KEY_LOCATION_CHANGED, location);
					bundle.putString("type", getResources().getString(R.string.explicit));
					intent.putExtras(bundle);
					MainActivity.this.sendBroadcast(intent);
				}*/
				/*
				{
					Intent intent = new Intent();
					intent.setComponent(new ComponentName(MainActivity.this, "com.test.leakviabroadcast.MyBroadcastReceiver"));
					Bundle bundle = new Bundle();
					bundle.putParcelable(LocationManager.KEY_LOCATION_CHANGED, location);
					bundle.putString("type", getResources().getString(R.string.explicit));
					intent.putExtras(bundle);
					MainActivity.this.sendBroadcast(intent);
				}*/
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras)
			{
				
			}

			@Override
			public void onProviderEnabled(String provider)
			{
				
			}

			@Override
			public void onProviderDisabled(String provider)
			{
				
			}
			
		});
		// Request location update using PendingIntent
		/*
		Intent intent = new Intent(this, MyBroadcastReceiver.class);
		Bundle bundle = new Bundle();
		bundle.putString("type", getResources().getString(R.string.pending_intent));
		intent.putExtras(bundle);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 10, pendingIntent);
		
		// Dynamically register a BroadcastReceiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION);
		registerReceiver(new BroadcastReceiver()
		{
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
		}, filter);*/
	}
}
