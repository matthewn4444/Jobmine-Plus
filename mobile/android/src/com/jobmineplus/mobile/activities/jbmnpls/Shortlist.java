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
import com.jobmineplus.mobile.widgets.Job.APPLY_STATUS;
import com.jobmineplus.mobile.widgets.table.ColumnInfo;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;

public class Shortlist extends JbmnplsListActivityBase implements TableParser.OnTableParseListener {

    //======================
    //  Declaration Objects
    //======================
    protected final static String DATE_FORMAT = "d MMM yyyy";
    private final TableParser parser = new TableParser();

    public static final TableParserOutline SHORTLIST_OUTLINE =
            new TableParserOutline("UW_CO_STUJOBLST$scrolli$0", 9,
                    new ColumnInfo(0, ColumnInfo.ID),
                    new ColumnInfo(1, ColumnInfo.TEXT),
                    new ColumnInfo(2, ColumnInfo.TEXT),
                    new ColumnInfo(4, ColumnInfo.TEXT),
                    new ColumnInfo(5, ColumnInfo.APPLY_STATUS),
                    new ColumnInfo(6, ColumnInfo.DATE, DATE_FORMAT),
                    new ColumnInfo(7, ColumnInfo.NUMERIC));

    protected final int[] WIDGET_RESOURCE_LIST = {
            R.id.job_title, R.id.job_employer, R.id.location,
            R.id.job_status_first_line,R.id.job_status_second_line,
            R.id.job_last_day };

    //====================
    //  Override Methods
    //====================
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        super.defineUI(savedInstanceState);
        parser.setOnTableRowParse(this);
        setAdapter(new ShortlistAdapter(this, R.layout.job_widget, WIDGET_RESOURCE_LIST, getList()));
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
                (String)            jobData[2],     (String)jobData[3],
                (Job.APPLY_STATUS)  jobData[4],     (Date)  jobData[5],
                (Integer)           jobData[6]);
        addJob(job);
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
        protected HIGHLIGHTING setJobWidgetValues(Job job, View[] elements, View layout) {
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
            } else if (status == APPLY_STATUS.CANNOT_APPLY) {
                return HIGHLIGHTING.BAD;
            }
            return HIGHLIGHTING.NORMAL;
        }
    }
}