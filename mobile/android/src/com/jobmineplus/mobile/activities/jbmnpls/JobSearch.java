package com.jobmineplus.mobile.activities.jbmnpls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.SimpleActivityBase;
import com.jobmineplus.mobile.database.pages.PageResult;
import com.jobmineplus.mobile.exceptions.JbmnplsException;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.exceptions.JbmnplsLostStateException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.HeightAnimation;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase.Formatter;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase.HIGHLIGHTING;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import com.jobmineplus.mobile.widgets.JbmnplsLoadingAdapterBase;
import com.jobmineplus.mobile.widgets.JbmnplsRequestQueue;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.Job.APPLY_STATUS;
import com.jobmineplus.mobile.widgets.Job.HEADER;
import com.jobmineplus.mobile.widgets.JobSearchDialog;
import com.jobmineplus.mobile.widgets.JobSearchDialog.OnJobSearchListener;
import com.jobmineplus.mobile.widgets.JobSearchProperties;
import com.jobmineplus.mobile.widgets.ListViewPlus;
import com.jobmineplus.mobile.widgets.ListViewPlus.OnVisualRowChangeListener;
import com.jobmineplus.mobile.widgets.table.SimpleHtmlParser;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;

public class JobSearch extends JbmnplsPageListActivityBase implements
                            OnJobSearchListener, TableParser.OnTableParseListener,
                            OnScrollListener, OnClickListener, OnVisualRowChangeListener {

    //======================
    //  Declaration Objects
    //======================
    public final static String PAGE_NAME = JobSearch.class.getName();
    public final static int FETCH_MORE_REACH_BOTTOM_COUNT = 30;
    private final static int VIEW_ITEM_POSITION_KEY = R.id.VIEW_ITEM_POSITION_KEY;
    private final static int VIEW_ITEM_JOB_KEY = R.id.VIEW_ITEM_JOB_KEY;
    private final static int REQUEST_CODE_DESCRIPTION = 1;

    // Tab Names
    public final static class PAGES {
        final public static String NEW = "New";
        final public static String READ = "Read";
        final public static String SHORTLIST = "Shortlisted";
        final public static String APPLIED = "Already Applied";
        final public static String ALL = "All Jobs";
    }

    // Task Queue
    private SearchRequestQueue taskQueue;

    // Shortlist data structures
    private final SparseIntArray jobPageArray = new SparseIntArray(200);
    private Set<Integer> shortlistSet;
    private List<Integer> readOrNewList;
    private List<Job> readOrNewJobList;

    // Search dialog and its properties
    private JobSearchProperties properties;
    private JobSearchDialog searchDialog;

    // Job Search states
    private boolean enableSearch = true;
    private boolean firstSearch;
    private boolean allJobsLoaded;

    // Current list position
    private int currentListPosition;

    // Alert dialog
    private AlertDialog.Builder alert;

    // Sorting variables
    private HEADER choseSortHeader;
    private boolean choseSortAscend;

    // Errors
    private static enum Error {
        INACTIVE, LOST_STATE, LOGGED_OUT, NONE
    };
    private Error stateError;

    private RowAnimation rowAnimation;

    // Shortlisting
    private Job currentShortlistedJob;
    private String shortlistedTab;
    private boolean enableShortlisting;

    private int numItemsFitInListView;
    private boolean showGrowAnimation;

    private View readView;

    public final static HEADER[] SORT_HEADERS = {
        HEADER.JOB_TITLE,
        HEADER.EMPLOYER_NAME,
        HEADER.LOCATION,
        HEADER.OPENINGS,
        HEADER.LAST_DAY_TO_APPLY,
        HEADER.NUM_APPS
    };

    public static final TableParserOutline JOBSEARCH_OUTLINE =
        new TableParserOutline("UW_CO_JOBRES_VW$scroll$0",
                HEADER.JOB_IDENTIFIER,
                HEADER.JOB_TITLE,
                HEADER.EMPLOYER_NAME,
                HEADER.UNIT_NAME,
                HEADER.LOCATION,
                HEADER.OPENINGS,
                HEADER.APPLY,
                HEADER.SHORTLIST,
                HEADER.NUM_APPS,
                HEADER.LAST_DAY_TO_APPLY);

    protected final int[] WIDGET_RESOURCE_LIST = {
            R.id.job_title, R.id.job_employer, R.id.location,
            R.id.job_last_day, R.id.job_num_app_openings, R.id.star, R.id.loading };

    //======================
    //  Overrided Methods
    //======================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alert = new Builder(this);
        alert.setNegativeButton(android.R.string.ok, null);
        alert.create();
        taskQueue = new SearchRequestQueue(this, null, null);       // Initially, it will actually get set in parseWebpage()
        firstSearch = true;
        currentListPosition = 0;
        allJobsLoaded = false;
        enableShortlisting = true;
        numItemsFitInListView = 1;
        stateError = Error.NONE;
        rowAnimation = new RowAnimation();
        setOnRowChangeListener(this);

        // Add the page tabs
        createTab(PAGES.NEW);
        createTab(PAGES.READ);
        createTab(PAGES.SHORTLIST);
        createTab(PAGES.APPLIED);
        createTab(PAGES.ALL);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (rowAnimation.isRunning()) {
            return;
        }

        Job job = getCurrentList().get(position);
        int jobId = job.getId();
        if (!job.hasDescriptionData() && isReallyOnline()) {
            taskQueue.addTask(SearchRequestQueue.DESCRIPTION);
        }

        // Deal with reading new jobs from new or all pages
        String currentTab = getCurrentTabName();
        if (currentTab == PAGES.NEW) {
            readView = view;
        } else if (currentTab == PAGES.ALL) {
            // Search for the job in the new page, if exists, then transfer it from new to read
            JbmnplsAdapterBase newAdapter = getAdapterByTab(PAGES.NEW);
            int pos = newAdapter.getJobPosition(jobId);
            if (pos != -1) {
                log("transfered", job.getEmployer());
                JbmnplsAdapterBase readAdapter = getAdapterByTab(PAGES.READ);
                readAdapter.add(job);
                newAdapter.remove(pos);
            }
        }

        goToDescription(jobId);
    }

    @Override
    public HEADER[] getTableHeaders() {
        return SORT_HEADERS;
    }

    @Override
    protected String setUp(Bundle savedInstanceState) {
        pageName = PAGE_NAME;
        return getUrl();
    }

    @Override
    public int[] getJobListItemResources() {
        return WIDGET_RESOURCE_LIST;
    }

    @Override
    protected int getActionBarId() {
        return R.menu.actionbar_job_search;
    }

    @Override
    protected void requestData() throws RuntimeException {
        super.requestData();
        firstSearch = true;
        clearAllLists();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search && canSearch()) {
            showSearchDialog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void parseWebpage(String html) {
        properties = new JobSearchProperties();

        /*
         * Parse the original job search properties
         */
        SimpleHtmlParser parser = new SimpleHtmlParser(html);

        // State Number
        parser.skipText("id='ICStateNum'");
        String stateNum = parser.getAttributeInCurrentElement("value");

        // ICSID
        parser.skipText("id='ICSID'");
        String icsID = parser.getAttributeInCurrentElement("value");

        // Term
        parser.skipText("win0divUW_CO_JOBSRCH_UW_CO_WT_SESSION", "input");
        properties.term.set(parser.getAttributeInCurrentElement("value"));

        // Job Level: Junior
        parser.skipText("win0divUW_CO_JOBSRCH_UW_CO_COOP_JR", "onclick=");
        properties.levelJunior.set(parser.getAttributeInCurrentElement("checked") != null);

        // Job Level: Bachelors
        parser.skipText("win0divUW_CO_JOBSRCH_UW_CO_BACHELOR", "onclick=");
        properties.levelBachelors.set(parser.getAttributeInCurrentElement("checked") != null);

        // Job Level: Intermediate
        parser.skipText("win0divUW_CO_JOBSRCH_UW_CO_COOP_INT", "onclick=");
        properties.levelIntermediate.set(parser.getAttributeInCurrentElement("checked") != null);

        // Job Level: Masters
        parser.skipText("win0divUW_CO_JOBSRCH_UW_CO_MASTERS", "onclick=");
        properties.levelMasters.set(parser.getAttributeInCurrentElement("checked") != null);

        // Job Level: Senior
        parser.skipText("win0divUW_CO_JOBSRCH_UW_CO_COOP_SR", "onclick=");
        properties.levelSenior.set(parser.getAttributeInCurrentElement("checked") != null);

        // Job Level: Ph.D.
        parser.skipText("win0divUW_CO_JOBSRCH_UW_CO_PHD", "onclick=");
        properties.levelPhD.set(parser.getAttributeInCurrentElement("checked") != null);

        // Employer name
        parser.skipText("win0divUW_CO_JOBSRCH_UW_CO_EMPLYR_NAME", "input");
        properties.employer.set(parser.getAttributeInCurrentElement("value"));
        if (properties.employer.get() == null) {
            throw new JbmnplsParsingException("Cannot find the value attribute for search employer");
        }

        // Job Title
        parser.skipText("UW_CO_JOBSRCH_UW_CO_JOB_TITLE", "input");
        properties.title.set(parser.getAttributeInCurrentElement("value"));
        if (properties.title.get() == null) {
            throw new JbmnplsParsingException("Cannot find the value attribute for search job title");
        }

        // Disciplines
        parser.skipText("UW_CO_JOBSRCH_UW_CO_ADV_DISCP1", "selected=");
        properties.disciplines1.set(parser.getTextInCurrentElement());

        parser.skipText("UW_CO_JOBSRCH_UW_CO_ADV_DISCP2", "selected=");
        properties.disciplines2.set(parser.getTextInCurrentElement());

        parser.skipText("UW_CO_JOBSRCH_UW_CO_ADV_DISCP3", "selected=");
        properties.disciplines3.set(parser.getTextInCurrentElement());

        // Location
        parser.skipText("win0divUW_CO_JOBSRCH_UW_CO_LOCATION", "input");
        properties.location.set(parser.getAttributeInCurrentElement("value"));
        if (properties.location.get() == null) {
            throw new JbmnplsParsingException("Cannot find the value attribute for search location");
        }

        // Job Search Filter
        parser.skipText("win0divUW_CO_JOBSRCH_UW_CO_JS_JOBSTATUS", "selected=");
        properties.filter.set(JobSearchProperties.FILTER.fromString(parser.getTextInCurrentElement()));

        // Job Type
        parser.skipText("win0divUW_CO_JOBSRCH_UW_CO_JOB_TYPE", "checked=", "label");
        properties.jobType.set(JobSearchProperties.JOBTYPE.fromString(parser.getTextInCurrentElement()));

        taskQueue = new SearchRequestQueue(this, stateNum, icsID);

        properties.acceptChanges();
    }

    @Override
    public void onRowParse(TableParserOutline outline, Object... jobData) {
        int id = (Integer)jobData[0];

        if (!jobExists(id)) {
            APPLY_STATUS appStatus = (APPLY_STATUS)jobData[6];
            boolean hasBeenPlaced = false;

            Job job = new Job(          id, (String)    jobData[1], (String)jobData[2],
                    (String)    jobData[4], (Integer)   jobData[5], appStatus,
                    (Date)      jobData[9], (Integer)   jobData[8]);

            if (appStatus == APPLY_STATUS.ALREADY_APPLIED) {
                hasBeenPlaced = true;
                addJobToListByTabId(PAGES.APPLIED, job);
            }

            // Maintain the shortlisted ids database
            if (((String)jobData[7]).equals("On Short List")) {
                shortlistSet.add(id);
                hasBeenPlaced = true;
                addJobToListByTabId(PAGES.SHORTLIST, job);
            } else {
                shortlistSet.remove(id);
            }

            // If it has not been placed in any tab, then it is new
            if (!hasBeenPlaced) {
                readOrNewList.add(id);
                readOrNewJobList.add(job);
            }

            // Place the jobs in different tabs
            addJobToListByTabId(PAGES.ALL, job);
            addJob(job);
        }
    }

    @Override
    protected void onRequestComplete(boolean pullData) {
        // Coming from HomeActivity
        if (firstSearch && pullData) {
            showSearchDialog();

            // If there was an error we will show an error
            switch (stateError) {
            case INACTIVE:
                showAlert(getString(R.string.search_inactivity));
                break;
            case LOGGED_OUT:
                showAlert(getString(R.string.search_logged_out));
                break;
            case LOST_STATE:
                showAlert(getString(R.string.search_lost_state));
                break;
            default:
                break;
            }
            stateError = Error.NONE;
        } else {
            super.onRequestComplete(pullData);
        }
    }

    protected boolean jobExists(Job job) {
        return jobExists(job.getId());
    }

    protected boolean jobExists(int id) {
        return jobPageArray.indexOfKey(id) >= 0;
    }

    @Override
    protected void addJob(Job job) {
        jobPageArray.append(job.getId(), taskQueue.getCurrentPageNumber());
        super.addJob(job);
    }

    @Override
    public void clearAllLists() {
        super.clearAllLists();
        jobPageArray.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableShortlisting(isReallyOnline());
        checkIfSearchExpired();
    }

    @Override
    protected void onDestroy() {
        taskQueue.cancelAllTasks();
        super.onDestroy();
    }

    @Override
    protected void scrollToTop() {
        currentListPosition = 0;
        super.scrollToTop();
    }

    @Override
    protected void sort(HEADER header, boolean ascend) {
        if (!allJobsLoaded && isReallyOnline()) {
            choseSortHeader = header;
            choseSortAscend = ascend;
            taskQueue.addTask(SearchRequestQueue.SORT, getString(R.string.search_sort_getting_jobs));
        } else {
            super.sort(header, ascend);
        }
    }

    @Override
    protected void onNetworkConnectionChanged(boolean connected) {
        super.onNetworkConnectionChanged(connected);
        enableShortlisting(isReallyOnline());

        showLoadingAtEnd(connected && isOnline() && !allJobsLoaded);
        setSearchEnabled(connected);
        if (connected) {
            attachScrollListener(true);
        } else {
            taskQueue.cancelAllTasks();
            attachScrollListener(false);
        }
    }

    @Override
    protected int getJobListItemLayout() {
        return R.layout.job_search_widget;
    }

    @Override
    protected long doOffine() {
        // Get the shortlist ids from the databsae
        PageResult r = pageDataSource.getPageData(client.getUsername(), Shortlist.PAGE_NAME);
        if (r != null) {
            shortlistSet = new HashSet<Integer>(r.ids);
        } else {
            shortlistSet = new HashSet<Integer>();
        }
        doneLoadingAllJobs();
        return super.doOffine();
    }

    //=================
    //  List Adapter
    //=================
    @Override
    protected HIGHLIGHTING formatJobListItem(int position, Job job,
            View[] elements, View layout) {
        layout.setTag(VIEW_ITEM_POSITION_KEY, position);
        layout.setTag(VIEW_ITEM_JOB_KEY, job);
        Formatter.setText((TextView)elements[0], job.getTitle());
        Formatter.setText((TextView)elements[1], job.getEmployer(), true);
        Formatter.setText((TextView)elements[2], job.getLocation());
        Formatter.setDate((TextView)elements[3], job.getLastDateToApply(), "Closes by");
        Formatter.setText((TextView)elements[4], "Openings: " + job.getNumberOfOpenings() + " | Apps #: " + job.getNumberOfApplications());

        // Checkbox
        CheckBox box = (CheckBox)elements[5];
        box.setOnClickListener(this);
        box.setChecked(isShortlisted(job));
        box.setEnabled(!box.isChecked() && enableShortlisting);

        View progress = elements[6];
        if (currentShortlistedJob != null && job.getId() == currentShortlistedJob.getId()) {
            box.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);
        } else {
            box.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
        }

        return HIGHLIGHTING.NORMAL;
    }

    //=======================
    //  Handle Search Icon
    //=======================
    @Override
    protected void onlineModeChanged(boolean flag) {
        setSearchEnabled(flag);
        enableShortlisting(isReallyOnline());

        // Coming in offline and going online, we need to get the new data
        if (firstSearch && flag) {
            if (isReallyOnline()) {
                requestData();
            }
        } else {
            if (flag) {
                // Goes online and needs to continue to get more jobs
                if (!allJobsLoaded) {
                    if (!taskQueue.hasLoaded100Jobs()) {
                        taskQueue.addTask(SearchRequestQueue.VIEW100);
                    }
                    showLoadingAtEnd(true);
                    attachScrollListener(true);
                }
            } else {
                // Go offline
                taskQueue.cancelAllTasks();
                showLoadingAtEnd(false);
                attachScrollListener(false);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem switchButton = menu.findItem(R.id.action_search);
        setSearchIcon(switchButton);
        return super.onPrepareOptionsMenu(menu);
    }

    private boolean canSearch() {
        return enableSearch && isReallyOnline();
    }

    private void setSearchEnabled(boolean enable) {
        enableSearch = enable;
        supportInvalidateOptionsMenu();
    }

    private void setSearchIcon(MenuItem button) {
        boolean ableToSearch = canSearch();
        button.setEnabled(ableToSearch);
        if (ableToSearch) {
            button.setIcon(R.drawable.ic_action_search);
        } else {
            button.setIcon(R.drawable.ic_action_search_disabled);
        }
    }

    @Override
    public void onPageSelected(int index) {
        super.onPageSelected(index);
        if (!allJobsLoaded) {
            showLoadingAtEnd(true);
            attachScrollListener(true);
        }
    }

    //=======================
    //  Activity for result
    //=======================
    @Override
    protected void goToDescription(int jobId) {
        Intent in = new Intent(this, Description.class);
        in.putExtra(EXTRA_JOB_ID, Integer.toString(jobId));
        startActivityForResult(in, REQUEST_CODE_DESCRIPTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_DESCRIPTION) {
            if (resultCode == Activity.RESULT_OK) {
                // Successfully read the job description
                String tab = getCurrentTabName();
                if (readView != null && tab == PAGES.NEW) {
                    Job job = (Job)readView.getTag(VIEW_ITEM_JOB_KEY);
                    addJobToListByTabId(PAGES.READ, job);
                    getAdapterByTab(PAGES.READ).notifyDataSetChanged();

                    // Animate the row removal
                    if (!rowAnimation.isRunning()) {
                        rowAnimation.setDelay(400);     // Glitch where the animation wont animate unless it waits a little
                        rowAnimation.startShink(readView);
                        rowAnimation.setDelay(0);
                    } else {
                        // If somehow fails and not looking at new page, just remove it
                        int pos = (Integer)readView.getTag(VIEW_ITEM_POSITION_KEY);
                        JbmnplsAdapterBase adapter = getAdapterByTab(PAGES.NEW);
                        adapter.remove(pos);
                    }
                }
            } else {
                // Cancelled reading the job description
                readView = null;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //=======================
    //  OnListViewListener
    //=======================
    @Override
    public void onVisuallyAddedRows(ListView listView) {
        String tab = getCurrentTabName();
        if (showGrowAnimation && tab == PAGES.SHORTLIST) {
            // Detect if user can see it
            if (listView.getChildCount() <= numItemsFitInListView) {
                // Ignore the requestLayout() improperly warning
                rowAnimation.startGrow(getListViewByTab(tab).getLastChild());
            }
            showGrowAnimation = false;
        }
    }

    @Override
    public void onVisuallyRemovedRows(ListView listView) {
    }

    //=======================
    //  OnJobSearchListener
    //=======================
    @Override
    public void onJobTypeChange(Spinner spinner, JobSearchProperties.JOBTYPE type) {
        properties.jobType.set(type);
        taskQueue.addTask(SearchRequestQueue.JOBTYPE);
    }

    @Override
    public void onSearch(JobSearchProperties prop) {
        if (isReallyOnline()) {
            taskQueue.cancelAllTasks();
            taskQueue.addTask(SearchRequestQueue.SEARCH, getString(R.string.search_searching_dialog));
        } else {
            hideSearchDialog();
            showAlert(getString(R.string.search_searching_offline));
        }
    }

    @Override
    public void onCancel() {
        if (taskQueue.isRunning()) {
            // Cancel only if job type changes when pressing cancel on the search dialog
            if (taskQueue.getCurrentCommand() == SearchRequestQueue.JOBTYPE) {
                taskQueue.cancelCurrent(true);
            }
        }
        if (firstSearch && getListByTab(PAGES.ALL).isEmpty()) {
            finish();
        }
    }

    //===========================
    //  OnCheckedChange Listener
    //===========================
    @Override
    public void onClick(View v) {
        if (v instanceof CheckBox) {
            CheckBox box = (CheckBox)v;

            // Get job from a parent layout's tag
            ViewGroup vg = (ViewGroup)box.getParent();
            View view = (View)vg.getParent();
            Job job = (Job)view.getTag(VIEW_ITEM_JOB_KEY);
            currentShortlistedJob = job;
            shortlistedTab = getCurrentTabName();
            taskQueue.addTask(SearchRequestQueue.SHORTLIST);
            enableShortlisting(false);

            // Show loading symbol
            View bar = vg.findViewById(R.id.loading);
            bar.setVisibility(View.VISIBLE);
            box.setVisibility(View.GONE);
        }
    }

    //====================
    //  Scroll Listener
    //====================
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        showGrowAnimation = false;
        numItemsFitInListView = Math.max(numItemsFitInListView, visibleItemCount);

        // Have the scroll update only on new or all lists
        String tab = getCurrentTabName();
        if (tab == PAGES.ALL || tab == PAGES.NEW) {
            currentListPosition = firstVisibleItem + visibleItemCount;
            fetchMoreIfNeeded();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    //==================================
    //  Row addition/removal animation
    //==================================
    class RowAnimation extends HeightAnimation {
        private boolean isGrowing;
        private String removalTab;

        public void startGrow(View v) {
            start(v, true);
        }

        public void startShink(View v) {
            start(v, false);
            removalTab = getCurrentTabName();
        }

        @Override
        public void start(View v, boolean grow) {
            isGrowing = grow;
            super.start(v, grow);
        }

        private void removeJob() {
            final int position = (Integer)getView().getTag(VIEW_ITEM_POSITION_KEY);
            JbmnplsAdapterBase adapter = getAdapterByTab(removalTab);           // TODO make this work with read to remove
            ArrayList<Job> jobs = adapter.getList();
            if (jobs.size() > 0) {
                if (jobs.remove(position) == null) {
                    throw new NullPointerException("Unable to find the job removed from animation");
                }
                adapter.notifyDataSetChanged();
            }
            removalTab = null;
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            if (!isGrowing) {
                removeJob();
            }
            super.onAnimationCancel(animator);
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (!isGrowing) {
                removeJob();
            }
            super.onAnimationEnd(animator);
        }
    }

    //=======================
    //  Methods for all Tabs
    //=======================
    protected void showLoadingAtEnd(boolean flag) {
        for (int i = 0; i < numOfTabs(); i++) {
            JbmnplsLoadingAdapterBase adapter = (JbmnplsLoadingAdapterBase)getAdapterByIndex(i);
            if (flag) {
                if (adapter.getCount() > numItemsFitInListView) {
                    adapter.showLoadingAtEnd(true);
                } else {
                    adapter.showLoadingAtEnd(false);
                }
            } else {
                adapter.showLoadingAtEnd(false);
            }
        }
    }

    protected void attachScrollListener(boolean flag) {
        OnScrollListener listener = flag ? this : null;
        for (int i = 0; i < numOfTabs(); i++) {
            ListView list = getListViewByIndex(i);
            if (list != null) {
                list.setOnScrollListener(listener);
            }
        }
    }

    //==================
    //  Miscellaneous
    //==================

    private boolean checkIfSearchExpired() {
        // When 20 min has past when nothing has posted
        boolean flag = searchDialog != null && !searchDialog.isShowing() && !client.isLoggedIn();

        if (flag) {
            // Reset to first time search and show the dialog again
            stateError = Error.INACTIVE;
            requestData();
        }
        return flag;
    }

    private void showSearchDialog() {
        // Need to get search values, so we will get them
        if (properties == null) {
            requestData();
            return;
        }
        if (canSearch()) {
            if (searchDialog == null) {
                searchDialog = new JobSearchDialog(this);
                searchDialog.setOnJobSearchListener(this);
                searchDialog.setProperties(properties);
            }
            searchDialog.show();
        }
    }

    protected void hideSearchDialog() {
        if (searchDialog != null) {
            searchDialog.dismiss();
        }
    }

    protected String getUrl() {
        return JbmnplsHttpClient.GET_LINKS.SEARCH;
    }

    protected void showAlert(String message) {
        alert.setMessage(message);
        alert.show();
    }

    // Only fetch more on all or new pages
    protected void fetchMoreIfNeeded() {
        String tab = getCurrentTabName();
        if (tab == PAGES.ALL || tab == PAGES.NEW) {
            int currentlyLoaded = Math.min(getListByTab(PAGES.ALL).size(), getListByTab(PAGES.NEW).size());
            if (currentlyLoaded - currentListPosition < FETCH_MORE_REACH_BOTTOM_COUNT   // If near the bottom of the list
                    && taskQueue.hasLoaded100Jobs()                             // Has loaded 100 items
                    && !allJobsLoaded
                    && !taskQueue.isRunning()) {                                // Not running any task currently
                taskQueue.addTask(SearchRequestQueue.NEXTPAGE);
            }
        }
    }

    protected void doneLoadingAllJobs() {
        allJobsLoaded = true;
        showLoadingAtEnd(false);
        attachScrollListener(false);
    }

    protected boolean isShortlisted(Job job) {
        return isShortlisted(job.getId());
    }

    protected boolean isShortlisted(int id) {
        return shortlistSet.contains(id);
    }

    protected void enableShortlisting(boolean flag) {
        if (enableShortlisting != flag) {
            for (int l = 0; l < numOfTabs(); l++) {
                ListView list = getListViewByIndex(l);
                if (list != null) {
                    for (int i = 0; i < list.getChildCount(); i++) {
                        View view = list.getChildAt(i);
                        CheckBox box = (CheckBox)view.findViewById(R.id.star);

                        // If the row is shortlisted, then check it and enable it
                        if (isShortlisted((Job)view.getTag(VIEW_ITEM_JOB_KEY)) || box.isChecked()) {
                            box.setEnabled(false);
                            box.setChecked(true);
                        } else {
                            box.setEnabled(flag);
                        }
                    }
                }
            }
            enableShortlisting = flag;
        }
    }

    protected void removeAllItemLoadingImage() {
        for (int l = 0; l < numOfTabs(); l++) {
            ListView list = getListViewByIndex(l);
            if (list != null) {
                for (int i = 0; i < list.getChildCount(); i++) {
                    View view = list.getChildAt(i);
                    view.findViewById(R.id.star).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.loading).setVisibility(View.GONE);
                }
            }
        }
    }

    //===========================
    //  All Purpose Task Runner
    //===========================
    private final class SearchRequestQueue extends JbmnplsRequestQueue<Void> {
        public final static int INITIAL_RESULT_COUNT = 25;
        public final static int RESULT_COUNT_100 = 100;

        // Task States
        public static final int SEARCH = 0;
        public static final int VIEW100 = 1;
        public static final int NEXTPAGE = 2;
        public static final int SHORTLIST = 3;
        public static final int JOBTYPE = 4;
        public static final int SORT = 5;
        public static final int DESCRIPTION = 6;

        // Task Response states
        protected static final int JOBTYPE_UNAUTH = 7;

        // Shortlist check string matches
        public static final String SHORTLIST_SUCCESS_STRING = "The job has been added successfully";

        // Request States
        private String lastSearchedHtml = null;
        private int currentPage;
        private int numJobs;
        private int totalPages;
        private boolean hasLoaded100;

        private final JobSearch activity;
        TableParser tableParser;

        public SearchRequestQueue(JobSearch a, String stateNumber, String id) {
            super(a, SimpleActivityBase.client, getUrl(), stateNumber, id);
            activity = a;
            currentPage = 0;
            hasLoaded100 = false;
        }

        public int getCurrentPageNumber() {
            return currentPage;
        }

        public boolean hasLoaded100Jobs() {
            return hasLoaded100;
        }

        private void parseHtmlForJobs() {
            readOrNewList = new ArrayList<Integer>();
            readOrNewJobList = new ArrayList<Job>();
            tableParser.execute(JOBSEARCH_OUTLINE, lastSearchedHtml);

            if (!readOrNewList.isEmpty()) {
                // Now we can get the jobs from their ids from the database
                ArrayList<Job> jobs = jobDataSource.getJobsByIdList(readOrNewList);
                if (jobs != null && !jobs.isEmpty()) {
                    for (int i = 0; i < jobs.size(); i++) {
                        // Add the job to read if it has description data
                        Job job = jobs.get(i);
                        if (job.hasDescriptionData()) {
                            addJobToListByTabId(PAGES.READ, job);
                        } else {
                            addJobToListByTabId(PAGES.NEW, job);
                        }
                    }
                } else if (!readOrNewJobList.isEmpty()) {
                    // Add all the jobs if this is the first time searching
                    getListByTab(PAGES.NEW).addAll(readOrNewJobList);
                }
            }
            readOrNewJobList = null;
            readOrNewList = null;
        }

        @Override
        protected Integer doInBackground(Integer... params) throws IOException {
            tableParser = new TableParser();
            tableParser.setOnTableRowParse(activity);
            ArrayList<Integer> shortlistIds = null;
            SimpleHtmlParser parser = null;
            String response = null;

            switch(getCurrentCommand()) {
            case SEARCH:
                showGrowAnimation = false;
                List<NameValuePair> data = new ArrayList<NameValuePair>();
                properties.addChangesToPostData(data);
                lastSearchedHtml = doPost("UW_CO_JOBSRCHDW_UW_CO_DW_SRCHBTN", data);

                // Get the shortlist ids from the databsae
                PageResult r = pageDataSource.getPageData(client.getUsername(), Shortlist.PAGE_NAME);
                if (r != null && r.ids != null) {
                    shortlistSet = new HashSet<Integer>(r.ids);
                } else {
                    shortlistSet = new HashSet<Integer>();
                }

                currentPage = 1;

                // Parse the jobs out
                clearAllLists();
                parseHtmlForJobs();
                properties.acceptChanges();

                updatePageJobInfo();

                // Write the shortlisted ids to the database
                shortlistIds = new ArrayList<Integer>(shortlistSet);
                pageDataSource.addPageIds(client.getUsername(), Shortlist.PAGE_NAME, shortlistIds);

                // Make a new task to view 100 jobs if there are more than 25 jobs
                if (numJobs > INITIAL_RESULT_COUNT && !hasLoaded100) {
                    addTask(VIEW100);
                }
                break;
            case VIEW100:
                view100();
                fetchMoreIfNeeded();
                break;
            case NEXTPAGE:
                getNextPage();
                parseHtmlForJobs();

                // If finished loading all the pages, then remove the scroll event
                if (currentPage != totalPages) {
                    fetchMoreIfNeeded();
                }
                break;
            case SHORTLIST:
                int goToPage = jobPageArray.get(currentShortlistedJob.getId());
                String name = null;

                if (currentPage != goToPage) {
                    // Go to the page
                    while(currentPage != goToPage) {
                        if (currentPage > goToPage) {
                            getPrevPage();
                        } else if (currentPage < goToPage) {
                            getNextPage();
                        }
                    }
                }

                // Get the shortlist job index from html
                parser = new SimpleHtmlParser(lastSearchedHtml);
                parser.skipText(currentShortlistedJob.getId() + "", "id='UW_CO_SLIST_HL$");
                name = parser.getAttributeInCurrentElement("id");

                response = doPost(name);

                if (!response.contains(SHORTLIST_SUCCESS_STRING)) {
                    throw new JbmnplsLostStateException("Was not able to shortlist");
                }

                // Write the shortlisted ids to the database
                shortlistSet.add(currentShortlistedJob.getId());
                shortlistIds = new ArrayList<Integer>(shortlistSet);
                pageDataSource.addPageIds(client.getUsername(), Shortlist.PAGE_NAME, shortlistIds);
                break;
            case JOBTYPE:
                response = doPost("TYPE_COOP", new BasicNameValuePair("TYPE_COOP",
                        properties.jobType.get().getIndex() + ""));

                if (response.contains(getString(R.string.job_search_type_unauth))) {
                    properties.jobType.rejectChange();
                    return JOBTYPE_UNAUTH;
                } else {
                    properties.jobType.updateChange();
                }
                break;
            case SORT:
                if (!hasLoaded100) {
                    view100();
                }
                while (currentPage < totalPages) {
                    getNextPage();
                }
                break;
            case DESCRIPTION:
                doPost("UW_CO_JOBTITLE_HL$0");
                break;
            default:
                return UNKNOWN_COMMAND;
            }
            return NO_PROBLEM;
        }

        @Override
        protected boolean checkForInActivity() {
            return checkIfSearchExpired();
        }

        private void updatePageJobInfo() {
            // Find the number of jobs in the result
            SimpleHtmlParser parser = new SimpleHtmlParser(lastSearchedHtml);
            parser.skipText("PSGRIDCOUNTER");
            String pageInfo = parser.getTextInCurrentElement();
            try {
                int ofIndex = pageInfo.lastIndexOf("of");
                if (getListByTab(PAGES.ALL).isEmpty()) {
                    numJobs = 0;
                    currentPage = 1;
                    totalPages = 1;
                } else {
                    numJobs = Integer.parseInt(pageInfo.substring(ofIndex + 3));
                    int dashIndex = pageInfo.indexOf("-");
                    int jobsPerPage = hasLoaded100 ? RESULT_COUNT_100 : INITIAL_RESULT_COUNT;
                    currentPage = (int)Math.ceil(Integer.parseInt(pageInfo.substring(0, dashIndex)) * 1.0 / jobsPerPage);
                    totalPages = (int)Math.ceil(numJobs * 1.0 / jobsPerPage);
                }
            } catch (NumberFormatException e) {
                throw new JbmnplsParsingException("Cannot find the number of jobs in the search result");
            }
        }

        private int view100() throws JbmnplsLoggedOutException, IOException {
            if (!hasLoaded100) {
                lastSearchedHtml = doPost("UW_CO_JOBRES_VW$hviewall$0");
                parseHtmlForJobs();

                // Update the page total
                hasLoaded100 = true;
                updatePageJobInfo();
            }
            return NO_PROBLEM;
        }

        private int getNextPage() throws JbmnplsLoggedOutException, IOException {
            if (currentPage == totalPages) {
                throw new JbmnplsLostStateException("Cannot go to next page because there is no more pages.");
            }
            lastSearchedHtml = doPost("UW_CO_JOBRES_VW$hdown$0");
            updatePageJobInfo();
            return NO_PROBLEM;
        }

        private int getPrevPage() throws JbmnplsLoggedOutException, IOException {
            if (currentPage == 0) {
                throw new JbmnplsLostStateException("Cannot go to previous page because current page is at 0.");
            }
            lastSearchedHtml = doPost("UW_CO_JOBRES_VW$hup$0");
            updatePageJobInfo();
            return NO_PROBLEM;
        }

        @Override
        protected void onCancelled() {
            // Reset shortlist state when cancelled
            enableShortlisting(true);
            currentShortlistedJob = null;
            shortlistedTab = null;
            removeAllItemLoadingImage();
            cancelAllTasks();
            super.onCancelled();
        }

        private void cancelled() {
            switch(getCurrentCommand()) {
            case SORT:
                resetSortingMenu();
                break;
            case SHORTLIST:
                // Revert check of the last item checked, check all pages for the shortlist id
                for (int l = 0; l < numOfTabs(); l++) {
                    ListView list = getListViewByIndex(l);
                    if (list != null) {
                        for (int i = 0; i < list.getChildCount(); i++) {
                            View view = list.getChildAt(i);
                            CheckBox box = (CheckBox) view.findViewById(R.id.star);
                            Job job = (Job) view.getTag(VIEW_ITEM_JOB_KEY);
                            if (currentShortlistedJob.getId() == job.getId()) {
                                box.setChecked(false);
                                break;
                            }
                        }
                    }
                }
                enableShortlisting(true);
                currentShortlistedJob = null;
                shortlistedTab = null;
                removeAllItemLoadingImage();
                break;
            }
            taskQueue.clear();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            if (result == CANCELLED) {
                cancelled();
                return;
            }

            // Finish off some of the commands
            switch(getCurrentCommand()) {
            case SEARCH:
                // Just finished search, reset all flags
                attachScrollListener(true);
                allJobsLoaded = false;
                firstSearch = false;        // No that you search, it is not the first time anymore
                resetSortingMenu();
                showLoadingAtEnd(true);

                if (currentPage == totalPages) {
                    doneLoadingAllJobs();
                }

                getSupportActionBar().setSubtitle(null);        // Remove subtitle after coming from offline
                hideSearchDialog();
                onRequestComplete(true);
                scrollToTop();
                break;
            case NEXTPAGE:
            case VIEW100:
                if (currentPage == totalPages) {
                    doneLoadingAllJobs();
                }
                onRequestComplete(true);
                break;
            case SORT:
                doneLoadingAllJobs();
                onRequestComplete(true);
                activity.sort(choseSortHeader, choseSortAscend);
                break;
            case SHORTLIST:
                enableShortlisting(true);
                removeAllItemLoadingImage();

                // Depending on what user is seeing and where it came from, this handles post shortlisting
                String currentTab = getCurrentTabName();
                int id = currentShortlistedJob.getId();
                boolean searchAndRemoveFromReadOrNewAdapter = false;
                addJobToListByTabId(PAGES.SHORTLIST, currentShortlistedJob);
                if (currentTab == PAGES.SHORTLIST) {
                    // If on the shortlisted page, then show the animation soon of it appearing
                    showGrowAnimation = true;
                    if (shortlistedTab != PAGES.APPLIED) {
                        searchAndRemoveFromReadOrNewAdapter = true;
                    }
                } else if (currentTab == PAGES.NEW || currentTab == PAGES.READ) {
                    // Currently looking at new or read, we will show the animation if it exists if
                    // not shortlisted from applied (or shortlist) page
                    if (shortlistedTab != PAGES.APPLIED) {
                        ListViewPlus listView = getListViewByTab(currentTab);
                        boolean found = false;

                        // Check to see if the user can see the shortlisted job, if so, then animate it
                        for (int i = 0; i < listView.getCount(); i++) {
                            View v = listView.getChildAt(i);
                            if (v == null) {
                                break;
                            }
                            if (((Job)v.getTag(VIEW_ITEM_JOB_KEY)).getId() == id) {
                                found = true;
                                // If can animate, otherwise just remove it (this will be rare)
                                if (!rowAnimation.isRunning()) {
                                    rowAnimation.startShink(v);
                                    shortlistedTab = currentTab;
                                } else {
                                    listView.removeViewAt((Integer)v.getTag(VIEW_ITEM_POSITION_KEY));
                                }
                                break;
                            }
                        }
                        // If user cannot see the shortlisted row, then we will just remove it
                        if (!found) {
                            searchAndRemoveFromReadOrNewAdapter = true;
                        }
                    }
                } else {
                    // Currently viewing all or applied
                    if (shortlistedTab != PAGES.APPLIED) {
                        // If we shortlisted from all, then we have to remove from read or new page
                        searchAndRemoveFromReadOrNewAdapter = true;
                    }
                }
                // Search for the job and remove from either read or new
                if (searchAndRemoveFromReadOrNewAdapter) {
                    // See if new page has the job
                    JbmnplsAdapterBase newAdapter = getAdapterByTab(PAGES.NEW);
                    if (!newAdapter.removeByJobId(id)) {
                        JbmnplsAdapterBase readAdapter = getAdapterByTab(PAGES.READ);
                        if (!readAdapter.removeByJobId(id)) {
                            throw new JbmnplsException("Cannot find shortlisted job in new or read when it was suppose to be here!");
                        }
                    }
                }
                updateLists();
                currentShortlistedJob = null;
                shortlistedTab = null;
                break;
            }


            // Parse the results
            switch(result) {
            case JOBTYPE_UNAUTH:
                searchDialog.setJobTypeSpinner(properties.jobType.get());

                // If there is no other tasks (such as searching) do not show the alert
                if (taskQueue.isEmpty()) {
                    showAlert(getString(R.string.job_search_type_unauth));
                }
                break;
            case UNKNOWN_COMMAND:
            case PARSING_ERROR:
                goToHomeActivity(getString(R.string.search_parsing_error_message));
                doneLoadingAllJobs();
                return;
            case LOST_STATE_RESULT:
                doneLoadingAllJobs();
                stateError = Error.LOST_STATE;
                requestData();
                break;
            case LOGOUT_RESULT:
                // Go back to home screen? and show a fail?
                doneLoadingAllJobs();
                stateError = Error.LOGGED_OUT;
                requestData();
                break;
            }
        }

    }
}
