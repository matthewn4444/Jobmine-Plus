package com.jobmineplus.mobile.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class JobDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "jobtable.db";
    private static final int DATABASE_VERSION = 1;

    public JobDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method is called during create of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        JobTable.onCreate(database);
    }
    
    public void dropTable(SQLiteDatabase database) {
        JobTable.dropTable(database);
    }

    // Method is called during an upgrade of the database
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion,
            int newVersion) {
        JobTable.onUpgrade(database, oldVersion, newVersion);
    }
}
