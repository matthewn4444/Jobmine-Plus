package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.ListIterator;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.HiddenColumnsException;
import com.jobmineplus.mobile.exceptions.JbmnplsException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.services.JobService;
import com.jobmineplus.mobile.widgets.Job;

import android.app.LocalActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import org.apache.http.message.BasicNameValuePair;


public class Applications extends JbmnplsActivityBase implements OnItemClickListener, TabHost.TabContentFactory, TabHost.OnTabChangeListener{
	
	protected final String[] TABLE_ID = {"UW_CO_STU_APPSV$scroll$0", "UW_CO_APPS_VW2$scrolli$0"};
	protected final short[] TABLE_NUM_COLS = {10, 12};				  
	
	
	protected HashMap<Integer, Job> jobs;
	
	protected ArrayList<Integer> activeJobs;
	protected ArrayList<Integer> rejectedJobs;
	protected ArrayList<Integer> allJobs;
	protected JobListAdapter adapter;
	protected LocalActivityManager lcActMan;
	
	protected TabHost tabHost;
	protected FrameLayout content;
	ListView list;

	//====================
	// 	Override Methods
	//====================
	
	@Override
	protected String setUp(Bundle savedInstanceState) {
		setContentView(R.layout.applications);
		
    	list = (ListView) findViewById(R.id.job_list);
		content = (FrameLayout) findViewById(android.R.id.content);
		
    	list.setOnItemClickListener(this);
    	jobs = new HashMap<Integer, Job>();
    	
    	tabHost = (TabHost) findViewById(R.id.tabhost);
    	
		lcActMan = new LocalActivityManager(this, false);
		lcActMan.dispatchCreate(savedInstanceState);
		tabHost.setup(lcActMan);

		
		/*
		 * TODO fix up this mess, break it out maybe make an implementation?
		 * Fix how we should list items, should we make 3 list views instead
		 * of replacing just one?
		 */
		tabHost.setOnTabChangedListener(this);
		tabHost.addTab(tabHost.newTabSpec("all").setIndicator("All")
				.setContent(this));
		tabHost.addTab(tabHost.newTabSpec("active").setIndicator("Active")
				.setContent(this));
		tabHost.addTab(tabHost.newTabSpec("rejected").setIndicator("Rejected")
				.setContent(this));
    	
//    	return JbmnplsHttpService.GET_LINKS.APPLICATIONS;
    	return JbmnplsHttpService.GET_FAKE_LINKS.APPLICATIONS;
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		lcActMan.dispatchPause(isFinishing());
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		lcActMan.dispatchResume();
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}
	

	@Override
	public void onTabChanged(String tag) {
		if (allJobs != null) {
			if (tag == "all") {
				updateList(allJobs);
			} else if (tag == "active") {
				updateList(activeJobs);
			} else {
				updateList(rejectedJobs);
			}
		}
	}

	@Override
	public View createTabContent(String tag) {
		//If null, then the job lists arent downloaded yet :(
		if (allJobs != null) {
			if (tag == "all") {
				updateList(allJobs);
			} else if (tag == "active") {
				updateList(activeJobs);
			} else {
				updateList(rejectedJobs);
			}
		}
		return list;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		//TODO get the real list of whatever is displayed
		int jobId = allJobs.get(arg2);
		BasicNameValuePair pass = new BasicNameValuePair("jobId", Integer.toString(jobId));
		//startActivity(Description.class, pass);
	}
	
