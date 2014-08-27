package com.example.leakviasharedpreferences;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity
{
	private final static String LOG_TAG = MainActivity.class.getName();
	public final static String PREF_PREFIX = "PREF_DEMO_";
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		LocationManager locMgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener()
		{

			@Override
			public void onLocationChanged(Location location)
			{
				Log.d(LOG_TAG, "Obtain location: " + location);
				SharedPreferences settings = getSharedPreferences(PREF_PREFIX + "123" + Math.random(), 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("setup", true)
					.putString("location", location.toString())
					.commit();
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
				
			}
			
		});
	}
}
