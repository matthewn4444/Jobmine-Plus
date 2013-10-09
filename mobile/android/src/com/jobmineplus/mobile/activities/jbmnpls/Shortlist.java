package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.TutorialHelper;
import com.jobmineplus.mobile.widgets.Job.APPLY_STATUS;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;
import com.jobmineplus.mobile.widgets.Job.HEADER;

public class Shortlist extends JbmnplsListActivityBase implements TableParser.OnTableParseListener {

    //======================
    //  Declaration Objects
    //======================
    protected final static String DATE_FORMAT = "d MMM yyyy";
    private final TableParser parser = new TableParser();

    private static final HEADER[] SORT_HEADERS = {
        HEADER.JOB_TITLE,
        HEADER.EMPLOYER_NAME,
        HEADER.LOCATION,
        HEADER.APPLY
    };

    public static final TableParserOutline SHORTLIST_OUTLINE =
            new TableParserOutline("UW_CO_STUJOBLST$scrolli$0",
                    HEADER.JOB_IDENTIFIER,
                    HEADER.JOB_TITLE,
                    HEADER.EMPLOYER_NAME,
                    HEADER.UNIT_NAME_1,
                    HEADER.LOCATION,
                    HEADER.APPLY,
                    HEADER.LAST_DATE_TO_APPLY,
                    HEADER.NUM_APPS);

    protected final int[] WIDGET_RESOURCE_LIST = {
            R.id.job_title, R.id.job_employer, R.id.location,
            R.id.job_status_first_line,R.id.job_status_second_line,
            R.id.job_last_day };

    //====================
    //  Override Methods
    //====================
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        // Create the tutorial and set the content of this activity
        new TutorialHelper(this, R.layout.joblist,
                R.layout.tutorial_sorting, R.string.pref_seen_sorting_tutorial);

        super.defineUI(savedInstanceState);
        parser.setOnTableRowParse(this);
    }

    @Override
    public JbmnplsAdapterBase getNewAdapter() {
        return new ShortlistAdapter(this, R.layout.job_widget, WIDGET_RESOURCE_LIST, getList());
    }

    @Override
    protected String setUp(Bundle savedInstanceState) {
        pageName = Shortlist.class.getName();
        return JbmnplsHttpClient.GET_LINKS.SHORTLIST;
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        int jobId = getList().get(arg2).getId();
        goToDescription(jobId);
    }

    public void onRowParse(TableParserOutline outline, Object... jobData) {
        Job job = new Job(  // Shortlist constructor
                (Integer)           jobData[0],     (String)jobData[1],
                (String)            jobData[2],     (String)jobData[4],
                (Job.APPLY_STATUS)  jobData[5],     (Date)  jobData[6],
                (Integer)           jobData[7]);
        addJob(job);
    }

    @Override
    public HEADER[] getTableHeaders() {
        return SORT_HEADERS;
    }

    @Override
    protected void parseWebpage(String html) {
        clearList();
        parser.execute(SHORTLIST_OUTLINE, html);
    }

    //=================
    //  List Adapter
    //=================
    private class ShortlistAdapter extends JbmnplsAdapterBase {
        public ShortlistAdapter(Activity a, int listViewResourceId, int[] viewResourceIdListInWidget,
                ArrayList<Job> list) {
            super(a, listViewResourceId, viewResourceIdListInWidget, list);
        }

        @Override
        protected HIGHLIGHTING setJobWidgetValues(int position, Job job, View[] elements, View layout) {
            APPLY_STATUS status = job.getApplicationStatus();
            String statusStr = status == APPLY_STATUS.CANNOT_APPLY ? "Cannot Apply" : status.toString();

            setText(0, job.getTitle());
            setText(1, job.getEmployer(), true);
            setText(2, job.getLocation());
            setText(3, 4, statusStr, true);

            // Show the closing date if hasnt passed yet
            Date closingDate = job.getLastDateToApply();
            if (closingDate.after(new Date())) {
                setDate(5, job.getLastDateToApply(), "Apply by");
            } else {
                hide(5);
            }

            if (status == APPLY_STATUS.ALREADY_APPLIED) {
                return HIGHLIGHTING.GREAT;
            } else if (status == APPLY_STATUS.CANNOT_APPLY || status == APPLY_STATUS.NOT_POSTED) {
                return HIGHLIGHTING.BAD;
            }
            return HIGHLIGHTING.NORMAL;
        }
    }
}