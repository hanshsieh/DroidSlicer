package com.example.manipulatedfilename;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;

public class MyReceiver extends BroadcastReceiver
{
	public static final String ACTION = "MY_ACTION";
	public static final String CATEGORY = "MY_CATEGROY";
	private Context mCtx;
	@Override
	public void onReceive(Context context, Intent intent)
	{
		mCtx = context;
		noleak1(context);
	}
	public File getOutputFile()
	{
		String fileName = String.valueOf(new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()))
                + ".mov";
		File dir = new File(String.valueOf(PreferenceManager.getDefaultSharedPreferences(mCtx.getApplicationContext())
               .getString("File", "")) + File.separator + "Videos");
		dir.mkdirs();
		return new File(dir, fileName);
	}
	public void noleak1(Context context)
	{
		File file = getOutputFile();
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
