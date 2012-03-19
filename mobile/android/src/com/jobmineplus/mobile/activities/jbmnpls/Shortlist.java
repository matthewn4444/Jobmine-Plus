package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.Date;

import org.jsoup.nodes.Document;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.widgets.Job;

public class Shortlist extends JbmnplsListActivityBase{

    //======================
    //  Declaration Objects
    //======================
    protected final String DATE_FORMAT = "d MMM yyyy";
    
    protected final TableParsingOutline SHORTLIST_OUTLINE = 
            new TableParsingOutline("UW_CO_STUJOBLST$scrolli$0", 9,
                    new ColumnInfo(1, ColumnInfo.TEXT), 
                    new ColumnInfo(2, ColumnInfo.TEXT), 
                    new ColumnInfo(4, ColumnInfo.TEXT), 
                    new ColumnInfo(5, ColumnInfo.STATUS), 
                    new ColumnInfo(6, ColumnInfo.DATE, DATE_FORMAT), 
                    new ColumnInfo(7, ColumnInfo.NUMERIC));
    
    //====================
    //  Override Methods
    //====================
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        int jobId = getList().get(arg2);
        goToDescription(jobId);
    }

    @Override
    protected String setUp(Bundle savedInstanceState) {
        setContentView(R.layout.shortlist);
        return JbmnplsHttpService.GET_LINKS.SHORTLIST;
    }
    
    @Override
    protected void onRowParse(TableParsingOutline outline, Object... jobData) {
        Job job = new Job(  //Shortlist constructor
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

    @Override
    protected void onRequestComplete() {
        updateList();
    }
}