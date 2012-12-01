package com.jobmineplus.mobile.database.pages;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Pair;

import com.jobmineplus.mobile.database.DataSourceBase;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.widgets.Job;

public final class PageDataSource extends DataSourceBase{
    // Database fields
    private PageDatabaseHelper dbHelper;
    private JbmnplsHttpService service;

    public PageDataSource(Context context) {
        dbHelper = new PageDatabaseHelper(context);
        service = JbmnplsHttpService.getInstance();
    }

    @Override
    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    @Override
    public void close() {
        dbHelper.close();
    }

    public synchronized void addPage(String pagename, ArrayList<Job> jobs,
            long timestamp) {
        if (jobs.isEmpty()) {
            return;
        }
        String username = service.getUsername();
        if (username == "") { return; }

        // Make list of jobs as string, remove last comma
        StringBuilder sb = new StringBuilder();
        for (Job job : jobs) {
            sb.append(job.getId()).append(',');
        }
        sb.deleteCharAt(sb.length() - 1);

        internalAddPage(username, pagename, sb.toString(), timestamp);
    }

    public synchronized void addPage(String pagename, HashMap<String, ArrayList<Job>> jobMap,
            long timestamp) {
        if (jobMap.isEmpty()) {
            return;
        }
        String username = service.getUsername();
        if (username == "") { return; }

        // Build the string
        StringBuilder sb = new StringBuilder();
        for (String key : jobMap.keySet()) {
            ArrayList<Job> jobs = jobMap.get(key);
            sb.append(key).append(':');
            for (Job job : jobs) {
                sb.append(job.getId()).append(',');
            }
            sb.deleteCharAt(sb.length() - 1).append('|');
        }
        sb.deleteCharAt(sb.length() - 1);
        internalAddPage(username, pagename, sb.toString(), timestamp);
    }

    /**
     * Returns all the ids of jobs from this user of the page specified
     * @param pagename
     * @return list of ids, null if empty
     */
    public synchronized ArrayList<Integer> getJobsIds(String pagename) {
        String username = service.getUsername();
        if (username == "") { return null; }

        Cursor cursor = database.rawQuery(String.format(
                "select * from %s where %s='%s' and %s='%s'",
                PageTable.TABLE_PAGE, PageTable.COLUMN_PAGENAME, pagename,
                PageTable.COLUMN_USERNAME, username), null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        if (cursor.isAfterLast()) {
            cursor.close();
            return null;
        }

        String[] idStrings = cursor.getString(3).split(",");
        ArrayList<Integer> ids = new ArrayList<Integer>(idStrings.length);
        for (int i = 0; i < idStrings.length; i++) {
            ids.add(Integer.parseInt(idStrings[i]));
        }
        cursor.close();
        return ids;
    }

    public synchronized HashMap<String, ArrayList<Integer>> getJobsIdMap(String pagename) {
        String username = service.getUsername();
        if (username == "") { return null; }

        Cursor cursor = database.rawQuery(String.format(
                "select * from %s where %s='%s' and %s='%s'",
                PageTable.TABLE_PAGE, PageTable.COLUMN_PAGENAME, pagename,
                PageTable.COLUMN_USERNAME, username), null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        if (cursor.isAfterLast()) {
            return null;
        }

        String tabt = cursor.getString(3);
        String[] tabString = tabt.split("\\|");

        HashMap<String, ArrayList<Integer>> jobMap = new HashMap<String, ArrayList<Integer>>();

        for (String str : tabString) {
            int colonPos =  str.indexOf(':');
            if (colonPos == -1) {
                continue;
            }
            String tag = str.substring(0, colonPos);
            String[] idStrings = str.substring(colonPos + 1).split(",");
            ArrayList<Integer> ids = new ArrayList<Integer>(idStrings.length);
            for (int i = 0; i < idStrings.length; i++) {
                ids.add(Integer.parseInt(idStrings[i]));
            }
            jobMap.put(tag, ids);
        }
        cursor.close();
        return jobMap;
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
