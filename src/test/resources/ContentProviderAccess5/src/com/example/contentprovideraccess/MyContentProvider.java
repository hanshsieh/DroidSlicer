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

	@Override
	public boolean onCreate()
	{
		return true;
	}

	@Override
	public String getType(Uri uri)
	{
		return "";
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		StringBuilder builder = new StringBuilder();
		for(String str : projection)
		{
			builder.append(str);
		}
		return new MyCursor(builder.toString());
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
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