package com.example.manipulatedfilename;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity
{
	private final static String LOG_TAG = MainActivity.class.getName();
	private final ExecutorService mExecutorPool = Executors.newFixedThreadPool(5);
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		LocationManager locMgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener()
		{

			@Override
			public void onLocationChanged(final Location location)
			{
				
				Log.d(LOG_TAG, "Obtain location: " + location);
				mExecutorPool.execute(new Runnable()
				{

					@Override
					public void run()
					{
						try
						{
							MyAsyncTask task = new MyAsyncTask(MainActivity.this);
							task.execute(location);
							//task.process(task.get());
						}
						catch(Exception ex)
						{
							ex.printStackTrace();
						}
					}
				});	
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
