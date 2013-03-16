package com.jobmineplus.mobile.database;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

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
    protected long updateElseInsert(String table, ArrayList<Pair<String, Object>> where, ContentValues values) {
        // Unlike insert, update does not throw any errors and it will be faster
        String whereStr = "";
        int i = 0;
        for (i = 0; i < where.size() - 1; i++) {
            whereStr += where.get(i).first + "='" + where.get(i).second + "' AND ";
        }
        whereStr += where.get(i).first + "=?";

        long affected = database.update(table, values, whereStr, new String[]{where.get(i).second + ""});
        if (affected == 0) {
            affected = database.insertOrThrow(table, null, values);
        }
        return affected;
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
        Log.i("jbmnplsmbl", returnStr);
    }
}
