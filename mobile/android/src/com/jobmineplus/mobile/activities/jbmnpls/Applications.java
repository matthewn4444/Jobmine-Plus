package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.Date;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.Job.STATUS;
import com.jobmineplus.mobile.widgets.TutorialHelper;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;
import com.jobmineplus.mobile.widgets.Job.HEADER;

public class Applications extends JbmnplsPageListActivityBase implements TableParser.OnTableParseListener{

    //======================
    //  Declaration Objects
    //======================
    public final static String PAGE_NAME = Applications.class.getName();
    public final static String ACTIVE_TABLE_ID = "UW_CO_STU_APPSV$scroll$0";
    public final static String ALL_TABLE_ID = "UW_CO_APPS_VW2$scrolli$0";

    public final static class LISTS {
        final public static String ALL_JOBS = "All";
        final public static String ACTIVE_JOBS = "Active";
        final public static String REJECTED_JOBS = "Rejected";
    }

    public final static HEADER[] SORT_HEADERS = {
        HEADER.JOB_TITLE,
        HEADER.EMPLOYER,
        HEADER.APP_STATUS
    };

    protected final static String DATE_FORMAT = "d-MMM-yyyy";
    private final TableParser parser = new TableParser();

    public static final TableParserOutline[] ACTIVE_OUTLINES = {
        // 10 Columns
        new TableParserOutline(ACTIVE_TABLE_ID,
                HEADER.JOB_ID, HEADER.JOB_TITLE, HEADER.EMPLOYER, HEADER.UNIT, HEADER.TERM,
                HEADER.JOB_STATUS, HEADER.APP_STATUS, HEADER.VIEW_DETAILS, HEADER.LAST_DAY_TO_APPLY, HEADER.NUM_APPS),

        // 9 Columns
        new TableParserOutline(ACTIVE_TABLE_ID,
                HEADER.JOB_ID, HEADER.JOB_TITLE, HEADER.EMPLOYER, HEADER.UNIT, HEADER.TERM,
                HEADER.JOB_STATUS, HEADER.VIEW_DETAILS, HEADER.LAST_DAY_TO_APPLY, HEADER.NUM_APPS),

        // 9 Columns with App Status
        new TableParserOutline(ACTIVE_TABLE_ID,
                HEADER.JOB_ID, HEADER.JOB_TITLE, HEADER.EMPLOYER, HEADER.UNIT, HEADER.TERM,
                HEADER.JOB_STATUS, HEADER.APP_STATUS, HEADER.VIEW_DETAILS, HEADER.NUM_APPS, HEADER.LAST_DAY_TO_APPLY)
    };

    public static final TableParserOutline ALL_OUTLINE = new TableParserOutline(ALL_TABLE_ID,
            HEADER.JOB_ID, HEADER.JOB_TITLE, HEADER.EMPLOYER, HEADER.UNIT, HEADER.TERM,
            HEADER.JOB_STATUS, HEADER.APP_STATUS, HEADER.VIEW_DETAILS, HEADER.LAST_DAY_TO_APPLY, HEADER.NUM_APPS);

    protected static final int[] WIDGET_RESOURCE_LIST = {
            R.id.job_title, R.id.job_employer, R.id.location,
            R.id.job_status_first_line, R.id.job_status_second_line,
            R.id.job_last_day};

    //============================
    //  Static Public Methods
    //============================
    public static Job parseRowTableOutline(TableParserOutline outline, Object... jobData) {
        int id = (Integer) jobData[0];

        if (outline == ACTIVE_OUTLINES[0] || outline == ALL_OUTLINE) {
            Job.STATUS status = (Job.STATUS)jobData[6];
            return new Job(          id,    (String)    jobData[1],
                    (String)    jobData[2], (String)    jobData[4],
                    (Job.STATE) jobData[5],                 status,
                    (Date)      jobData[8], (Integer)   jobData[9]);
        } else if (outline == ACTIVE_OUTLINES[1]) {
            return new Job(          id,    (String)    jobData[1],
                    (String)    jobData[2], (String)    jobData[4],
                    (Job.STATE) jobData[5],     STATUS.getDefault(),
                    (Date)      jobData[7], (Integer)   jobData[8]);
        } else {    // ACTIVE_OUTLINES[2]
            Job.STATUS status = (Job.STATUS)jobData[6];
            return new Job(          id,    (String)    jobData[1],
                    (String)    jobData[2], (String)    jobData[4],
                    (Job.STATE) jobData[5],                 status,
                    (Date)      jobData[9], (Integer)   jobData[8]);
        }
    }

