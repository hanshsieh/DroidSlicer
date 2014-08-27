package com.example.manipulatedfilename;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.location.Location;

public class MyAsyncTask extends android.os.AsyncTask<Location, Object, Location>
{
	private final Context mCtx;
	public MyAsyncTask(Context ctx)
	{
		mCtx = ctx;
	}
	public void process(Location location)
	{
		File cacheDir = mCtx.getCacheDir();
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
		mCtx.sendBroadcast(intent);
	}
	@Override
	protected void onPostExecute(Location location)
	{
		//process(location);
	}
	@Override
	protected Location doInBackground(Location... params)
	{
		Location location = params[0];
		return location;
	}
}
