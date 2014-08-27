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
				new Thread()
				{
					@Override
					public void run()
					{
						mExecutorPool.execute(new Runnable()
						{

							@Override
							public void run()
							{
								process(location);
							}
						});							
					}
				}.start();
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
	public void process(Location location)
	{
		File cacheDir = getCacheDir();
		StringBuilder builder = new StringBuilder();
		Random rand = new Random();
		builder.append("myfile_");
		if(rand.nextBoolean())
			builder.append(rand.nextInt());
		String fileName = builder.toString() + ".txt";
		File file = new File(cacheDir, fileName);
		OutputStreamWriter output = null;
		try
		{
			output = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)));
			output.write("lat: " + location.getLatitude() + ", lng: " + location.getLongitude());
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			if(output != null)
			{
				try
				{
					output.close();
				}
				catch(Exception ex)
				{}
			}
		}
		Intent intent = new Intent();
		intent.setAction(MyReceiver.ACTION);
		intent.addCategory(MyReceiver.CATEGORY);
		sendBroadcast(intent);
	}
}
