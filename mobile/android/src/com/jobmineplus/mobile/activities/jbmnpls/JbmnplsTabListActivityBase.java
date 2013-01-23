package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.os.Bundle;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.jobmineplus.mobile.widgets.Job;

public abstract class JbmnplsTabListActivityBase extends JbmnplsTabActivityBase implements OnItemClickListener{

    //======================
    //  Declaration Objects
    //======================
    private HashMap<String, ArrayList<Job>> lists;
    private ArrayAdapter<Job> adapter;
    private ListView list;
    private String currentTab;

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
        list = (ListView) findViewById(android.R.id.list);
        list.setOnItemClickListener(this);
        lists = new HashMap<String, ArrayList<Job>>();
        super.defineUI(savedInstanceState);
    }

    @Override
    protected void setUpTab(TabInfo tab) {
        clearListByTabId(tab.getTag());
        super.setUpTab(tab);
    };

    @Override
    public void onTabSwitched(String tag) {
        updateList(tag);
    }

    @Override
    protected void onRequestComplete() {
        updateList(currentTab);
        jobsToDatabase();
    }

    /**
     * Instead of running a request to get a job list for each tab, we ask the data source
     * to do it for us (1 database request). Look at getJobsMap for more info.
     */
    @Override
    protected void doOffine() {
        HashMap<String, ArrayList<Integer>> idMap = pageDataSource.getJobsIdMap(pageName);
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
            pageDataSource.addPage(pageName, lists, timestamp);
        }
        return null;
    }

    //====================================
    //  Class Public/Protected Methods
    //====================================

    protected void createTab(String tag, String displayName) {
        super.createTab(tag, displayName, list);
    }

    protected String getCurrentTab() {
        return currentTab;
    }

    protected void updateList(String tag) {
        currentTab = tag;
        adapter = makeAdapterFromList( getListByTabId(tag) );
        adapter.notifyDataSetChanged();
        list.setAdapter(adapter);
    }

    //======================
    //  List Transactions
    //======================

    protected boolean listContainsId(String tag, int id) {
        ArrayList<Job> theList = lists.get(tag);
        for (Job job: theList) {
            if (job.getId() == id) {
                return true;
            }
        }
        return false;
    }

    protected boolean isListEmpty(String tag) {
        return lists.get(tag).isEmpty();
    }

    protected ArrayList<Job> getListByTabId(String tag) {
        return lists.get(tag);
    }

    protected void addJobToListByTabId(String tag, Job job) {
        ArrayList<Job> theList = lists.get(tag);
        if (theList != null) {
            theList.add(job);
        }
    }

    protected void clearListByTabId(String tag) {
        lists.put(tag, new ArrayList<Job>());
    }

    protected void clearAllLists() {
        for (String key: lists.keySet()) {
            clearListByTabId(key);
        }
    }
}
