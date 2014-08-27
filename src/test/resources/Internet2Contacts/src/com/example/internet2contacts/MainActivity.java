package com.example.internet2contacts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends Activity
{
	TextView tv;
	private String googleWeatherUrl1 = "http://www.google.com/ig/api?weather=zhengzhou";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		tv = (TextView) findViewById(R.id.TextView02);
		urlConn();
		EditText editText = (EditText) findViewById(R.id.EditText01);
		editText.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) 
			{
				urlConn();
				String content = tv.getText().toString();
				writeToContacts(content);
			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
			
		});
	}
	protected void writeToContacts(String content)
	{
		ContentValues vals = new ContentValues();
		vals.put("name", content);
		getContentResolver().insert(ContactsContract.Settings.CONTENT_URI, vals);
	}
	protected void urlConn() {

		try {
			// URL
			URL url = new URL(googleWeatherUrl1);
			// HttpURLConnection
			HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();

			if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				Toast.makeText(this, "Sucessfully connect to Google Weather API",
						Toast.LENGTH_SHORT).show();
				// InputStreamReader
				BufferedReader isr = new BufferedReader(new InputStreamReader(httpconn.getInputStream(), "utf-8"));
				StringBuilder content = new StringBuilder();
				// read
				String line;
				while ((line = isr.readLine()) != null) {
					content.append(line);
				}
				isr.close();
				tv.setText(content);
			}
			//disconnect
			httpconn.disconnect();

		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "Fail to connect to Google Weather API", Toast.LENGTH_SHORT)
					.show();
			e.printStackTrace();
		}
	}

}