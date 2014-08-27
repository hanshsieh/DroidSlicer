package com.example.interactivityleak;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.view.Menu;

public class Activity1 extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main1);
		LocationManager locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Location loc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Intent intent = new Intent(this, Activity2.class);
		Bundle bundle = new Bundle();
		bundle.putString("Location", loc == null ? "Not available" : loc.toString());
		intent.putExtras(bundle);
		startActivity(intent);
	}
}
