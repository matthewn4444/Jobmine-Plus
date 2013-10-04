package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.internal.view.menu.SubMenuBuilder;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.database.pages.PageResult;
import com.jobmineplus.mobile.exceptions.JbmnplsException;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.Job.HEADER;
import com.jobmineplus.mobile.widgets.Job.HeaderComparator;
import com.jobmineplus.mobile.widgets.Job.HeaderComparator.DIRECTION;

public abstract class JbmnplsListActivityBase extends JbmnplsActivityBase implements OnItemClickListener{

    //=================
    //  Declarations
    //=================
    private ListView list;
    private TextView emptyText;
    private JbmnplsAdapterBase adapter;
    private MenuItem sortSelected;
    private boolean sortedAscending = false;
    private HeaderComparator sortComparer = new Job.HeaderComparator();

    //====================
    //  Abstract Methods
    //====================
    public abstract HEADER[] getTableHeaders();

    public abstract JbmnplsAdapterBase getAdapter();

    //====================
    //  Override Methods
    //====================
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        if (findViewById(android.R.id.empty) == null) {
            setContentView(R.layout.joblist);
        }
        list = (ListView) findViewById(R.id.list);
        emptyText = (TextView) findViewById(android.R.id.empty);
        list.setVisibility(View.INVISIBLE);
        list.setEmptyView(emptyText);
        list.setOnItemClickListener(this);

        // Set up the adapter
        adapter = getAdapter();
        list.setAdapter(adapter);
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

    //=================================
    //  Sorting
    //=================================
    protected void sort(HEADER header) {
        sort(header, true);
    }

    protected void sort(HEADER header, boolean ascend) {
        sortComparer.setHeader(header);
        sortComparer.setDirection(ascend ? DIRECTION.ASCEND : DIRECTION.DESCEND);
        adapter.sort(sortComparer);
    }

    //=================================
    //  Class Public/Protected Methods
    //=================================
    protected void updateList() throws JbmnplsException{
        adapter.notifyDataSetChanged();
        if (!adapter.isEmpty()) {
            list.setVisibility(View.VISIBLE);
        }
    }

    protected void scrollToTop() {
        list.setSelectionAfterHeaderView();
    }

    protected ArrayList<Job> getList() {
        return allJobs;
    }

    protected void clearList() {
        allJobs.clear();
    }
}
