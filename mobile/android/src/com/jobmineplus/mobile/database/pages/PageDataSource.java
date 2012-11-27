package com.jobmineplus.mobile.database.pages;

import java.util.ArrayList;
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

    private final String[] allColumns = {
            PageTable.COLUMN_ID,
            PageTable.COLUMN_USER,
            PageTable.COLUMN_PAGENAME,
            PageTable.COLUMN_JOBLIST,
            PageTable.COLUMN_TIME
    };

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
        internalAddPage(service.getUsername(), pagename, jobs, timestamp);
    }

    public synchronized int[] getJobsIds(String pagename) {
        Cursor cursor = database.rawQuery(String.format(
                "select * from %s where %s='%s' and %s='%s'",
                PageTable.TABLE_PAGE, PageTable.COLUMN_PAGENAME, pagename,
                PageTable.COLUMN_USER, service.getUsername()), null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        if (cursor.isAfterLast()) {
            return null;
        }

        String[] idStrings = cursor.getString(3).split(",");
        int[] ids = new int[idStrings.length];
        for (int i = 0; i < idStrings.length; i++) {
            ids[i] = Integer.parseInt(idStrings[i]);
        }
        cursor.close();
        return ids;
    }

    private void internalAddPage(String username, String pagename,
            ArrayList<Job> jobs, long timestamp) {

        // Make list of jobs as string, remove last comma
        StringBuilder sb = new StringBuilder();
        for (Job job : jobs) {
            sb.append(job.getId()).append(',');
        }
        sb.deleteCharAt(sb.length() - 1);

        ContentValues values = new ContentValues();
        addNonNullValue(values, PageTable.COLUMN_USER, username);
        addNonNullValue(values, PageTable.COLUMN_PAGENAME, pagename);
        addNonNullValue(values, PageTable.COLUMN_JOBLIST, sb.toString());
        addNonNullValue(values, PageTable.COLUMN_TIME, timestamp);

        // Where statement
        ArrayList<Pair<String, Object>> where = new ArrayList<Pair<String,Object>>();
        where.add(new Pair<String, Object>(PageTable.COLUMN_PAGENAME, pagename));
        where.add(new Pair<String, Object>(PageTable.COLUMN_USER, username));

        updateElseInsert(PageTable.TABLE_PAGE, where, values);
    }
}
