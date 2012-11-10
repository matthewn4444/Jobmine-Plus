package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.Date;

import org.jsoup.nodes.Document;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.ViewAdapterBase;
import com.jobmineplus.mobile.widgets.table.ColumnInfo;
import com.jobmineplus.mobile.widgets.table.TableParsingOutline;

public class Shortlist extends JbmnplsListActivityBase implements TableParsingOutline.OnTableParseListener {

    //======================
    //  Declaration Objects
    //======================
    protected final String DATE_FORMAT = "d MMM yyyy";
    
    protected final TableParsingOutline SHORTLIST_OUTLINE = 
            new TableParsingOutline("UW_CO_STUJOBLST$scrolli$0", 9, 0,
                    new ColumnInfo(1, ColumnInfo.TEXT), 
                    new ColumnInfo(2, ColumnInfo.TEXT), 
                    new ColumnInfo(4, ColumnInfo.TEXT), 
                    new ColumnInfo(5, ColumnInfo.STATUS), 
                    new ColumnInfo(6, ColumnInfo.DATE, DATE_FORMAT), 
                    new ColumnInfo(7, ColumnInfo.NUMERIC));
    
    protected final int[] WIDGET_RESOURCE_LIST = { 
            R.id.job_title, R.id.job_employer, R.id.location, 
            R.id.job_status,R.id.job_last_day, R.id.job_apps };

    //====================
    //  Override Methods
    //====================
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        super.defineUI(savedInstanceState);
        SHORTLIST_OUTLINE.setOnTableRowParse(this);
        setAdapter(new ShortlistAdapter(this, android.R.id.list, 
                R.layout.job_widget, WIDGET_RESOURCE_LIST, getList()));
    }
    
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        int jobId = getList().get(arg2).getId();
        goToDescription(jobId);
    }
    
    @Override
    protected String setUp(Bundle savedInstanceState) {
        setContentView(R.layout.shortlist);
        return JbmnplsHttpService.GET_LINKS.SHORTLIST;
    }
    
    public void onRowParse(TableParsingOutline outline, Object... jobData) {
        Job job = new Job(  // Shortlist constructor
                (Integer)   jobData[0],     (String)jobData[1],
                (String)    jobData[2],     (String)jobData[3],
                (Job.STATUS)jobData[4],     (Date)  jobData[5],
                (Integer)   jobData[6]);
        addJob(job);
    }

    @Override
    protected void parseWebpage(Document doc) {
        clearList();
        SHORTLIST_OUTLINE.execute(doc);
    }

    //=================
    //  List Adapter
    //=================
    private class ShortlistAdapter extends ViewAdapterBase<Job> {
        public ShortlistAdapter(Activity a, int listViewResourceId,
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
                ((TextView) elements[2]).setText(job.getLocation());
                ((TextView) elements[3]).setText(job.getDisplayStatus());
                ((TextView) elements[4]).setText(DISPLAY_DATE_FORMAT.format(job.getLastDateToApply()));
                ((TextView) elements[5]).setText(Integer.toString(job.getNumberOfApplications()));
            }
        }
    }
}