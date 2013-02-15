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
    public long putUser(String username, String password, Boolean isLastUser) {
        return internalPutUser(username, password, isLastUser);
    }

    public synchronized long putUser(String username, String password) {
        return internalPutUser(username, password, false);
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
        } else {
            return false;
        }
        boolean correct = !cursor.isAfterLast();
        cursor.close();
        return correct;
    }

    // TODO when implement logout, please mark the last user to false (0)
    public synchronized Pair<String, String> getLastUser() {
        Cursor cursor = database.rawQuery(String.format(
                "select %s, %s from %s where %s='%s'",
                UserTable.COLUMN_USERNAME, UserTable.COLUMN_PASSWORD, UserTable.TABLE_USER,
                UserTable.COLUMN_LAST_USER, 1), null);
        if (cursor != null) {
            cursor.moveToFirst();
        } else {
            return null;
        }
        Pair<String, String> p = new Pair<String, String>(cursor.getString(0), cursor.getString(1));
        cursor.close();
        return p;
    }

    //=========================
    // Private Methods
    //=========================

    private long internalPutUser(String username, String password, Boolean isLastUser) {
        ContentValues values = new ContentValues();
        addNonNullValue(values, UserTable.COLUMN_USERNAME, username);
        addNonNullValue(values, UserTable.COLUMN_PASSWORD, password);

        if (isLastUser) {
            addNonNullValue(values, UserTable.COLUMN_LAST_USER, 1);
        }

        // Where statement
        ArrayList<Pair<String, Object>> where = new ArrayList<Pair<String,Object>>();
        where.add(new Pair<String, Object>(UserTable.COLUMN_USERNAME, username));

        return updateElseInsert(UserTable.TABLE_USER, where, values);
    }
}
