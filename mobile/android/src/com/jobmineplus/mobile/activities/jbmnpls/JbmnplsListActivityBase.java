package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;

import android.os.Bundle;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.jobmineplus.mobile.widgets.Job;

public abstract class JbmnplsListActivityBase extends JbmnplsActivityBase implements OnItemClickListener{
	
	//=================
	// 	Declarations
	//=================
	private ListView list;
	protected JobListAdapter adapter;
	protected ArrayList<Integer> allJobs;
	
	//====================
	// 	Override Methods
	//====================
	@Override
	protected void defineUI(Bundle savedInstanceState) {
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
		app.addJob(job);
		allJobs.add(id);
	}
}
