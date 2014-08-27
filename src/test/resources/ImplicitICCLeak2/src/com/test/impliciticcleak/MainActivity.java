package com.test.impliciticcleak;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class MainActivity extends Activity
{
	private final static String LOG_TAG = MainActivity.class.getName();
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		LocationManager locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener()
		{

			@Override
			public void onLocationChanged(Location loc)
			{
				Intent intent = new Intent();
				intent.setAction(MyBroadcastReceiver2.ACTION);
				Bundle bundle = new Bundle();
				String str = "" + loc.getLatitude() + ", " + loc.getLongitude();
				bundle.putString("Location", loc == null ? "Not available" : str);
				intent.putExtras(bundle);
				sendBroadcast(intent);
				Toast.makeText(MainActivity.this, "Location is sent", Toast.LENGTH_SHORT).show();
				Log.d(LOG_TAG, "Location is sent with action " + intent.getAction());
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
