package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.Date;

import org.jsoup.nodes.Document;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.widgets.Job;

public class Applications extends JbmnplsTabListActivityBase{
    
    //======================
    //  Declaration Objects
    //======================
    private final static class LISTS {
        final public static String ALL_JOBS = "all";
        final public static String ACTIVE_JOBS = "active";
        final public static String REJECTED_JOBS = "rejected";
    }
    
    protected final String DATE_FORMAT = "d-MMM-yyyy";
    
    protected final ColumnInfo COLUMN1 = new ColumnInfo(1, ColumnInfo.TEXT);
    protected final ColumnInfo COLUMN2 = new ColumnInfo(2, ColumnInfo.TEXT);
    protected final ColumnInfo COLUMN4 = new ColumnInfo(4, ColumnInfo.TEXT);
    protected final ColumnInfo COLUMN5 = new ColumnInfo(5, ColumnInfo.STATE);
    protected final ColumnInfo COLUMN6 = new ColumnInfo(6, ColumnInfo.STATUS);
    protected final ColumnInfo COLUMN8 = new ColumnInfo(8, ColumnInfo.DATE, DATE_FORMAT);
    protected final ColumnInfo COLUMN9 = new ColumnInfo(9, ColumnInfo.NUMERIC);
    
    protected final TableParsingOutline ACTIVE_OUTLINE = 
            new TableParsingOutline("UW_CO_STU_APPSV$scroll$0", 10,
                    COLUMN1, COLUMN2, COLUMN4, COLUMN5, COLUMN6, COLUMN8, COLUMN9);
    
    protected final TableParsingOutline ALL_OUTLINE = 
            new TableParsingOutline("UW_CO_APPS_VW2$scrolli$0", 12,
                    COLUMN1, COLUMN2, COLUMN4, COLUMN5, COLUMN6, COLUMN8, COLUMN9);
    
    //====================
    //  Override Methods
    //====================
    
    @Override
    protected String setUp(Bundle savedInstanceState) {
        setContentView(R.layout.applications);
        return JbmnplsHttpService.GET_LINKS.APPLICATIONS;
    
    }
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        super.defineUI(savedInstanceState);

        createTab(LISTS.ALL_JOBS, "All");
        createTab(LISTS.ACTIVE_JOBS, "Active");
        createTab(LISTS.REJECTED_JOBS, "Rejected");
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        int jobId = getListByTabId(getCurrentTab()).get(arg2);
        goToDescription(jobId);
    }

    @Override
    protected void parseWebpage(Document doc) {
        clearAllLists();
        ACTIVE_OUTLINE.execute(doc);
        ALL_OUTLINE.execute(doc);
    }
    
    @Override
    protected void onRowParse(TableParsingOutline outline, Object... jobData) {
        Job.STATUS status = (Job.STATUS)jobData[5];
        int id = (Integer) jobData[0];
        //Applications constructor
        Job job = new Job(          id, (String)    jobData[1],     
                (String)    jobData[2], (String)    jobData[3],   
                (Job.STATE) jobData[4],                 status, 
                (Date)      jobData[6], (Integer)   jobData[7]);
        if (outline.equals(ALL_OUTLINE)) {
            if (status == Job.STATUS.EMPLOYED) {
                addJobToListByTabId(LISTS.ACTIVE_JOBS, job);
            } else {
                //  If this job id is not contained inside Active, then we can
                //  put it in rejected
                if (!listContainsId(LISTS.ACTIVE_JOBS, id)) {
                    addJobToListByTabId(LISTS.REJECTED_JOBS, job);
                }
            }
            addJobToService(job);
            addJobToListByTabId(LISTS.ALL_JOBS, job);
        } else {
            addJobToListByTabId(LISTS.ACTIVE_JOBS, job);
        }
    }
    
    @Override
    protected void onRequestComplete() {
        updateList(LISTS.ALL_JOBS);
    }
}