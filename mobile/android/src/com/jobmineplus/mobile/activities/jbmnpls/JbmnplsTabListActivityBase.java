package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.HashMap;

import android.os.Bundle;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.jobmineplus.mobile.widgets.Job;

public abstract class JbmnplsTabListActivityBase extends JbmnplsTabActivityBase implements OnItemClickListener{
    
    //======================
    //  Declaration Objects
    //======================
    private HashMap<String, ArrayList<Integer>> lists;
    private ArrayAdapter<Integer> adapter;
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
    protected abstract ArrayAdapter<Integer> makeAdapterFromList (ArrayList<Integer> list);
    
    //====================
    //  Override Methods
    //====================
    
    @Override 
    protected void defineUI(Bundle savedInstanceState) {
        list = (ListView) findViewById(android.R.id.list);
        list.setOnItemClickListener(this);
        lists = new HashMap<String, ArrayList<Integer>>();
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
        app.addJob(job);
    }
    
    protected void clearListByTabId(String tag) {
        lists.put(tag, new ArrayList<Integer>());
    }
    
    protected void clearAllLists() {
        for (String key: lists.keySet()) {
            clearListByTabId(key);
        }
    }
}