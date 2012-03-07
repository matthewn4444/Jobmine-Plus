package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.HashMap;

import android.view.View;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.jobmineplus.mobile.activities.jbmnpls.JbmnplsActivityBase.JobListAdapter;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.Job;
import org.apache.http.NameValuePair;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public abstract class JbmnplsTabListActivityBase extends JbmnplsTabActivityBase implements OnItemClickListener{
	
	//======================
	// 	Declaration Objects
	//======================
	private HashMap<String, ArrayList<Integer>> lists;
	private JobListAdapter adapter;
	private ListView list;
	private String currentTab;
	
	//====================
	// 	Override Methods
	//====================
	
	@Override 
	protected void defineUI() {
		list = (ListView) findViewById(android.R.id.list);
		list.setOnItemClickListener(this);
		super.defineUI();
	}
	
	@Override 
	protected void setUpTabs() {
		lists = new HashMap<String, ArrayList<Integer>>(tabInfo.size());
		for(NameValuePair pair: tabInfo) {
			clearListByTabId(pair.getName());
		}
		super.setUpTabs();
	}
	
	@Override
	public View onTabSwitched(String tag) {
		updateList(tag);
		return list;
	}
	
	//=================================
	//	Class Public/Protected Methods
	//=================================
	
	protected String getCurrentTab() {
		return currentTab;
	}
	
	protected void updateList(String tag) {
		currentTab = tag;
		adapter = new JobListAdapter(this, android.R.id.list, lists.get(tag));
		adapter.notifyDataSetChanged();
		list.setAdapter(adapter);
	}

	//======================
	//	List Transactions
	//======================
	
	protected boolean listContainsId(String tag, int id) {
		ArrayList<Integer> theList = lists.get(tag);
		return theList.contains(id);
	}
	
	protected boolean isListEmpty(String tag) {
		return lists.get(tag).isEmpty();
	}
	
	protected ArrayList<Integer> getListByTabId(String tag) {
		return lists.get(tag);
	}
	
	protected void addJobToListByTabId(String tag, Job job) {
		ArrayList<Integer> theList = lists.get(tag);
		if (theList != null) {
			theList.add(job.getId());
		}
	}
	
	protected void addJobToService(Job job) {
		jobService.addJob(job);
	}
	
	protected void clearListByTabId(String id) {
		lists.put(id, new ArrayList<Integer>());
	}
}
