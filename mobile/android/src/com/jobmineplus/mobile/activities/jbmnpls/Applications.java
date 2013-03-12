package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.ViewAdapterBase;
import com.jobmineplus.mobile.widgets.table.ColumnInfo;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;

public class Applications extends JbmnplsTabListActivityBase implements TableParser.OnTableParseListener{

    //======================
    //  Declaration Objects
    //======================
    public final static String PAGE_NAME = Applications.class.getName();

    public final static class LISTS {
        final public static String ALL_JOBS = "all";
        final public static String ACTIVE_JOBS = "active";
        final public static String REJECTED_JOBS = "rejected";
    }

    protected final static String DATE_FORMAT = "d-MMM-yyyy";
    private final TableParser parser = new TableParser();

    protected final static ColumnInfo COLUMNID = new ColumnInfo(0, ColumnInfo.ID);
    protected final static ColumnInfo COLUMN1  = new ColumnInfo(1, ColumnInfo.TEXT);
    protected final static ColumnInfo COLUMN2  = new ColumnInfo(2, ColumnInfo.TEXT);
    protected final static ColumnInfo COLUMN4  = new ColumnInfo(4, ColumnInfo.TEXT);
    protected final static ColumnInfo COLUMN5  = new ColumnInfo(5, ColumnInfo.STATE);
    protected final static ColumnInfo COLUMN6  = new ColumnInfo(6, ColumnInfo.STATUS);
    protected final static ColumnInfo COLUMN8  = new ColumnInfo(8, ColumnInfo.DATE, DATE_FORMAT);
    protected final static ColumnInfo COLUMN9  = new ColumnInfo(9, ColumnInfo.NUMERIC);

    public static final TableParserOutline ACTIVE_OUTLINE =
            new TableParserOutline("UW_CO_STU_APPSV$scroll$0", 10,
                    COLUMNID, COLUMN1, COLUMN2, COLUMN4, COLUMN5, COLUMN6, COLUMN8, COLUMN9);

    public static final TableParserOutline ALL_OUTLINE =
            new TableParserOutline("UW_CO_APPS_VW2$scrolli$0", 12,
                    COLUMNID, COLUMN1, COLUMN2, COLUMN4, COLUMN5, COLUMN6, COLUMN8, COLUMN9);

    protected static final int[] WIDGET_RESOURCE_LIST = {
            R.id.job_title, R.id.job_employer, R.id.location,
            R.id.job_status,R.id.job_last_day, R.id.job_apps };

    //============================
    //  Static Public Methods
    //============================
    public static Job parseRowTableOutline(TableParserOutline outline, Object... jobData) {
        Job.STATUS status = (Job.STATUS)jobData[5];
        int id = (Integer) jobData[0];
        //Applications constructor
        return new Job(          id, (String)    jobData[1],
                (String)    jobData[2], (String)    jobData[3],
                (Job.STATE) jobData[4],                 status,
                (Date)      jobData[6], (Integer)   jobData[7]);
    }

    //====================
    //  Override Methods
    //====================

    @Override
    protected String setUp(Bundle savedInstanceState) {
        setContentView(R.layout.applications);
        pageName = Applications.class.getName();
        return JbmnplsHttpClient.GET_LINKS.APPLICATIONS;
    }
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        super.defineUI(savedInstanceState);
        parser.setOnTableRowParse(this);

        createTab(LISTS.ALL_JOBS, "All");
        createTab(LISTS.ACTIVE_JOBS, "Active");
        createTab(LISTS.REJECTED_JOBS, "Rejected");
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        int jobId = getListByTabId(getCurrentTab()).get(arg2).getId();
        goToDescription(jobId);
    }

    @Override
    protected void parseWebpage(String html) {
        clearAllLists();
        parser.execute(ACTIVE_OUTLINE, html);
        parser.execute(ALL_OUTLINE, html);
    }

    public void onRowParse(TableParserOutline outline, Object... jobData) {
        Job job = parseRowTableOutline(outline, jobData);
        Job.STATUS status = job.getStatus();

        if (outline.equals(ALL_OUTLINE)) {
            if (status == Job.STATUS.EMPLOYED) {
                if (!listContainsId(LISTS.ACTIVE_JOBS, job.getId())) {
                    addJobToListByTabId(LISTS.ACTIVE_JOBS, job);
                }
            } else {
                //  If this job id is not contained inside Active, then we can
                //  put it in rejected
                if (!listContainsId(LISTS.ACTIVE_JOBS, job.getId())) {
                    addJobToListByTabId(LISTS.REJECTED_JOBS, job);
                }
            }
            addJob(job);
            addJobToListByTabId(LISTS.ALL_JOBS, job);
        } else {
            addJobToListByTabId(LISTS.ACTIVE_JOBS, job);
        }
        addJob(job);
    }

    @Override
    protected ArrayAdapter<Job> makeAdapterFromList(ArrayList<Job> list) {
        return new ApplicationsAdapter(this, android.R.id.list,
                R.layout.job_widget, WIDGET_RESOURCE_LIST, list);
    }

    //=================
    //  List Adapter
    //=================
    private class ApplicationsAdapter extends ViewAdapterBase<Job> {
        public ApplicationsAdapter(Activity a, int listViewResourceId,
                int widgetResourceLayout, int[] viewResourceIdListInWidget,
                ArrayList<Job> list) {
            super(a, listViewResourceId, widgetResourceLayout, viewResourceIdListInWidget,
                    list);
        }

        @Override
        protected void setWidgetValues(Job job, View[] elements) {
            if (job != null) {
                ((TextView) elements[0]).setText(job.getTitle());
                ((TextView) elements[1]).setText(job.getEmployer());
                ((TextView) elements[2]).setVisibility(View.GONE);          //Make location invisible
                ((TextView) elements[3]).setText(job.getDisplayStatus());
                ((TextView) elements[4]).setText(DISPLAY_DATE_FORMAT.format(job.getLastDateToApply()));
                ((TextView) elements[5]).setText(Integer.toString(job.getNumberOfApplications()));
            }
        }
    }
}