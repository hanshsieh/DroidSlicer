package com.example.contentprovideraccess;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity
{
	private final static String LOG_TAG = MainActivity.class.getName();
	private String mAuth;
	@Override
	protected void onResume()
	{
		try
		{
			LocationManager locMgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	
			locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 10, new LocationListener()
			{
				@Override
				public void onLocationChanged(Location location)
				{
					ContentResolver resolver = getContentResolver();
					
					// Insert the location into the db
					ContentValues vals = new ContentValues();
					{
						vals.put("lat", location.getLatitude());
						vals.put("lng", location.getLongitude());
					}
					
					// Send the location data to other content provider
					{
						Uri.Builder builder = new Uri.Builder();
						builder.scheme("content")
							.authority(MyContentProvider2.AUTHORITY)
							.appendPath("MyTable1");
						Uri uri = builder.build();
						resolver.insert(uri, vals);
					}
					
					{
						Uri uri = Uri.parse("content://details").buildUpon().appendQueryParameter("id", "2222").build();
						Log.d(LOG_TAG, uri.toString());
						resolver.insert(uri, vals);
					}
					{
						Uri uri = Uri.parse("content://" + mAuth).buildUpon().appendQueryParameter("id", "3333").build();
						Log.d(LOG_TAG, uri.toString());
						resolver.insert(uri, vals);
					}
					
					// Query the data back again
					/*{
						Uri.Builder builder = new Uri.Builder();
						builder.scheme("content");
						//builder.authority(MyContentProvider.AUTHORITY); // debug
						builder.appendPath("MyTable1");
						Uri uri = builder.build();
						Log.d(LOG_TAG, uri.toString());
						Cursor cursor = resolver.query(uri, new String[]{"lat", "lng"}, null, null, null);
						try
						{
							while(cursor.moveToNext())
							{
								double lat = cursor.getDouble(cursor.getColumnIndex("lat"));
								double lng = cursor.getDouble(cursor.getColumnIndex("lng"));
								Log.d(LOG_TAG, "lat: " + lat + ", lng: " + lng);
								SmsManager smsManager = SmsManager.getDefault();
								smsManager.sendTextMessage("0900000000", null, "lat: " + lat + ", lng: " + lng, null, null);
							}
						}
						finally
						{
							try
							{
								cursor.close();
							}
							catch(Exception ex)
							{}
						}
					}*/
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
		catch(Exception ex)
		{
			Log.d(LOG_TAG, "exception: " + ex.getMessage());
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mAuth = MyContentProvider.AUTHORITY;
	}
}
