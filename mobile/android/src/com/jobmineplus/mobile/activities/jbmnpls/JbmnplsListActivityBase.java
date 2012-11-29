package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;

import android.os.Bundle;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.jobmineplus.mobile.exceptions.JbmnplsException;
import com.jobmineplus.mobile.widgets.Job;

public abstract class JbmnplsListActivityBase extends JbmnplsActivityBase implements OnItemClickListener{

    /**
     * TODO after adding jobs, you can try to place them in database
     * on another thread, make it 400ms faster
     *
     */

    //=================
    //  Declarations
    //=================
    private ListView list;
    private ArrayAdapter<Job> adapter;

    //====================
    //  Override Methods
    //====================
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        list = (ListView) findViewById(android.R.id.list);
        list.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void doOffine() {
        int[] ids = pageDataSource.getJobsIds(pageName);
        allJobs.clear();

        if (ids != null) {
            ArrayList<Job> jobs = jobDataSource.getJobsByIdList(ids);
            for (Job job : jobs) {
                allJobs.add(job);
            }
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
    protected void setAdapter(ArrayAdapter<Job> newAdapter) {
        adapter = newAdapter;
    }

    protected void updateList() throws JbmnplsException{
        if (adapter == null) {
            throw new JbmnplsException("You have not set an adapter yet. Please use setAdapter(adapter).");
        }
        adapter.notifyDataSetChanged();
        list.setAdapter(adapter);
    }

    protected ArrayList<Job> getList() {
        return allJobs;
    }

    protected void clearList() {
        allJobs.clear();
    }

    @Override
    protected void onRequestComplete() {
        updateList();
        jobsToDatabase();
    }
}
