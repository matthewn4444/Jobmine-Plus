package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.database.pages.PageResult;
import com.jobmineplus.mobile.exceptions.JbmnplsException;
import com.jobmineplus.mobile.widgets.Job;

public abstract class JbmnplsListActivityBase extends JbmnplsActivityBase implements OnItemClickListener{

    //=================
    //  Declarations
    //=================
    private ListView list;
    private TextView emptyText;
    private ArrayAdapter<Job> adapter;

    //====================
    //  Override Methods
    //====================
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        setContentView(R.layout.joblist);
        list = (ListView) findViewById(R.id.list);
        emptyText = (TextView) findViewById(android.R.id.empty);
        list.setVisibility(View.INVISIBLE);
        list.setEmptyView(emptyText);
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
    protected long doOffine() {
        PageResult result = pageDataSource.getPageData(client.getUsername(), pageName);
        if (result != null) {
            ArrayList<Integer> ids = result.ids;
            allJobs.clear();

            if (ids != null) {
                ArrayList<Job> jobs = jobDataSource.getJobsByIdList(ids);
                for (Job job : jobs) {
                    allJobs.add(job);
                }
            }
            return result.timestamp;
        }
        return 0;
    }

    @Override
    protected void onRequestComplete(boolean pullData) {
        updateList();
        emptyText.setVisibility(View.VISIBLE);
        if (pullData) {
            jobsToDatabase();
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
        if (!adapter.isEmpty()) {
            list.setVisibility(View.VISIBLE);
        }
    }

    protected ArrayList<Job> getList() {
        return allJobs;
    }

    protected void clearList() {
        allJobs.clear();
    }
}
