package com.test.sqliteaccess;

import java.io.File;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity
{

	private void insertLocationToDb(Location loc)
	{
        MyDBHelper dbHelper = new MyDBHelper(this);
        // Insert by raw SQL
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        SQLiteStatement stm = db.compileStatement("INSERT INTO MyTable (DATA, DATETIME, LOCATION) VALUES (?, datetime('now'), ?)");
        stm.bindString(1, "Hello world");
        stm.bindString(2, loc.getLatitude() + "," + loc.getLongitude());
        stm.executeInsert();
        db.close();
    }
	private SQLiteDatabase insertLocationToMemDb(Location loc)
	{
    	SQLiteDatabase db = SQLiteDatabase.create(null);
    	db.execSQL(
    	        "CREATE TABLE MyTable (" +
    	            "ID INTEGER PRIMARY KEY, " +
    	            "DATA VARCHAR(50), " +
    	            "DATETIME DATETIME NULL, " +
    	            "LOCATION STRING NULL" +
    		    ")"
    		);
        ContentValues values = new ContentValues();
        values.put("DATA","YoHO");
        values.put("DATETIME","2012-05-09 20:30:00");
        values.put("LOCATION", loc.getLatitude() + "," + loc.getLongitude());
        db.insertOrThrow("MyTable", null, values);
        return db;
    }
	private String queryLocationFromDb(SQLiteDatabase db)
	{
        // Query by raw SQL
    	Cursor cursor = null;
    	try
    	{
	        cursor = db.rawQuery("SELECT DATA, DATETIME, LOCATION FROM MyTable", null);
	        cursor.moveToFirst();
	        while(!cursor.isAfterLast())
	        {
	        	String locStr = cursor.getString(2);
	            Log.e("SQLiteDBTestingActivity", "DATA = "+cursor.getString(0));
	            Log.e("SQLiteDBTestingActivity", "DATETIME = "+cursor.getString(1));
	            Log.e("SQLiteDBTestingActivity", "LOCATION = "+locStr);
	            return locStr;
	        }
	        return "NOT FOUND";
    	}
    	finally
    	{
    		if(cursor != null)
    			cursor.close();
    		if(db != null)
    			db.close();
    	}
    }
	public void sendToSMS(String str)
	{
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage("0900000000", null, str, null, null);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        LocationManager locMgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Location loc = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        insertLocationToDb(loc);
        //SQLiteDatabase memDb = insertLocationToMemDb(loc);
        sendToSMS(queryLocationFromDb(SQLiteDatabase.openOrCreateDatabase(getDatabasePath(MyDBHelper._DB_DATABASE_NAME), null)));
        //sendToSMS(queryLocationFromDb(memDb));
        //sendToSMS(queryLocationFromDb(openOrCreateDatabase(MyDBHelper._DB_DATABASE_NAME, MODE_PRIVATE, null)));
	}
}
