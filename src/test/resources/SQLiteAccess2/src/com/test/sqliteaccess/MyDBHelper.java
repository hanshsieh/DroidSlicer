package com.test.sqliteaccess;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDBHelper extends SQLiteOpenHelper
{
    final private static int _DB_VERSION = 1;
    final public static String _DB_DATABASE_NAME = "DbHelper.db";
    public MyDBHelper(Context context)
    {
    	super(context,_DB_DATABASE_NAME,null,_DB_VERSION);
    }
    public MyDBHelper(Context context, String name, CursorFactory factory, int version)
    {
    	super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL(
	        "CREATE TABLE MyTable (" +
	            "ID INTEGER PRIMARY KEY, " +
	            "DATA VARCHAR(50), " +
	            "DATETIME DATETIME NULL, " +
	            "LOCATION STRING NULL" +
		    ")"
		);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS MyTable");
        onCreate(db);
    }
}