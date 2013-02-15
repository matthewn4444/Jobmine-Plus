package com.jobmineplus.mobile.database.users;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class UserTable {
    public static final String TAG = UserTable.class.getName();
    public static final String TABLE_USER = "user";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_LAST_USER = "last_user";

    private static final String DATABASE_CREATE = "create table "
            + TABLE_USER
            + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_USERNAME + " text not null, "
            + COLUMN_PASSWORD + " text not null, "
            + COLUMN_LAST_USER + " integer default 0 "
            + ");";
    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    public static void dropTable(SQLiteDatabase database) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
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
