package com.test.basiclocation2file;

import java.io.FileOutputStream;

import com.example.basiclocation2file.R;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		LocationManager locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Location loc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		String filename = "myfile";
		FileOutputStream outputStream;
		try
		{
			  outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
			  outputStream.write(loc.toString().getBytes());
			  outputStream.close();
		} catch (Exception e) {
		  e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
