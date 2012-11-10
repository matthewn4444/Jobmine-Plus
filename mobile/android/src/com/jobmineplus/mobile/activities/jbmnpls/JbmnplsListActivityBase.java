package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;

import android.os.Bundle;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.jobmineplus.mobile.exceptions.JbmnplsException;
import com.jobmineplus.mobile.widgets.Job;

public abstract class JbmnplsListActivityBase extends JbmnplsActivityBase implements OnItemClickListener{
    
    //=================
    //  Declarations
    //=================
    private ListView list;
    private ArrayAdapter<Integer> adapter;
    protected ArrayList<Integer> allJobs;
    
    //====================
    //  Override Methods
    //====================
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        list = (ListView) findViewById(android.R.id.list);
        list.setOnItemClickListener(this);
        allJobs = new ArrayList<Integer>();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    
    //=================================
    //  Class Public/Protected Methods
    //=================================
    
    /**
     * If you are not using the Job object to be displayed on the
     * page, please extend JobListAdapter and place the adapter
     * on setUp();
     * @param adapter
     */
    protected void setAdapter(ArrayAdapter<Integer> newAdapter) {
        adapter = newAdapter; 
    }
    
    protected void updateList() throws JbmnplsException{
        if (adapter == null) {
            throw new JbmnplsException("You have not set an adapter yet. Please use setAdapter(adapter).");
        }
        adapter.notifyDataSetChanged();
        list.setAdapter(adapter);
    }
    
    protected void addJob(Job job) {
        int id = job.getId();
        //app.addJob(job);
        jobDataSource.addJob(job);
        allJobs.add(id);
    }
    
    protected ArrayList<Integer> getList() {
        return allJobs;
    }
    
    protected void clearList() {
        allJobs.clear();
    }
    
    protected void onRequestComplete() {
        updateList();
    }
}
