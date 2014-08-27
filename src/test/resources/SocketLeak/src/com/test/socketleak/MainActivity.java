package com.test.socketleak;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity
{
	private final String LOG_TAG = MainActivity.class.getName();
	private Location mLoc = null;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		new Thread()
		{
			@Override
			public void run()
			{
				Socket sock = null;
				BufferedWriter fileWriter = null;
				try
				{
					fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("command.log", true)));
					sock = new Socket("8.8.8.8", 5566);
					BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
					String line;
					writer.write("Hey, please give me an command\n");
					while((line = reader.readLine()) != null)
					{
						fileWriter.write(line + "\n");
						if(line.equals("GPS"))
						{
							writer.write("Location: " + mLoc);
						}
						else
						{
							writer.write("Unknown command: " + line + "\n");
						}
						writer.write("\n===============\nCommand finished, give me another command\n");
						writer.flush();
					}
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
				}
				finally
				{
					if(sock != null)
					{
						try
						{
							sock.close();
						}
						catch(Exception ex)
						{}
					}
					if(fileWriter != null)
					{
						try
						{
							fileWriter.close();
						}
						catch(Exception ex)
						{}
					}
				}
			}
		}.start();
		LocationManager locMgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener()
		{

			@Override
			public void onLocationChanged(Location location)
			{
				Log.d(LOG_TAG, "Obtain location: " + location);
				mLoc = location;
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
