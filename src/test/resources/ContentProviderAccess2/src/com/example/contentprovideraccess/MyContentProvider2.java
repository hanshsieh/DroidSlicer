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


public class MyContentProvider2 extends ContentProvider
{
	public static final String AUTHORITY = "com.example.study2";
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		return null;
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

	@Override
	public String getType(Uri arg0)
	{
		return null;
	}

	@Override
	public boolean onCreate()
	{
		return false;
	}
}