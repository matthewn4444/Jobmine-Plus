package com.jobmineplus.mobile.database.pages;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class PageTable {
    public static final String TAG = PageTable.class.getName();
    public static final String TABLE_PAGE = "page";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PAGENAME = "pagename";
    public static final String COLUMN_USER = "user";
    public static final String COLUMN_JOBLIST = "joblist";
    public static final String COLUMN_TIME = "timestamp";


    private static final String DATABASE_CREATE = "create table "
            + TABLE_PAGE
            + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_PAGENAME + " text not null, "
            + COLUMN_USER + " text not null, "
            + COLUMN_JOBLIST + " text, "
            + COLUMN_TIME + " integer default 0 "
            + ");";

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    public static void dropTable(SQLiteDatabase database) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
            int newVersion) {
        Log.w(TAG, "Updating database from version" + oldVersion
                + " to " + newVersion
                + ", which will destroy all old data");
        dropTable(database);
        onCreate(database);
    }

}