	@Override
	protected void parseWebpage(Document doc) throws HiddenColumnsException, JbmnplsParsingException{
		Element activeApps = parseAppsTable(doc, TABLE_ID[0]);
		Element allApps = parseAppsTable(doc, TABLE_ID[1]);
		
		if (activeApps == null) {
			throw new JbmnplsParsingException("Cannot parse '" + TABLE_ID[0] + "' in Applications.");
		}
		if (allApps == null) {
			throw new JbmnplsParsingException("Cannot parse '" + TABLE_ID[1] + "' in Applications.");
		}
		Elements activeRows = activeApps.getElementsByTag("tr");
		int activeNumRows = activeRows.size();
		Elements allRows = allApps.getElementsByTag("tr");
		int allNumRows = allRows.size();
		
		//Parse active apps
		Element header = activeRows.get(0);
		Job job;
		activeJobs = new ArrayList<Integer>(allNumRows);
		if (header.getElementsByTag("th").size() != TABLE_NUM_COLS[0]) {
			throw new HiddenColumnsException();
		}
		for (int i = 1; i < activeNumRows; i++) {
			Element rowEl = activeRows.get(i);
			Elements tds = rowEl.getElementsByTag("td");
			
			// See if table is empty
			String str_id = getTextFromElement(tds.get(0));
			if (str_id.length() == 0) {
				break;
			}
			int id 				= Integer.parseInt(str_id);
			String title		= getTextFromElement(tds.get(1));
			String employer 	= getTextFromElement(tds.get(2));
			String term 		= getTextFromElement(tds.get(4));
			Job.STATE state = Job.STATE.getStatefromString(getTextFromElement(tds.get(5)));
			Job.STATUS status = Job.STATUS.getStatusfromString(getTextFromElement(tds.get(6)));
			Date lastDate 		= getDateFromElement(tds.get(8));
			int numApps 		= getIntFromElement	(tds.get(9));
			job = new Job(id, title, employer, term, state, status, lastDate, numApps);
			activeJobs.add(id);
			jobs.put(id, job);
			jobService.addJob(job);
		}
		
		//	Parse All apps
		header = allRows.get(0);
		allJobs = new ArrayList<Integer>(allNumRows);
		rejectedJobs = new ArrayList<Integer>(allNumRows);
		if (header.getElementsByTag("th").size() != TABLE_NUM_COLS[1]) {
			throw new HiddenColumnsException();
		}
		for (int i = 1; i < allNumRows; i++) {
			Element rowEl = allRows.get(i);
			Elements tds = rowEl.getElementsByTag("td");

			// See if table is empty
			String str_id = getTextFromElement(tds.get(0));
			if (str_id.length() == 0) {
				break;
			}
			int id 				= Integer.parseInt(str_id);
			
			//If it is not contained, then parse and throw job id in rejectedJobs
			if (activeJobs.isEmpty() || !activeJobs.contains(id)) {
				String title		= getTextFromElement(tds.get(1));
				String employer 	= getTextFromElement(tds.get(2));
				String term 		= getTextFromElement(tds.get(4));
				Job.STATE state = Job.STATE.getStatefromString(getTextFromElement(tds.get(5)));
				Job.STATUS status = Job.STATUS.getStatusfromString(getTextFromElement(tds.get(6)));
				Date lastDate 		= getDateFromElement(tds.get(8));
				int numApps 		= getIntFromElement	(tds.get(9));
				job = new Job(id, title, employer, term, state, status, lastDate, numApps);
				jobService.addJob(job);
				
				if (status == Job.STATUS.EMPLOYED) {
					activeJobs.add(id);
				} else {
					rejectedJobs.add(id);
				}
				jobs.put(id, job);
			}
			allJobs.add(id);
		}
		updateList(allJobs);
	}

	//=================================
	//	Class Public/Protected Methods
	//=================================
	protected Element parseAppsTable(Document doc, String id) throws JbmnplsParsingException {
		try{
			return doc.getElementById(id).select("tr:eq(1) table table").first();
		} catch(Exception e) {
			throw new JbmnplsParsingException("Problem parsing Applications table.");
		}
	}
	
	protected void updateList(ArrayList<Integer> displayList) {
		adapter = new JobListAdapter(this, R.id.job_list, displayList);
		adapter.notifyDataSetChanged();
		list.setAdapter(adapter);
	}
	
}

