package com.jobmineplus.mobile.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public abstract class DataSourceBase {
    protected SQLiteDatabase database;

    // ======================
    // Abstract Methods
    // ======================
    public abstract void open();

    public abstract void close();

    // ======================
    // Protected Methods
    // ======================
    protected void updateElseInsert(String table, long id, ContentValues values) {
        // Unlike insert, update does not throw any errors and it will be faster
        int affected = database.update(table, values, "_id=?", new String[]{id + ""});
        if (affected == 0) {
            database.insertOrThrow(table, null, values);
        }
    }

    protected void addNonNullValue(ContentValues values, String column, String value) {
        if (value != null) {
            values.put(column, value);
        }
    }

    protected void addNonNullValue(ContentValues values, String column, int value) {
        if (value != 0) {
            values.put(column, value);
        }
    }

    protected void addNonNullValue(ContentValues values, String column, long value) {
        if (value != 0) {
            values.put(column, value);
        }
    }

    // TEMP
    protected void log(Object... txt) {
        String returnStr = "";
        int i = 1;
        int size = txt.length;
        if (size != 0) {
            returnStr = txt[0] == null ? "null" : txt[0].toString();
            for (; i < size; i++) {
                returnStr += ", "
                        + (txt[i] == null ? "null" : txt[i].toString());
            }
        }
        System.out.println(returnStr);
    }
}
