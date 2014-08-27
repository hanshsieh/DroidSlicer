package com.test.urlconntofile;

import android.os.Bundle;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View.OnKeyListener;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
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

import com.test.leakviaurlconnection.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
	private static final String savefilepath = "/sdcard/uc/";
	TextView tv;
	private final static String FILE_NAME1 = "output1.txt";
	private final static String FILE_NAME2 = "output2.txt";
	private final static String FILE_NAME3 = "output3.txt";
	private String googleWeatherUrl1 = "http://www.google.com/ig/api?weather=zhengzhou";
	private String googleWeatherUrl2 = "http://www.google.com/ig/api?hl=zh-cn&weather=zhengzhou";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Button b2 = (Button) findViewById(R.id.Button02);

		tv = (TextView) findViewById(R.id.TextView02);
		b2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				httpClientConn();
				String content = tv.getText().toString();
				writeToFile(FILE_NAME2, content);
			}
		});
		/*
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
				writeToFile(FILE_NAME3, content);
			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
			
		});*/
		//downloadAPK();
		//downloadAPK2();
		//downloadAPK3();
	}
	private void downloadAPK()
	{
		DefaultHttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost("www.example.com/apk1");
        request.addHeader("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        request.addHeader("Accept-Language", "zh-CN, en-US");
        request.addHeader("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
        try {
            HttpResponse response = client.execute(((HttpUriRequest)request));
            if(response.getStatusLine().getStatusCode() != 200)
            {
                return;
            }

            InputStream input = response.getEntity().getContent();
            FileOutputStream fileOutput = new FileOutputStream(new File(savefilepath + "myupdate.apk"));
            byte[] buf = new byte[1024];
            while(true) {
                int nRead = input.read(buf);
                if(nRead == -1) {
                    break;
                }

                fileOutput.write(buf, 0, nRead);
            }

            input.close();
            fileOutput.close();
            client.getConnectionManager().shutdown();
        }
        catch(IOException ex) 
        {
            ex.printStackTrace();
        }
	}
	private void downloadAPK2()
	{
		DefaultHttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost("www.example.com/apk2");
        try {
            HttpResponse response = client.execute(((HttpUriRequest)request));
            if(response.getStatusLine().getStatusCode() != 200)
            {
                return;
            }

            FileOutputStream fileOutput = new FileOutputStream(new File(savefilepath + "myupdate2.apk"));
            response.getEntity().writeTo(fileOutput);
            fileOutput.close();
            client.getConnectionManager().shutdown();
        }
        catch(IOException ex) 
        {
            ex.printStackTrace();
        }
	}
	private void downloadAPK3()
	{
		DefaultHttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost("www.example.com/apk3");
        try {
            String response = client.execute(((HttpUriRequest)request), new ResponseHandler<String>()
            {
				@Override
				public String handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException
				{
					return String.valueOf(response.getEntity().getContent().read());
				}            	
            });
            FileOutputStream fileOutput = new FileOutputStream(new File(savefilepath + "myupdate3.apk"));
            fileOutput.write(response.getBytes());
            fileOutput.close();
            client.getConnectionManager().shutdown();
        }
        catch(IOException ex) 
        {
            ex.printStackTrace();
        }
	}
	protected void writeToFile(String filename, String content)
	{
		try
		{
			FileOutputStream output = openFileOutput(filename, Context.MODE_PRIVATE);
			Writer writer = new BufferedWriter(new OutputStreamWriter(output));
			writer.write(content);
			Toast.makeText(this, "Data has been written to file", Toast.LENGTH_SHORT)
				.show();
		}
		catch(Exception ex)
		{
			
		}
		
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

	protected FbHttpRequest buildRequest(String url)
	{
		return new FbHttpRequest(new HttpGet(url));
	}
	// Connect GoogleWeatherAPI via HttpCient
	protected void httpClientConn()
	{
		
		FbHttpRequest req = buildRequest(googleWeatherUrl2);
		sendRequest(req);
	}
	protected void sendRequest(FbHttpRequest req)
	{
		DefaultHttpClient httpclient = new DefaultHttpClient();
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		
		try {
			String content = httpclient.execute(req.getRequest(), responseHandler);
			Toast.makeText(getApplicationContext(), "Sucessfully connect to Google Weather API",
					Toast.LENGTH_SHORT).show();
			tv.setText(content);
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "Fail to connect to Google Weather API", Toast.LENGTH_SHORT)
			.show();
			e.printStackTrace();
		}
		httpclient.getConnectionManager().shutdown();
	}
}