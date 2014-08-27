package com.test.impliciticcleak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class MyBroadcastReceiver2 extends BroadcastReceiver
{
	public static final String ACTION = MyBroadcastReceiver2.class.getPackage().getName() + ".MY_RECEIVER2";
	@Override
	public void onReceive(Context context, Intent intent)
	{
		
	}

}
