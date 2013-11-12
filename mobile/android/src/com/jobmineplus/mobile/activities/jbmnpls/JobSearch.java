package com.jobmineplus.mobile.activities.jbmnpls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.os.Bundle;
import android.util.Pair;
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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.database.pages.PageResult;
import com.jobmineplus.mobile.exceptions.JbmnplsCancelledException;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.exceptions.JbmnplsLostStateException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase.Formatter;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase.HIGHLIGHTING;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import com.jobmineplus.mobile.widgets.JbmnplsLoadingAdapterBase;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.Job.APPLY_STATUS;
import com.jobmineplus.mobile.widgets.Job.HEADER;
import com.jobmineplus.mobile.widgets.JobSearchDialog;
import com.jobmineplus.mobile.widgets.JobSearchDialog.OnJobSearchListener;
import com.jobmineplus.mobile.widgets.JobSearchProperties;
import com.jobmineplus.mobile.widgets.JobminePlusMobileLog;
import com.jobmineplus.mobile.widgets.ProgressDialogAsyncTaskBase;
import com.jobmineplus.mobile.widgets.table.SimpleHtmlParser;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;

public class JobSearch extends JbmnplsListActivityBase implements
                            OnJobSearchListener, TableParser.OnTableParseListener,
                            OnScrollListener, OnClickListener {

    // Few TODO notes
    //      1. concurrentmodificationexception happens sometimes (Arraylist or the jobSourceDatabase

    //======================
    //  Declaration Objects
    //======================
    public final static String PAGE_NAME = JobSearch.class.getName();
    public final static int INITIAL_RESULT_COUNT = 25;
    public final static int RESULT_COUNT_100 = 100;
    public final static int FETCH_MORE_REACH_BOTTOM_COUNT = 40;

    // Task list
    private final Queue<Pair<Integer, String>> taskQueue = new LinkedList<Pair<Integer,String>>();
    private final SparseIntArray jobPageArray = new SparseIntArray(200);
    private String lastSearchedHtml = null;
    private Set<Integer> shortlistSet;

    private JobSearchProperties properties;
    private SearchRequestTask jobSearchPageTask;

    private boolean enableSearch = true;
    private boolean firstSearch;
    private boolean allJobsLoaded;
    private boolean hasLoaded100;

    private JobSearchDialog searchDialog;

    private AlertDialog.Builder alert;

    // Sorting variables
    private HEADER choseSortHeader;
    private boolean choseSortAscend;

    // Job search post properties
    private String icsID;
    private String stateNum;
    private int numJobs;
    private int currentPage;
    private int totalPages;
    private int currentListPosition;
    private boolean wasInactive = false;

    // Shortlisting
    private int idShortlisting = 0;
    private boolean enableShortlisting;

    public final static HEADER[] SORT_HEADERS = {
        HEADER.JOB_TITLE,
        HEADER.EMPLOYER_NAME,
        HEADER.LOCATION,
        HEADER.OPENINGS,
        HEADER.APPLY,
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
            R.id.job_last_day, R.id.star, R.id.loading };

    //======================
    //  Overrided Methods
    //======================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alert = new Builder(this);
        alert.setNegativeButton(android.R.string.ok, null);
        alert.create();
        firstSearch = true;
        currentPage = 0;
        currentListPosition = 0;
        allJobsLoaded = false;
        enableShortlisting = true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Job job = getList().get(position);
        int jobId = job.getId();
        if (!job.hasDescriptionData()) {
            addTask(SearchRequestTask.DESCRIPTION);
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
        stateNum = parser.getAttributeInCurrentElement("value");

        // ICSID
        parser.skipText("id='ICSID'");
        icsID = parser.getAttributeInCurrentElement("value");

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

        properties.acceptChanges();
    }

    @Override
    public void onRowParse(TableParserOutline outline, Object... jobData) {
        int id = (Integer)jobData[0];

        // Maintain the shortlisted ids database
        if (((String)jobData[7]).equals("On Short List")) {
            shortlistSet.add(id);
        } else {
            shortlistSet.remove(id);
        }
        Job job = new Job(          id, (String)    jobData[1], (String)jobData[2],
                (String)    jobData[4], (Integer)   jobData[5], (APPLY_STATUS)jobData[6],
                (Date)      jobData[9], (Integer)   jobData[8]);
        addJob(job);
    }

    @Override
    protected void onRequestComplete(boolean pullData) {
        // Coming from HomeActivity
        if (firstSearch && pullData) {
            showSearchDialog();

            // If timed out, then show the message
            if (wasInactive) {
                showAlert(getString(R.string.search_inactivity));
                wasInactive = false;
            }
        } else {
            super.onRequestComplete(pullData);
        }
    }

    protected boolean jobExists(Job job) {
        return jobPageArray.indexOfKey(job.getId()) >= 0;
    }

    @Override
    protected void addJob(Job job) {
        // If it does not exist in the map, then include it
        if (!jobExists(job)) {
            jobPageArray.append(job.getId(), currentPage);
            super.addJob(job);
        }
    }

    @Override
    protected void clearList() {
        super.clearList();
        jobPageArray.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfSearchExpired();
    }

    @Override
    protected void onDestroy() {
        cancelAllTasks();
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
            addTask(SearchRequestTask.SORT, getString(R.string.search_sort_getting_jobs));
        } else {
            super.sort(header, ascend);
        }
    }

    @Override
    protected void onNetworkConnectionChanged(boolean connected) {
        super.onNetworkConnectionChanged(connected);
        enableShortlisting(connected);

        ((JbmnplsLoadingAdapterBase)getAdapter()).showLoadingAtEnd(connected && isOnline() && !allJobsLoaded);
        setSearchEnabled(connected);
        if (connected) {
            getListView().setOnScrollListener(this);
        } else {
            cancelAllTasks();
            getListView().setOnScrollListener(null);
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
        return super.doOffine();
    }

    //=================
    //  List Adapter
    //=================
    @Override
    protected HIGHLIGHTING formatJobListItem(int position, Job job,
            View[] elements, View layout) {
        Formatter.setText((TextView)elements[0], job.getTitle());
        Formatter.setText((TextView)elements[1], job.getEmployer(), true);
        Formatter.setText((TextView)elements[2], job.getLocation());

        // Checkbox
        CheckBox box = (CheckBox)elements[4];
        box.setOnClickListener(this);
        box.setChecked(isShortlisted(job));
        box.setEnabled(!box.isChecked() && enableShortlisting);
        box.setTag(R.id.CHECKBOX_JOB_TAG_KEY, job);

        View progress = elements[5];
        if (job.getId() == idShortlisting) {
            box.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);
        } else {
            box.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
        }

        Formatter.hide(elements[3]);
        return HIGHLIGHTING.NORMAL;
    }

    //=======================
    //  Handle Search Icon
    //=======================
    @Override
    protected void onlineModeChanged(boolean flag) {
        setSearchEnabled(flag);
        enableShortlisting(flag);

        // Coming in offline and going online, we need to get the new data
        if (firstSearch && flag) {
            if (isReallyOnline()) {
                requestData();
            }
        } else {
            if (flag) {
                // Goes online and needs to continue to get more jobs
                if (!allJobsLoaded) {
                    if (!hasLoaded100) {
                        addTask(SearchRequestTask.VIEW100);
                    }
                    ((JbmnplsLoadingAdapterBase)getAdapter()).showLoadingAtEnd(true);
                    getListView().setOnScrollListener(this);
                }
            } else {
                // Go offline
                cancelAllTasks();
                ((JbmnplsLoadingAdapterBase)getAdapter()).showLoadingAtEnd(false);
                getListView().setOnScrollListener(null);
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

    //=======================
    //  OnJobSearchListener
    //=======================
    @Override
    public void onJobTypeChange(Spinner spinner, JobSearchProperties.JOBTYPE type) {
        properties.jobType.set(type);
        addTask(SearchRequestTask.JOBTYPE);
    }

    @Override
    public void onSearch(JobSearchProperties prop) {
        if (isReallyOnline()) {
            addTask(SearchRequestTask.SEARCH, getString(R.string.search_searching_dialog));
        } else {
            hideSearchDialog();
            showAlert(getString(R.string.search_searching_offline));
        }
    }

    @Override
    public void onCancel() {
        if (jobSearchPageTask != null) {
            // Cancel only if job type changes when pressing cancel on the search dialog
            if (jobSearchPageTask.currentCommand == SearchRequestTask.JOBTYPE) {
                jobSearchPageTask.cancel(true);
            }
        }
        if (firstSearch && getList().isEmpty()) {
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
            Job job = (Job)box.getTag(R.id.CHECKBOX_JOB_TAG_KEY);
            idShortlisting = job.getId();
            addTask(SearchRequestTask.SHORTLIST);
            enableShortlisting(false);

            // Show loading symbol
            ViewGroup vg = (ViewGroup)box.getParent();
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
        currentListPosition = firstVisibleItem + visibleItemCount;
        fetchMoreIfNeeded();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    //==================
    //  Miscellaneous
    //==================
    private boolean checkIfSearchExpired() {
        // When 20 min has past when nothing has posted
        boolean flag = !searchDialog.isShowing() && !client.isLoggedIn();

        if (flag) {
            // Reset to first time search and show the dialog again
            wasInactive = true;
            firstSearch = true;
            clearList();
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

    protected void fetchMoreIfNeeded() {
        int currentlyLoaded = getList().size();
        if (currentlyLoaded - currentListPosition < FETCH_MORE_REACH_BOTTOM_COUNT   // If near the bottom of the list
                && hasLoaded100                                                     // Has loaded 100 items
                && !allJobsLoaded
                && !jobSearchPageTask.isRunning()) {                                // Not running any task currently
            addTask(SearchRequestTask.NEXTPAGE);
        }
    }

    protected void doneLoadingAllJobs() {
        allJobsLoaded = true;
        ((JbmnplsLoadingAdapterBase)getAdapter()).showLoadingAtEnd(false);
        getListView().setOnScrollListener(null);
    }

    protected boolean isShortlisted(Job job) {
        return shortlistSet.contains(job.getId());
    }

    protected void enableShortlisting(boolean flag) {
        if (enableShortlisting != flag) {
            ListView list = getListView();
            for (int i = 0; i < list.getChildCount(); i++) {
                View view = list.getChildAt(i);
                CheckBox box = (CheckBox)view.findViewById(R.id.star);
                if (!box.isChecked()) {
                    box.setEnabled(flag);
                } else if (isShortlisted((Job)box.getTag(R.id.CHECKBOX_JOB_TAG_KEY))) {
                    // After shortlisting, we should disable the checkbox
                    box.setEnabled(false);
                }
            }
            enableShortlisting = flag;
        }
    }

    protected void removeAllItemLoadingImage() {
        ListView list = getListView();
        for (int i = 0; i < list.getChildCount(); i++) {
            View view = list.getChildAt(i);
            view.findViewById(R.id.star).setVisibility(View.VISIBLE);
            view.findViewById(R.id.loading).setVisibility(View.GONE);
        }
    }

    //===================
    //  Handling Tasks
    //===================
    private void addTask(int jobCode) {
        addTask(jobCode, null);
    }

    private void addTask(int jobCode, String message) {
        taskQueue.add(new Pair<Integer, String>(jobCode, message));
        runTask();
    }

    private void runTask() {
        if (!taskQueue.isEmpty() && (jobSearchPageTask == null || !jobSearchPageTask.isRunning())) {
            Pair<Integer, String> taskInfo = taskQueue.poll();
            jobSearchPageTask = new SearchRequestTask(this, taskInfo.second);
            jobSearchPageTask.execute(taskInfo.first);
        }
    }

    private void cancelAllTasks() {
        if (jobSearchPageTask != null) {
            jobSearchPageTask.cancel(true);
        }
    }

    //===========================
    //  All Purpose Task Runner
    //===========================
    private final class SearchRequestTask extends ProgressDialogAsyncTaskBase<Integer, Void, Integer> {

        private final TableParser tableParser = new TableParser();
        private final JobSearch activity;
        private int currentCommand;
        private String response = null;

        // Task States
        public static final int SEARCH = 0;
        public static final int VIEW100 = 1;
//        public static final int VIEW25 = 2;
        public static final int NEXTPAGE = 3;
//        public static final int PREVPAGE = 4;
//        public static final int LASTPAGE = 5;
//        public static final int FIRSTPAGE = 6;
        public static final int SHORTLIST = 7;
        public static final int JOBTYPE = 8;
        public static final int SORT = 9;
        public static final int DESCRIPTION = 10;

        // Task Response states
        public static final int NO_PROBLEM = 0;
        public static final int UNKNOWN_COMMAND = 1;
        public static final int JOBTYPE_UNAUTH = 2;
        public static final int LOGOUT_RESULT = 3;
        public static final int LOST_STATE_RESULT = 4;
        public static final int CANCELLED = 5;
        public static final int PARSING_ERROR = 6;

        public static final String LOST_STATE_STRING = "return to your most recent active page";
        public static final String EXCEPTION_STRING = "uw_exception.html";
        public static final String SHORTLIST_SUCCESS_STRING = "The job has been added successfully";

        public SearchRequestTask(JobSearch a, String message) {
            super(a, message, message != null);
            activity = a;
            tableParser.setOnTableRowParse(a);
        }

        private void updatePageJobInfo() {
            // Find the number of jobs in the result
            SimpleHtmlParser parser = new SimpleHtmlParser(lastSearchedHtml);
            parser.skipText("PSGRIDCOUNTER");
            String pageInfo = parser.getTextInCurrentElement();
            try {
                int ofIndex = pageInfo.lastIndexOf("of");
                if (getList().isEmpty()) {
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

        @Override
        protected Integer doInBackground(Integer... params) {
            if (checkIfSearchExpired()) {
                return NO_PROBLEM;
            }
            if (!verifyLogin()) {
                return LOGOUT_RESULT;
            }

            currentCommand = params[0];
            List<NameValuePair> postData = new ArrayList<NameValuePair>();
            SimpleHtmlParser parser = null;
            ArrayList<Integer> shortlistIds = null;
            int result = NO_PROBLEM;
            try {
                switch(currentCommand) {
                case SEARCH:
                    postData.add(new BasicNameValuePair("ICAction", "UW_CO_JOBSRCHDW_UW_CO_DW_SRCHBTN"));
                    properties.addChangesToPostData(postData);

                    doPost(postData, getUrl());
                    lastSearchedHtml = response;

                    // Get the shortlist ids from the databsae
                    PageResult r = pageDataSource.getPageData(client.getUsername(), Shortlist.PAGE_NAME);
                    if (r != null && r.ids != null) {
                        shortlistSet = new HashSet<Integer>(r.ids);
                    } else {
                        shortlistSet = new HashSet<Integer>();
                    }

                    currentPage = 1;

                    // Parse the jobs out
                    clearList();
                    tableParser.execute(JOBSEARCH_OUTLINE, lastSearchedHtml);
                    properties.acceptChanges();

                    updatePageJobInfo();

                    // Write the shortlisted ids to the database
                    shortlistIds = new ArrayList<Integer>(shortlistSet);
                    pageDataSource.addPageIds(client.getUsername(), Shortlist.PAGE_NAME, shortlistIds);

                    // Make a new task to view 100 jobs if there are more than 25 jobs
                    if (numJobs > INITIAL_RESULT_COUNT) {
                        addTask(VIEW100);
                    }
                    break;
                case VIEW100:
                    view100(postData);
                    fetchMoreIfNeeded();
                    break;
                case NEXTPAGE:
                    getNextPage(postData);
                    tableParser.execute(JOBSEARCH_OUTLINE, lastSearchedHtml);

                    // If finished loading all the pages, then remove the scroll event
                    if (currentPage != totalPages) {
                        fetchMoreIfNeeded();
                    }
                    return result;
                case SHORTLIST:
                    int goToPage = jobPageArray.get(idShortlisting);
                    String name = null;

                    if (currentPage != goToPage) {
                        // Go to the page
                        while(currentPage != goToPage) {
                            if (currentPage > goToPage) {
                                getPrevPage(postData);
                            } else if (currentPage < goToPage) {
                                getNextPage(postData);
                            }
                            postData.clear();
                        }
                    }

                    // Get the shortlist job index from html
                    parser = new SimpleHtmlParser(lastSearchedHtml);
                    parser.skipText(idShortlisting + "", "id='UW_CO_SLIST_HL$");
                    name = parser.getAttributeInCurrentElement("id");

                    postData.add(new BasicNameValuePair("ICAction", name));
                    doPost(postData, getUrl());

                    if (!response.contains(SHORTLIST_SUCCESS_STRING)) {
                        throw new JbmnplsLostStateException("Was not able to shortlist");
                    }

                    // Write the shortlisted ids to the database
                    shortlistSet.add(idShortlisting);
                    shortlistIds = new ArrayList<Integer>(shortlistSet);
                    pageDataSource.addPageIds(client.getUsername(), Shortlist.PAGE_NAME, shortlistIds);
                    break;
                case JOBTYPE:
                    postData.add(new BasicNameValuePair("ICAction", "TYPE_COOP"));
                    postData.add(new BasicNameValuePair("TYPE_COOP",
                            properties.jobType.get().getIndex() + ""));

                    doPost(postData, getUrl());
                    if (response.contains(getString(R.string.job_search_type_unauth))) {
                        properties.jobType.rejectChange();
                        return JOBTYPE_UNAUTH;
                    } else {
                        properties.jobType.updateChange();
                    }
                    break;
                case SORT:
                    if (!hasLoaded100) {
                        view100(postData);
                    }
                    while (currentPage < totalPages) {
                        getNextPage(postData);
                    }
                    break;
                case DESCRIPTION:
                    postData.add(new BasicNameValuePair("ICAction", "UW_CO_JOBTITLE_HL$0"));
                    doPost(postData, getUrl());
                    break;
                default:
                    return UNKNOWN_COMMAND;
                }
            } catch (JbmnplsCancelledException e) {
                return CANCELLED;
            } catch (JbmnplsLoggedOutException e) {
                e.printStackTrace();
                BugSenseHandler.sendException(e);
                JobminePlusMobileLog.sendException(activity, response, e);
                return LOGOUT_RESULT;
            } catch (JbmnplsLostStateException e) {
                e.printStackTrace();
                BugSenseHandler.sendException(e);
                JobminePlusMobileLog.sendException(activity, response, e);
                return LOST_STATE_RESULT;
            } catch (JbmnplsParsingException e) {
                e.printStackTrace();
                BugSenseHandler.sendException(e);
                JobminePlusMobileLog.sendException(activity, response, e);
                return PARSING_ERROR;
            } catch (IOException e) {
                e.printStackTrace();
                BugSenseHandler.sendException(e);
                JobminePlusMobileLog.sendException(activity, response, e);
                return PARSING_ERROR;
            }
            return NO_PROBLEM;
        }

        protected String doPost(List<NameValuePair> data, String url)
                throws JbmnplsLoggedOutException, IOException {
            data.add(new BasicNameValuePair("ICElementNum", "0"));
            data.add(new BasicNameValuePair("ICAJAX", "1"));
            data.add(new BasicNameValuePair("ICStateNum", stateNum));
            data.add(new BasicNameValuePair("ICSID", icsID));
            response = client.postJobmineHtml(data, url);

            // Check response for authorization
            if (response == null) {
                throw new JbmnplsCancelledException();
            } else if (response.contains(EXCEPTION_STRING)) {
                throw new JbmnplsParsingException("Jobmine threw an exception.");
            } else if (response.contains(LOST_STATE_STRING)) {
                throw new JbmnplsLostStateException();
            }

            // Find the new state number
            SimpleHtmlParser parser = new SimpleHtmlParser(response);
            int startIndex = parser.skipText("ICStateNum", "=");
            int endIndex = response.indexOf(";", startIndex);
            if (endIndex == -1) {
                throw new JbmnplsParsingException("Cannot find state number");
            }
            stateNum = response.substring(startIndex, endIndex);
            return response;
        }

        private int view100(List<NameValuePair> data) throws JbmnplsLoggedOutException, IOException {
            data.clear();
            data.add(new BasicNameValuePair("ICAction", "UW_CO_JOBRES_VW$hviewall$0"));
            doPost(data, getUrl());
            lastSearchedHtml = response;

            tableParser.execute(JOBSEARCH_OUTLINE, lastSearchedHtml);

            // Update the page total
            hasLoaded100 = true;
            updatePageJobInfo();
            return NO_PROBLEM;
        }

        private int getNextPage(List<NameValuePair> data) throws JbmnplsLoggedOutException, IOException {
            if (currentPage == totalPages) {
                throw new JbmnplsLostStateException("Cannot go to next page because there is no more pages.");
            }
            data.clear();
            data.add(new BasicNameValuePair("ICAction", "UW_CO_JOBRES_VW$hdown$0"));
            doPost(data, getUrl());
            lastSearchedHtml = response;
            updatePageJobInfo();
            return NO_PROBLEM;
        }

        private int getPrevPage(List<NameValuePair> data) throws JbmnplsLoggedOutException, IOException {
            if (currentPage == 0) {
                throw new JbmnplsLostStateException("Cannot go to previous page because current page is at 0.");
            }
            data.clear();
            data.add(new BasicNameValuePair("ICAction", "UW_CO_JOBRES_VW$hup$0"));
            doPost(data, getUrl());
            lastSearchedHtml = response;
            updatePageJobInfo();
            return NO_PROBLEM;
        }

        private void cancelled() {
            switch(currentCommand) {
            case SORT:
                resetSortingMenu();
                break;
            case SHORTLIST:
                // Revert check of the last item checked
                ListView list = getListView();
                for (int i = 0; i < list.getChildCount(); i++) {
                    CheckBox box = (CheckBox)list.getChildAt(i).findViewById(R.id.star);
                    Job job = (Job)box.getTag(R.id.CHECKBOX_JOB_TAG_KEY);
                    if (idShortlisting == job.getId()) {
                        box.setChecked(false);
                        break;
                    }
                }
                enableShortlisting(true);
                idShortlisting = 0;
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
            switch(currentCommand) {
            case SEARCH:
                // Just finished search, reset all flags
                getListView().setOnScrollListener(activity);
                allJobsLoaded = false;
                hasLoaded100 = false;
                firstSearch = false;        // No that you search, it is not the first time anymore
                resetSortingMenu();

                getSupportActionBar().setSubtitle(null);        // Remove subtitle after coming from offline
                hideSearchDialog();
                ((JbmnplsLoadingAdapterBase)getAdapter()).showLoadingAtEnd(true);
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
                idShortlisting = 0;
                removeAllItemLoadingImage();
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
                toast("Went to lost state and failed");
                doneLoadingAllJobs();
                break;
            case LOGOUT_RESULT:
                // Go back to home screen? and show a fail?
                toast("Went to logout and failed");
                doneLoadingAllJobs();
                break;
            }

            // Execute next task in the list
            runTask();
        }
    }
}
