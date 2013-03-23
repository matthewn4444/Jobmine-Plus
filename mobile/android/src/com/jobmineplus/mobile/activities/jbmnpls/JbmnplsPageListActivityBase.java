package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

import com.jobmineplus.mobile.widgets.Job;

public abstract class JbmnplsPageListActivityBase extends JbmnplsPageActivityBase implements OnItemClickListener {

    private HashMap<String, ArrayList<Job>> lists;

    //====================
    //  Abstract Methods
    //====================
    /**
     * You will return an ArrayAdapter of integers so that when updating lists
     * it will update use the correct adapter for each tabs
     * @param list: given the list to put into the new ArrayAdapter class
     * @return: the array adapter that is suppose to be made for the current tab
     */
    protected abstract ArrayAdapter<Job> makeAdapterFromList (ArrayList<Job> list);

    //====================
    //  Override Methods
    //====================
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        lists = new HashMap<String, ArrayList<Job>>();
        super.defineUI(savedInstanceState);
    }

    @Override
    protected void onRequestComplete() {
        updateLists();
        jobsToDatabase();
    }

    @Override
    public void onPageSelected(int index) {
        super.onPageSelected(index);
        getFragment(index).getListView().setOnItemClickListener(this);
    }

    /**
     * Instead of running a request to get a job list for each tab, we ask the data source
     * to do it for us (1 database request). Look at getJobsMap for more info.
     */
    @Override
    protected void doOffine() {
        HashMap<String, ArrayList<Integer>> idMap =
                pageDataSource.getJobsIdMap(client.getUsername(), pageName);
        if (idMap != null) {
            HashMap<String, ArrayList<Job>> retList = jobDataSource.getJobsMap(idMap);
            if (retList != null) {
                lists = retList;

                // Make the job list
                HashSet<Integer> ids = new HashSet<Integer>();
                for (String tag : lists.keySet()) {
                    ArrayList<Job> jobs = lists.get(tag);
                    if (!jobs.isEmpty()) {
                        for (Job job : jobs) {
                            if (!ids.contains(job.getId())) {
                                ids.add(job.getId());
                                allJobs.add(job);
                            }
                        }
                    }
                }
            }
        }
    }


    @Override
    public Void doPutTask() {
        jobDataSource.addJobs(allJobs);
        if (pageName != null) {
            pageDataSource.addPage(client.getUsername(), pageName, lists, timestamp);
        }
        return null;
    }

    //====================
    //  List Alters
    //====================
    public void clearAllLists() {
        for (String tag : lists.keySet()) {
            lists.get(tag).clear();
        }
    }

    protected void createTab(String displayName) {
        ListFragment frag = new ListFragment();
        ArrayList<Job> list = new ArrayList<Job>();
        ArrayAdapter<Job> adapter = makeAdapterFromList(list);
        frag.setListAdapter(adapter);
        super.createTab(displayName, frag);
        lists.put(displayName, list);
    }

    public void updateLists() {
        for (String tag : lists.keySet()) {
            ListFragment frag = (ListFragment) getFragment(tag);
            ArrayAdapter<Job> adapter = makeAdapterFromList(lists.get(tag));
            frag.setListAdapter(adapter);
        }
        getFragment(0).getListView().setOnItemClickListener(this);
    }

    public void addJobToListByTabId(String displayName, Job job) {
        lists.get(displayName).add(job);
    }

    //====================
    //  Accessors
    //====================
    public ListFragment getCurrentFragment() {
        return (ListFragment) super.getCurrentFragment();
    }

    public ListFragment getFragment(int index) {
        return (ListFragment) super.getFragment(index);
    }

    public ArrayList<Job> getListByTab(String displayName) {
        return lists.get(displayName);
    }

    public ArrayList<Job> getCurrentList() {
        return lists.get(getCurrentTabName());
    }

    public boolean listContainsId(String displayName, int id) {
        return getListByTab(displayName).contains(id);
    }
}
