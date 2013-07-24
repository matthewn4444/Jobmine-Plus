package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import junit.framework.Assert;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.internal.view.menu.SubMenuBuilder;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.database.pages.PageMapResult;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.Job.HEADER;
import com.jobmineplus.mobile.widgets.Job.HeaderComparator.DIRECTION;

public abstract class JbmnplsPageListActivityBase extends JbmnplsPageActivityBase implements OnItemClickListener {

    private HashMap<String, ArrayList<Job>> lists;

    private static final String DISPLAYNAME = "displayname";
    private Job.HeaderComparator comparer = new Job.HeaderComparator();
    private MenuItem sortSelected;
    private boolean sortedAscending = false;

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

    public abstract HEADER[] getTableHeaders();

    //====================
    //  Override Methods
    //====================
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        lists = new HashMap<String, ArrayList<Job>>();
        super.defineUI(savedInstanceState);
    }

    @Override
    protected void onRequestComplete(boolean pullData) {
        updateLists();
        if (pullData) {
            jobsToDatabase();
        }
    }

    /**
     * Instead of running a request to get a job list for each tab, we ask the data source
     * to do it for us (1 database request). Look at getJobsMap for more info.
     */
    @Override
    protected long doOffine() {
        PageMapResult result =
                pageDataSource.getPageDataMap(client.getUsername(), pageName);
        if (result != null) {
            HashMap<String, ArrayList<Integer>> idMap = result.idMap;
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
            return result.timestamp;
        }
        return 0;
    }

    @Override
    public Long doPutTask() {
        jobDataSource.addJobs(allJobs);
        if (pageName != null) {
            pageDataSource.addPage(client.getUsername(), pageName, lists, timestamp);
        }
        return null;
    }

    @Override
    protected int getActionBarId() {
        return R.menu.actionbar_with_sort;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       boolean flag = super.onCreateOptionsMenu(menu);
       MenuItem item = menu.findItem(R.id.action_sort);
       SubMenu sub = item.getSubMenu();
       sub.clear();

       // Because SherlockActionBar renders text differently, the tabbing is different
       String prefex = sub instanceof SubMenuBuilder ? " \t" : " \t\t";
       HEADER[] headers = getTableHeaders();
       for (int i = 0; i < headers.length; i++) {
           HEADER header = headers[i];
           sub.add(1, i, Menu.NONE, prefex + header.readable());
       }
       return flag;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        HEADER[] headers = getTableHeaders();
        if (id >= 0 && id < headers.length) {
            if (sortSelected != null && item != sortSelected) {
                sortSelected.setTitle(" " + ((String)sortSelected.getTitle()).substring(1));
                sortedAscending = true;
            } else {
                sortedAscending = !sortedAscending;
            }
            item.setTitle((sortedAscending ? "£" : "¥") + ((String)item.getTitle()).substring(1));
            sort(headers[id], sortedAscending);
            sortSelected = item;
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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
        PageListFragment frag = PageListFragment.newInstance();
        Bundle b = new Bundle();
        b.putString(DISPLAYNAME, displayName);
        frag.setArguments(b);
        ArrayList<Job> list = new ArrayList<Job>();
        ArrayAdapter<Job> adapter = makeAdapterFromList(list);
        frag.setListAdapter(adapter);
        super.createTab(displayName, frag);
        lists.put(displayName, list);
    }

    public void updateLists() {
        for (String tag : lists.keySet()) {
            PageListFragment frag = (PageListFragment) getFragment(tag);
            frag.showEmptyText();
            ArrayAdapter<Job> adapter = makeAdapterFromList(lists.get(tag));
            frag.setArrayAdapter(adapter);
        }
    }

    public void addJobToListByTabId(String displayName, Job job) {
        lists.get(displayName).add(job);
    }

    protected void sort(HEADER header) {
        sort(header, true);
    }

    protected void sort(HEADER header, boolean ascend) {
        // Update the current list, then the others
        comparer.setHeader(header);
        comparer.setDirection(ascend ? DIRECTION.ASCEND : DIRECTION.DESCEND);
        for (String tag : lists.keySet()) {
            ArrayAdapter<Job> adapter = ((PageListFragment) getFragment(tag)).getArrayAdapter();
            adapter.sort(comparer);
        }
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
        for (Job job : getListByTab(displayName)) {
            if (job.getId() == id) {
                return true;
            }
        }
        return false;
    }

    //============================
    //  Custom ListFragment Class
    //============================
    public final static class PageListFragment extends ListFragment {
        private boolean showEmptyText = false;
        public String displayName;
        private ArrayAdapter<Job> arrayAdapter;

        public final static PageListFragment newInstance() {
            return new PageListFragment();
        }

        // Hack that shows the text only when there is no content in the list
        public void showEmptyText() {
            showEmptyText = true;
            if (getView() != null) {
                setEmptyText(getString(R.string.empty_job_list));
            }
        }

        public void setArrayAdapter(ArrayAdapter<Job> adapter) {
            arrayAdapter = adapter;
            super.setListAdapter(adapter);
        }

        public ArrayAdapter<Job> getArrayAdapter() {
            return arrayAdapter;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            Bundle b = getArguments();
            setListShown(true);
            Assert.assertNotNull(b);
            displayName = b.getString(DISPLAYNAME);
            JbmnplsPageListActivityBase a = (JbmnplsPageListActivityBase)getActivity();
            a.createTab(displayName, this);
            getListView().setOnItemClickListener(a);
            if (showEmptyText) {
                setEmptyText(getString(R.string.empty_job_list));
            }
            super.onActivityCreated(savedInstanceState);
        }
    }


}