    //====================
    //  Override Methods
    //====================

    @Override
    protected String setUp(Bundle savedInstanceState) {
        pageName = Applications.class.getName();
        return JbmnplsHttpClient.GET_LINKS.APPLICATIONS;
    }
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        // Create the tutorial and set the content of this activity
        new TutorialHelper(this, R.layout.tabs,
                R.layout.tutorial_sorting, R.string.pref_seen_sorting_tutorial);

        super.defineUI(savedInstanceState);
        parser.setOnTableRowParse(this);
        createTab(LISTS.ACTIVE_JOBS);
        createTab(LISTS.REJECTED_JOBS);
        createTab(LISTS.ALL_JOBS);
    }

    @Override
    public HEADER[] getTableHeaders() {
        return SORT_HEADERS;
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        if (arg2 < getCurrentList().size()) {
            int jobId = getCurrentList().get(arg2).getId();
            goToDescription(jobId);
        }
    }

    @Override
    protected void parseWebpage(String html) {
        clearAllLists();
        parser.execute(ACTIVE_OUTLINES, html);
        parser.execute(ALL_OUTLINE, html);
    }

    public void onRowParse(TableParserOutline outline, Object... jobData) {
        Job job = parseRowTableOutline(outline, jobData);
        Job.STATUS status = job.getStatus();

        if (outline.equals(ALL_OUTLINE)) {
            if (!listContainsId(LISTS.ACTIVE_JOBS, job.getId())) {
                if (status == Job.STATUS.EMPLOYED) {
                    addJobToListByTabId(LISTS.ACTIVE_JOBS, job);
                } else {
                    //  If this job id is not contained inside Active, then we can
                    //  put it in rejected
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
        return new ApplicationsAdapter(this, R.layout.job_widget, WIDGET_RESOURCE_LIST, list);
    }

    //=================
    //  List Adapter
    //=================
    private class ApplicationsAdapter extends JbmnplsAdapterBase {
        public ApplicationsAdapter(Activity a, int widgetResourceLayout,
                int[] viewResourceIdListInWidget, ArrayList<Job> list) {
            super(a, widgetResourceLayout, viewResourceIdListInWidget,
                    list);
        }

        @Override
        protected HIGHLIGHTING setJobWidgetValues(Job job, View[] elements, View layout) {
            final Applications applications = (Applications) getActivity();
            String status = job.getDisplayStatus();

            // Set the text fields
            setText(0, job.getTitle());
            setText(1, job.getEmployer(), true);
            hide(2);
            setText(3, 4, status, true);

            // Show the closing date if hasnt passed yet
            Date closingDate = job.getLastDateToApply();
            if (closingDate.after(new Date())) {
                setDate(5, job.getLastDateToApply(), "Closes by");
            } else {
                hide(5);
            }

            /*
             *  Do the highlighting
             */
            if (isOneOf(status, "Selected", "Scheduled", "Employed") || status == "Ranking Completed"
                    && applications.listContainsId(LISTS.ACTIVE_JOBS, job.getId())) {
                return HIGHLIGHTING.GREAT;
            } else if (isOneOf(status, "Unfilled", "Ranking Completed", "Not Ranked", "Sign Off")) {
                return HIGHLIGHTING.BAD;
            } else if (isOneOf(status, "Not Selected", "Cancelled")) {
                return HIGHLIGHTING.WORSE;
            } else {
                return HIGHLIGHTING.NORMAL;
            }
        }
    }
}