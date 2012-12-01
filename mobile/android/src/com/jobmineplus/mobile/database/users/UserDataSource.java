package com.jobmineplus.mobile.database.users;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Pair;

import com.jobmineplus.mobile.database.DataSourceBase;

public class UserDataSource extends DataSourceBase{
    // Database fields
    private UserDatabaseHelper dbHelper;

    public UserDataSource(Context context) {
        dbHelper = new UserDatabaseHelper(context);
    }

    //=========================
    //  Override Methods
    //=========================
    @Override
    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    @Override
    public void close() {
        dbHelper.close();
    }

    //=========================
    //  Insertions
    //=========================
    public synchronized long putUser(String username, String password) {
        return internalPutUser(username, password);
    }

    //=========================
    //  Accessors
    //=========================
    public synchronized boolean checkCredentials (String username, String password) {
        if (username == "" || password == "") {
            return false;
        }

        // Check if there is a row with that username and password, if it exists, then
        // credentials are correct
        Cursor cursor = database.rawQuery(String.format(
                "select * from %s where %s='%s' and %s='%s'",
                UserTable.TABLE_USER, UserTable.COLUMN_USERNAME, username,
                UserTable.COLUMN_PASSWORD, password), null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        boolean correct = !cursor.isAfterLast();
        cursor.close();
        return correct;
    }

    //=========================
    // Private Methods
    //=========================

    private long internalPutUser(String username, String password) {
        ContentValues values = new ContentValues();
        addNonNullValue(values, UserTable.COLUMN_USERNAME, username);
        addNonNullValue(values, UserTable.COLUMN_PASSWORD, password);

        // Where statement
        ArrayList<Pair<String, Object>> where = new ArrayList<Pair<String,Object>>();
        where.add(new Pair<String, Object>(UserTable.COLUMN_USERNAME, username));

        return updateElseInsert(UserTable.TABLE_USER, where, values);
    }
}
