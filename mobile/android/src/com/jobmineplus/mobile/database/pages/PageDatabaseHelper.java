package com.jobmineplus.mobile.database.pages;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PageDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "pagetable.db";
    private static final int DATABASE_VERSION = 1;

    public PageDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method is called during create of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        PageTable.onCreate(database);
    }

    public void dropTable(SQLiteDatabase database) {
        PageTable.dropTable(database);
    }

    // Method is called during an upgrade of the database
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion,
            int newVersion) {
        PageTable.onUpgrade(database, oldVersion, newVersion);
    }
}
