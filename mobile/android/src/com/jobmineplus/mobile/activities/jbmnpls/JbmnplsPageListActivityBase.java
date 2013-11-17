package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import junit.framework.Assert;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.internal.view.menu.SubMenuBuilder;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.database.pages.PageMapResult;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase.HIGHLIGHTING;
import com.jobmineplus.mobile.widgets.JbmnplsLoadingAdapterBase;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.Job.HEADER;
import com.jobmineplus.mobile.widgets.Job.HeaderComparator.DIRECTION;

public abstract class JbmnplsPageListActivityBase extends JbmnplsPageActivityBase implements OnItemClickListener {

    private HashMap<String, ArrayList<Job>> lists;

    private static final String DISPLAYNAME = "displayname";
    private final Job.HeaderComparator comparer = new Job.HeaderComparator();
    private MenuItem sortSelected;
    private boolean sortedAscending = false;

    //====================
    //  Abstract Methods
    //====================
    public abstract HEADER[] getTableHeaders();

    public abstract int[] getJobListItemResources();

    protected abstract HIGHLIGHTING formatJobListItem(int position, Job job, View[] elements, View layout);

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
                    if (ids != null) {
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
        allJobs.clear();
    }

    protected void scrollToTop() {
        for (String tag : lists.keySet()) {
            ((PageListFragment)getFragment(tag)).scrollToTop();
        }
    }

    protected void createTab(String displayName) {
        PageListFragment frag = PageListFragment.newInstance();
        Bundle b = new Bundle();
        b.putString(DISPLAYNAME, displayName);
        frag.setArguments(b);

        // Set thew new adapter
        ArrayList<Job> list = new ArrayList<Job>();
        JbmnplsAdapterBase adapter = new JbmnplsPageListAdapter(this, list);
        frag.setPageListAdapter(adapter);
        super.createTab(displayName, frag);
        lists.put(displayName, list);
    }

    public void updateLists() {
        for (String tag : lists.keySet()) {
            PageListFragment frag = (PageListFragment) getFragment(tag);
            frag.showEmptyText();
            frag.notifyListDataSetChanged();
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
            ((PageListFragment) getFragment(tag)).getPageListAdapter().sort(comparer);
        }
    }

    protected void resetSortingMenu() {
        if (sortSelected != null) {
            sortSelected.setTitle(" " + ((String)sortSelected.getTitle()).substring(1));
        }
        sortedAscending = false;
    }

    //====================
    //  Accessors
    //====================
    @Override
    public ListFragment getCurrentFragment() {
        return (ListFragment) super.getCurrentFragment();
    }

    @Override
    public ListFragment getFragment(int index) {
        return (ListFragment) super.getFragment(index);
    }

    public ListView getListViewByTab(String displayName) {
        return ((PageListFragment)getFragment(displayName)).getListView();
    }

    public ListView getListViewByIndex(int index) {
        return ((PageListFragment)getFragment(index)).getListView();
    }

    public ListView getCurrentListView() {
        return getListViewByIndex(getCurrentIndex());
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

    public JbmnplsAdapterBase getAdapterByTab(String displayName) {
        return ((PageListFragment)getFragment(displayName)).getPageListAdapter();
    }

    public JbmnplsAdapterBase getAdapterByIndex(int index) {
        return ((PageListFragment)getFragment(index)).getPageListAdapter();
    }

    //========================
    //  Generic List Adapter
    //========================
    protected int getJobListItemLayout() {
        return R.layout.job_widget;
    }

    private class JbmnplsPageListAdapter extends JbmnplsLoadingAdapterBase {
        public JbmnplsPageListAdapter(JbmnplsPageListActivityBase a, ArrayList<Job> list) {
            super(a, getJobListItemLayout(), getJobListItemResources(), list);
        }

        @Override
        protected HIGHLIGHTING setJobWidgetValues(int position, Job job, View[] elements, View layout) {
            return formatJobListItem(position, job, elements, layout);
        }
    }

    //============================
    //  Custom ListFragment Class
    //============================
    public final static class PageListFragment extends ListFragment {
        private boolean showEmptyText = false;
        private boolean listIsAvailable = false;
        public String displayName;
        private JbmnplsAdapterBase listAdapter;

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

        public void notifyListDataSetChanged() {
            listAdapter.notifyDataSetChanged();
        }

        public void setPageListAdapter(JbmnplsAdapterBase adapter) {
            listAdapter = adapter;
            super.setListAdapter(adapter);
        }

        public JbmnplsAdapterBase getPageListAdapter() {
            return listAdapter;
        }

        public void scrollToTop() {
            ListView view = getListView();
            if (view != null) {
                view.setSelectionAfterHeaderView();
            }
        }

        @Override
        public ListView getListView() {
            if (listIsAvailable) {
                return super.getListView();
            } else {
                return null;
            }
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            listIsAvailable = false;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            Bundle b = getArguments();
            setListShown(true);
            listIsAvailable = true;
            Assert.assertNotNull(b);
            displayName = b.getString(DISPLAYNAME);
            JbmnplsPageListActivityBase a = (JbmnplsPageListActivityBase)getActivity();
            a.createTab(displayName, this);
            super.getListView().setOnItemClickListener(a);
            super.getListView().setAdapter(listAdapter);
            if (showEmptyText) {
                setEmptyText(getString(R.string.empty_job_list));
            }
            super.onActivityCreated(savedInstanceState);
        }
    }
}
