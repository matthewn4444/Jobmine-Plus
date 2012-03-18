package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.Date;

import org.apache.http.message.BasicNameValuePair;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.HiddenColumnsException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.widgets.Job;

public class Shortlist extends JbmnplsListActivityBase{

    protected final String TABLE_ID = "UW_CO_STUJOBLST$scrolli$0";
    protected final short TABLE_NUM_COLS = 9;
    protected final String DATE_FORMAT = "d MMM yyyy";
    
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
    protected Object parseWebpage(Document doc) {
        Element table = parseTableById(doc, TABLE_ID);
        if (table == null) {
            throw new JbmnplsParsingException("Cannot parse '" + TABLE_ID + "' in Applications.");
        }
        Elements rows = table.getElementsByTag("tr");
        int numRows = rows.size();
        
        //Parse Table
        clearList();
        Element header = rows.get(0);
        Job job = null;
        if (header.getElementsByTag("th").size() != TABLE_NUM_COLS) {
            throw new HiddenColumnsException();
        }

        for (int i = 1; i < numRows; i++) {
            Element rowEl = rows.get(i);
            Elements tds = rowEl.getElementsByTag("td");
            
            // See if table is empty
            int id = getIntFromElement(tds.get(0));
            if (id == 0) {
                break;
            }
            String title        = getTextFromElement(tds.get(1));
            String employer     = getTextFromElement(tds.get(2));
            String location     = getTextFromElement(tds.get(4));
            Job.STATUS status   = Job.STATUS.getStatusfromString(getTextFromElement(tds.get(5)));
            Date lastDate       = getDateFromElement(tds.get(6), DATE_FORMAT);
            int numApps         = getIntFromElement(tds.get(7));
            job = new Job(id, title, employer, location, status, lastDate, numApps);
            addJob(job);
        }
        return null;
    }

    @Override
    protected void onRequestComplete() {
        updateList();
    }
    
    //=====================
    //  Protected Methods  
    //=====================
    protected void goToDescription(int jobId) {
        BasicNameValuePair pass = new BasicNameValuePair("jobId", Integer.toString(jobId));
        startActivity(Description.class, pass);
    }

}
