package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.Date;
import java.util.ListIterator;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.HiddenColumnsException;
import com.jobmineplus.mobile.exceptions.JbmnplsException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.widgets.Job;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.ListView;
import android.widget.TextView;


public class Applications extends JbmnplsActivityBase{
	
	protected final String[] TABLE_ID = {"UW_CO_STU_APPSV$scroll$0", "UW_CO_APPS_VW2$scrolli$0"};
	protected final short[] TABLE_NUM_COLS = {10, 12};				  
	
	protected Job[] activeJobs;
	protected ArrayList<Job> allJobs;
	protected JobListAdapter adapter;
	
	protected TextView output;
	ListView list;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		//Set parent's values
		layout = R.layout.applications;
		dataUrl = JbmnplsHttpService.GET_LINKS.APPLICATIONS;
		
        super.onCreate(savedInstanceState);
        
        output = (TextView) findViewById(R.id.output);
    	output.setMovementMethod(new ScrollingMovementMethod());
    	
    	
    	list = (ListView) findViewById(R.id.job_list);
    	allJobs = new ArrayList<Job>();
    }
	
	protected Element parseAppsTable(Document doc, String id) throws JbmnplsParsingException {
		try{
			return doc.getElementById(id).select("tr:eq(1) table table").first();
		} catch(Exception e) {
			throw new JbmnplsParsingException("Problem parsing Applications table.");
		}
	}
	
	@Override
	protected void parseWebpage(Document doc) throws HiddenColumnsException, JbmnplsParsingException{
		Element activeApps = doc.getElementById(TABLE_ID[0]);
		Element allApps = parseAppsTable(doc, TABLE_ID[1]);
		
		if (allApps == null) {
			throw new JbmnplsParsingException("Cannot parse '" + TABLE_ID[1] + "' in Applications.");
		}
		Elements rows = allApps.getElementsByTag("tr");
		
		Element header = rows.get(0);
		int numRows = rows.size();
		Job job;
		if (header.getElementsByTag("th").size() != TABLE_NUM_COLS[1]) {
			throw new HiddenColumnsException();
		}
		for (int i = 1; i < numRows; i++) {
			Element rowEl = rows.get(i);
			Elements tds = rowEl.getElementsByTag("td");
			String id 			= getTextFromElement(tds.get(0));
			String title 		= getTextFromElement(tds.get(1));
			String employer 	= getTextFromElement(tds.get(2));
			String term 		= getTextFromElement(tds.get(4));
			Job.STATE state = Job.STATE.getStatefromString(getTextFromElement(tds.get(5)));
			Job.STATUS status = Job.STATUS.getStatusfromString(getTextFromElement(tds.get(6)));
			Date lastDate 		= getDateFromElement(tds.get(8));
			int numApps 		= getIntFromElement	(tds.get(9));
			job = new Job(id, title, employer, term, state, status, lastDate, numApps);
			allJobs.add(job);
		}
		adapter = new JobListAdapter(this, R.id.job_list, allJobs);
		list.setAdapter(adapter);
	}
}

