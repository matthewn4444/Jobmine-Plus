package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.HashMap;

import android.os.Bundle;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.StopWatch;

public abstract class JbmnplsTabListActivityBase extends JbmnplsTabActivityBase implements OnItemClickListener{

    //======================
    //  Declaration Objects
    //======================
    private HashMap<String, ArrayList<Job>> lists;
    private ArrayList<Job> allJobs;
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
        allJobs = new ArrayList<Job>();
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
        StopWatch sw = new StopWatch(true);
        jobsToDatabase(allJobs);
        Toast.makeText(this, sw.elapsed() + " ms for db", Toast.LENGTH_SHORT).show();
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
        return theList.contains(id);
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

    protected void addJob(Job job) {
        allJobs.add(job);
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
