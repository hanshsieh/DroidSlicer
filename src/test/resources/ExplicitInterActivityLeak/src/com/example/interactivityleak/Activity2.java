package com.example.interactivityleak;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.telephony.SmsManager;
import android.view.Menu;
import android.widget.TextView;

public class Activity2 extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main2);
	}
	@Override
	protected void onResume()
	{
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		String data = bundle.getString("Location");
		TextView textView = (TextView)findViewById(R.id.data);
		textView.setText("Data: " + data);
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage("0900000000", null, data, null, null);
	}
}
