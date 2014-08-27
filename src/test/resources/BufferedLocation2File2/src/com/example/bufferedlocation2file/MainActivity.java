package com.example.bufferedlocation2file;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import com.example.bufferedlocation2file2.R;

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
			String filename = "myfile2";
			FileOutputStream outputStream = new FileOutputStream(filename);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(outputStream)));
			writer.write(loc.toString());
			writer.close();
		}
		catch (Exception e) 
		{
		  e.printStackTrace();
		}
	}
}

