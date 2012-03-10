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
import com.jobmineplus.mobile.widgets.Job;

import android.app.LocalActivityManager;
import android.content.Context;
import android.content.Intent;
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

public class Applications extends JbmnplsTabListActivityBase{
	
	//======================
	// 	Declaration Objects
	//======================
	protected final String[] TABLE_ID = {"UW_CO_STU_APPSV$scroll$0", "UW_CO_APPS_VW2$scrolli$0"};
	protected final short[] TABLE_NUM_COLS = {10, 12};
	
	private static class LISTS {
		public static String ALL_JOBS = "all";
		public static String ACTIVE_JOBS = "active";
		public static String REJECTED_JOBS = "rejected";
	}
	
	//====================
	// 	Override Methods
	//====================
	
	@Override
	protected String setUp(Bundle savedInstanceState) {
		setContentView(R.layout.applications);
//    	return JbmnplsHttpService.GET_LINKS.APPLICATIONS;
    	return JbmnplsHttpService.GET_FAKE_LINKS.APPLICATIONS;
	
	}
	@Override
	protected void defineUI() {
    	createTab(LISTS.ALL_JOBS, "All");
    	createTab(LISTS.ACTIVE_JOBS, "Active");
    	createTab(LISTS.REJECTED_JOBS, "Rejected");
    	
		super.defineUI();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		int jobId = getListByTabId(getCurrentTab()).get(arg2);
		BasicNameValuePair pass = new BasicNameValuePair("jobId", Integer.toString(jobId));
		startActivity(Description.class, pass);
	}
	
	@Override
	protected void parseWebpage(Document doc) throws HiddenColumnsException, JbmnplsParsingException{
		Element activeApps = parseTableById(doc, TABLE_ID[0]);
		Element allApps = parseTableById(doc, TABLE_ID[1]);
		
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
		clearListByTabId(LISTS.ACTIVE_JOBS);
		Element header = activeRows.get(0);
		Job job = null;
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
			addJobToListByTabId(LISTS.ACTIVE_JOBS, job);
		}
		
		//	Parse All apps
		header = allRows.get(0);
		clearListByTabId(LISTS.ALL_JOBS);
		clearListByTabId(LISTS.REJECTED_JOBS);
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
			if (isListEmpty(LISTS.ACTIVE_JOBS) || !listContainsId(LISTS.ACTIVE_JOBS, id)) {
				String title		= getTextFromElement(tds.get(1));
				String employer 	= getTextFromElement(tds.get(2));
				String term 		= getTextFromElement(tds.get(4));
				Job.STATE state = Job.STATE.getStatefromString(getTextFromElement(tds.get(5)));
				Job.STATUS status = Job.STATUS.getStatusfromString(getTextFromElement(tds.get(6)));
				Date lastDate 		= getDateFromElement(tds.get(8));
				int numApps 		= getIntFromElement	(tds.get(9));
				job = new Job(id, title, employer, term, state, status, lastDate, numApps);
				app.addJob(job);
				
				if (status == Job.STATUS.EMPLOYED) {
					addJobToListByTabId(LISTS.ACTIVE_JOBS, job);
				} else {
					addJobToListByTabId(LISTS.REJECTED_JOBS, job);
				}
			}
			addJobToService(job);
			addJobToListByTabId(LISTS.ALL_JOBS, job);
		}
		updateList(LISTS.ALL_JOBS);
	}
}

