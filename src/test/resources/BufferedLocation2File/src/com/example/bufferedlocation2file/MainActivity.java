package com.example.bufferedlocation2file;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		LocationManager locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Location loc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		try
		{
			String filename = "myfile1";
			FileOutputStream outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
			BufferedWriter writer1 = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(outputStream)));
			writer1.write(loc.toString());
			writer1.close();
		}
		catch (Exception e)
		{
		  e.printStackTrace();
		}
	}
}

