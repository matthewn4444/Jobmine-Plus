package com.jobmineplus.mobile.database.pages;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Pair;

import com.bugsense.trace.BugSenseHandler;
import com.jobmineplus.mobile.database.DataSourceBase;
import com.jobmineplus.mobile.widgets.Job;

public final class PageDataSource extends DataSourceBase{
    // Database fields
    private PageDatabaseHelper dbHelper;

    public PageDataSource(Context context) {
        dbHelper = new PageDatabaseHelper(context);
    }

    @Override
    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    @Override
    public void close() {
        dbHelper.close();
    }

    public synchronized void addPage(String username, String pagename, ArrayList<Job> jobs,
            long timestamp) {
        String joblist = "";
        if (!jobs.isEmpty()) {
            // Make list of jobs as string, remove last comma
            StringBuilder sb = new StringBuilder();
            for (Job job : jobs) {
                if (job != null) {
                    sb.append(job.getId()).append(',');
                }
            }
            if (sb.charAt(sb.length() - 1) == ',') {
                sb.deleteCharAt(sb.length() - 1);
            }
            joblist = sb.toString();
        }
        internalAddPage(username, pagename, joblist, timestamp);
    }

    public synchronized void addPage(String username, String pagename, HashMap<String, ArrayList<Job>> jobMap,
            long timestamp) {
        // Build the string
        StringBuilder sb = new StringBuilder();
        for (String key : jobMap.keySet()) {
            ArrayList<Job> jobs = jobMap.get(key);
            sb.append(key).append(':');
            for (Job job : jobs) {
                if (job != null) {
                    sb.append(job.getId()).append(',');
                }
            }
            if (sb.charAt(sb.length() - 1) == ',') {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append('|');
        }
        sb.deleteCharAt(sb.length() - 1);
        internalAddPage(username, pagename, sb.toString(), timestamp);
    }

    /**
     * Returns all the ids of jobs from this user of the page specified
     * @param pagename
     * @return list of ids, null if empty
     */
    public synchronized ArrayList<Integer> getJobsIds(String username, String pagename) {
        Cursor cursor = getCursorFromPage(username, pagename, PageTable.COLUMN_JOBLIST);
        if (cursor == null) { return null; }
        return cursorToJobIds(cursor);
    }

    public synchronized HashMap<String, ArrayList<Integer>> getJobsIdMap(String username, String pagename) {
        Cursor cursor = getCursorFromPage(username, pagename, PageTable.COLUMN_JOBLIST);
        if (cursor == null) { return null; }
        return cursorToJobsMap(cursor);
    }

    public synchronized PageResult getPageData(String username, String pagename) {
        Cursor cursor = getCursorFromPage(username, pagename, PageTable.COLUMN_JOBLIST, PageTable.COLUMN_TIME);
        if (cursor == null) { return null; }

        long time = cursor.getLong(1);
        return new PageResult(cursorToJobIds(cursor), time);
    }

    public synchronized PageMapResult getPageDataMap(String username, String pagename) {
        Cursor cursor = getCursorFromPage(username, pagename, PageTable.COLUMN_JOBLIST, PageTable.COLUMN_TIME);
        if (cursor == null) { return null; }

        long time = cursor.getLong(1);
        return new PageMapResult(cursorToJobsMap(cursor), time);
    }

    public synchronized ArrayList<Integer> cursorToJobIds(Cursor cursor) {
        String retStr = cursor.getString(0);
        if (retStr.length() == 0) {
            cursor.close();
            return null;
        }
        String[] idStrings = retStr.split(",");
        ArrayList<Integer> ids = new ArrayList<Integer>(idStrings.length);
        for (int i = 0; i < idStrings.length; i++) {
            try {
                ids.add(Integer.parseInt(idStrings[i]));
            } catch(NumberFormatException e) {
                e.printStackTrace();
                BugSenseHandler.sendExceptionMessage("Weird parsing ", idStrings[i], e);
            }
        }
        cursor.close();
        return ids;
    }

    private HashMap<String, ArrayList<Integer>> cursorToJobsMap(Cursor cursor) {
        String tabt = cursor.getString(0);
        String[] tabString = tabt.split("\\|");

        HashMap<String, ArrayList<Integer>> jobMap = new HashMap<String, ArrayList<Integer>>();

        for (String str : tabString) {
            int colonPos =  str.indexOf(':');
            if (colonPos == -1) {
                continue;
            }
            String tag = str.substring(0, colonPos);
            String retStr = str.substring(colonPos + 1);
            if (retStr.length() == 0) {
                jobMap.put(tag, null);
                continue;
            }
            String[] idStrings = retStr.split(",");
            ArrayList<Integer> ids = new ArrayList<Integer>(idStrings.length);
            for (int i = 0; i < idStrings.length; i++) {
                ids.add(Integer.parseInt(idStrings[i]));
            }
            jobMap.put(tag, ids);
        }
        cursor.close();
        return jobMap;
    }

    private Cursor getCursorFromPage(String username, String pagename, String... columns) {
        if (username == null) {
            throw new NullPointerException("Username is null trying to get cursor from page.");
        }
        // Build query
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        for (String column : columns) {
            sb.append(column).append(',');
        }
        sb.deleteCharAt(sb.length() - 1);       // Remove last comma

        // select <columns> from <table> where pageCol='page' and userCol='username'
        sb.append(" from ").append(PageTable.TABLE_PAGE).append(' ')
        .append(" where ").append(PageTable.COLUMN_PAGENAME).append("='")
        .append(pagename).append("' and ").append(PageTable.COLUMN_USERNAME)
        .append("='").append(username).append('\'');

        // Run query
        if (!database.isOpen()) {
            open();
        }
        Cursor cursor = database.rawQuery(sb.toString(), null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        if (cursor.isAfterLast()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    private void internalAddPage(String username, String pagename,
            String jobsString, long timestamp) {

        ContentValues values = new ContentValues();
        addNonNullValue(values, PageTable.COLUMN_USERNAME, username);
        addNonNullValue(values, PageTable.COLUMN_PAGENAME, pagename);
        addNonNullValue(values, PageTable.COLUMN_JOBLIST, jobsString);
        addNonNullValue(values, PageTable.COLUMN_TIME, timestamp);

        // Where statement
        ArrayList<Pair<String, Object>> where = new ArrayList<Pair<String,Object>>();
        where.add(new Pair<String, Object>(PageTable.COLUMN_PAGENAME, pagename));
        where.add(new Pair<String, Object>(PageTable.COLUMN_USERNAME, username));

        updateElseInsert(PageTable.TABLE_PAGE, where, values);
    }
}
