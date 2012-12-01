package com.jobmineplus.mobile.database.users;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "usertable.db";
    private static final int DATABASE_VERSION = 1;

    public UserDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method is called during create of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        UserTable.onCreate(database);
    }

    public void dropTable(SQLiteDatabase database) {
        UserTable.dropTable(database);
    }

    // Method is called during an upgrade of the database
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion,
            int newVersion) {
        UserTable.onUpgrade(database, oldVersion, newVersion);
    }
}
