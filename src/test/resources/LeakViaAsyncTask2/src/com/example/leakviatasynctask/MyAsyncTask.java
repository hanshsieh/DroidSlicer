package com.example.leakviatasynctask;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.preference.PreferenceManager;

public class MyAsyncTask extends android.os.AsyncTask<String, Object, String>
{
	public MyAsyncTask()
	{
	}
	@Override
	protected String doInBackground(String... params)
	{
		String str = params[0];
		return str;
	}
}
