package com.jobmineplus.mobile.activities.jbmnpls;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.message.BasicNameValuePair;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.jbmnpls.JbmnplsActivityBase.JobListAdapter;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.Job;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public abstract class JbmnplsListActivityBase extends JbmnplsActivityBase implements OnItemClickListener{
	
	//=================
	// 	Declarations
	//=================
	private ListView list;
	protected JobListAdapter adapter;
	protected ArrayList<Integer> allJobs;
	
	//====================
	//	Abstract Methods
	//====================
//	@Override
//	public abstract void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3);
	
	//====================
	// 	Override Methods
	//====================
	@Override
	protected void defineUI() {
		list = (ListView) findViewById(android.R.id.list);
		list.setOnItemClickListener(this);
		allJobs = new ArrayList<Integer>();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}
	
	//=================================
	//	Class Public/Protected Methods
	//=================================
	protected void updateList(ArrayList<Integer> displayList) {
		adapter = new JobListAdapter(this, android.R.id.list, displayList);
		adapter.notifyDataSetChanged();
		list.setAdapter(adapter);
	}
	
	protected void addJob(Job job) {
		int id = job.getId();
		jobService.addJob(job);
		allJobs.add(id);
	}
}
