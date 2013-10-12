package com.jobmineplus.mobile.activities.jbmnpls;

import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Pair;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.bugsense.trace.BugSenseHandler;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.exceptions.JbmnplsLostStateException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import com.jobmineplus.mobile.widgets.JbmnplsLoadingAdapterBase;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.JobSearchDialog;
import com.jobmineplus.mobile.widgets.JobSearchProperties;
import com.jobmineplus.mobile.widgets.JobminePlusMobileLog;
import com.jobmineplus.mobile.widgets.ProgressDialogAsyncTaskBase;
import com.jobmineplus.mobile.widgets.Job.HEADER;
import com.jobmineplus.mobile.widgets.JobSearchDialog.OnJobSearchListener;
import com.jobmineplus.mobile.widgets.table.SimpleHtmlParser;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;

public class JobSearch extends JbmnplsListActivityBase implements
                            OnJobSearchListener, TableParser.OnTableParseListener,
                            OnScrollListener {
// TODO fix when offline and pulling data and going back


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

    public final static HEADER[] SORT_HEADERS = {
        HEADER.JOB_TITLE,
        HEADER.EMPLOYER_NAME,
        HEADER.LOCATION,
        HEADER.OPENINGS,
        HEADER.APPLY,
        HEADER.LAST_DAY_TO_APPLY,
        HEADER.NUM_APPS
    };      // TODO add shortlist column to the database and able to sort it later

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

    protected final int[] WIDGET_RESOURCE_LIST = {          // TODO change this
            R.id.job_title, R.id.job_employer, R.id.location,
            R.id.job_status_first_line,R.id.job_status_second_line,
            R.id.job_last_day };

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
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int jobId = getList().get(position).getId();
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
    public JbmnplsAdapterBase getNewAdapter() {
        return new JobSearchAdapter(this, R.layout.job_widget, WIDGET_RESOURCE_LIST, getList());
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
        } else if (id == R.id.action_sort) {
            SubMenu sub = item.getSubMenu();
            boolean isReallyOnline = isReallyOnline();
            for (int i = 0; i < sub.size(); i++) {
                sub.getItem(i).setVisible(isReallyOnline);
            }
            if (!isReallyOnline) {
                showAlert(getString(R.string.search_sort_offline));
            }
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
        Job job = new Job(
                (Integer)   jobData[0], (String)    jobData[1], (String)jobData[2],
                (String)    jobData[4], (Integer)   jobData[5], (Date)  jobData[9],
                (Integer)   jobData[8]);
        addJob(job);
    }

    @Override
    protected void onRequestComplete(boolean pullData) {
        // Coming from HomeActivity
        if (firstSearch && pullData) {
            showSearchDialog();
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
        if (!allJobsLoaded) {
            choseSortHeader = header;
            choseSortAscend = ascend;
            addTask(SearchRequestTask.SORT, getString(R.string.search_sort_getting_jobs));
        } else {
            super.sort(header, ascend);
        }
    }

    //=======================
    //  Handle Search Icon
    //=======================
    @Override
    protected void onlineModeChanged(boolean flag) {
        setSearchEnabled(flag);

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
                    ((JobSearchAdapter)getAdapter()).showLoadingAtEnd(true);
                    getListView().setOnScrollListener(this);
                }
            } else {
                // Go offline
                cancelAllTasks();
                ((JobSearchAdapter)getAdapter()).showLoadingAtEnd(false);
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
        if (enableSearch != enable) {
            synchronized (this) {
                if (enableSearch != enable) {
                    enableSearch = enable;
                    supportInvalidateOptionsMenu();
                }
            }
        }
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
        firstSearch = false;        // No that you search, it is not the first time anymore
        addTask(SearchRequestTask.SEARCH, "Searching...");
    }

    @Override
    public void onCancel() {
        if (jobSearchPageTask != null) {
            jobSearchPageTask.cancel(true);
        }
        if (firstSearch && getList().isEmpty()) {
            finish();
        }
    }

    //====================
    //  Scroll Listener
    //====================
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        currentListPosition = firstVisibleItem + visibleItemCount;

        // See if we are offline
        if (isReallyOnline()) {
            fetchMoreIfNeeded();
            setSearchEnabled(true);
            ((JobSearchAdapter)getAdapter()).showLoadingAtEnd(true);
        } else {
            setSearchEnabled(false);
            ((JobSearchAdapter)getAdapter()).showLoadingAtEnd(false);
            cancelAllTasks();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    //==================
    //  Miscellaneous
    //==================
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
        ((JobSearchAdapter)getAdapter()).showLoadingAtEnd(false);
        getListView().setOnScrollListener(null);
    }

    //=================
    //  List Adapter
    //=================
    private class JobSearchAdapter extends JbmnplsLoadingAdapterBase {
        public JobSearchAdapter(Activity a, int listViewResourceId, int[] viewResourceIdListInWidget,
                ArrayList<Job> list) {
            super(a, listViewResourceId, viewResourceIdListInWidget, list);
        }

        @Override
        protected HIGHLIGHTING setJobWidgetValues(int position, Job job, View[] elements, View layout) {
//            APPLY_STATUS status = job.getApplicationStatus();
//            String statusStr = status == APPLY_STATUS.CANNOT_APPLY ? "Cannot Apply" : status.toString();
//
            setText(0, job.getTitle());
            setText(1, job.getEmployer(), true);
            setText(2, job.getLocation());
//            setText(3, 4, statusStr, true);
//
//            // Show the closing date if hasnt passed yet
//            Date closingDate = job.getLastDateToApply();
//            if (closingDate.after(new Date())) {
//                setDate(5, job.getLastDateToApply(), "Apply by");
//            } else {
//                hide(5);
//            }
            hide(5);

//
//            if (status == APPLY_STATUS.ALREADY_APPLIED) {
//                return HIGHLIGHTING.GREAT;
//            } else if (status == APPLY_STATUS.CANNOT_APPLY || status == APPLY_STATUS.NOT_POSTED) {
//                return HIGHLIGHTING.BAD;
//            }
            return HIGHLIGHTING.NORMAL;
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
            taskQueue.clear();
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
        private String response;

        // Task States
        public static final int SEARCH = 0;
        public static final int VIEW100 = 1;
        public static final int VIEW25 = 2;
        public static final int NEXTPAGE = 3;
        public static final int PREVPAGE = 4;
        public static final int LASTPAGE = 5;
        public static final int FIRSTPAGE = 6;
        public static final int SHORTLIST = 7;
        public static final int JOBTYPE = 8;
        public static final int SORT = 9;

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

        public SearchRequestTask(JobSearch a, String message) {
            super(a, message, message != null);
            activity = a;
            tableParser.setOnTableRowParse(a);
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            currentCommand = params[0];
            List<NameValuePair> postData = new ArrayList<NameValuePair>();
            SimpleHtmlParser parser = null;
            int result = NO_PROBLEM;
            try {
                switch(currentCommand) {
                case SEARCH:
                    postData.add(new BasicNameValuePair("ICAction", "UW_CO_JOBSRCHDW_UW_CO_DW_SRCHBTN"));

                    // Location
                    if (properties.location.hasChanged()) {
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_LOCATION",
                                properties.location.get()));
                    }

                    // Type
                    if (properties.jobType.hasChanged()) {
                        postData.add(new BasicNameValuePair("TYPE_COOP",
                                properties.jobType.get().getIndex()+""));
                    }

                    // Disciplines
                    if (properties.disciplines1.hasChanged()) {
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_ADV_DISCP1",
                                JobSearchDialog.getDisciplineCodeFromName(properties.disciplines1.get())));
                    }
                    if (properties.disciplines2.hasChanged()) {
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_ADV_DISCP2",
                                JobSearchDialog.getDisciplineCodeFromName(properties.disciplines2.get())));
                    }
                    if (properties.disciplines3.hasChanged()) {
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_ADV_DISCP3",
                                JobSearchDialog.getDisciplineCodeFromName(properties.disciplines3.get())));
                    }

                    // Search Filter
                    if (properties.filter.hasChanged()) {
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_JS_JOBSTATUS",
                                properties.filter.get().getCode()));
                    }

                    // Term
                    if (properties.term.hasChanged()) {
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_WT_SESSION",
                                properties.term.get()));
                    }

                    // Employer Text
                    if (properties.employer.hasChanged()) {
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_EMPLYR_NAME",
                                properties.employer.get()));
                    }

                    // Job Title Text
                    if (properties.title.hasChanged()) {
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_JOB_TITLE",
                                properties.title.get()));
                    }

                    // Checkboxes
                    if (properties.levelJunior.hasChanged()) {
                        String yes = properties.levelJunior.get() ? "Y" : "N";
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_COOP_JR$chk", yes));
                    }
                    if (properties.levelJunior.hasChanged()) {
                        String yes = properties.levelIntermediate.get() ? "Y" : "N";
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_COOP_INT$chk", yes));
                    }
                    if (properties.levelJunior.hasChanged()) {
                        String yes = properties.levelSenior.get() ? "Y" : "N";
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_COOP_SR$chk", yes));
                    }
                    if (properties.levelBachelors.hasChanged()) {
                        String yes = properties.levelBachelors.get() ? "Y" : "N";
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_BACHELOR$chk", yes));
                    }
                    if (properties.levelMasters.hasChanged()) {
                        String yes = properties.levelMasters.get() ? "Y" : "N";
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_MASTERS$chk", yes));
                    }
                    if (properties.levelPhD.hasChanged()) {
                        String yes = properties.levelPhD.get() ? "Y" : "N";
                        postData.add(new BasicNameValuePair("UW_CO_JOBSRCH_UW_CO_PHD$chk", yes));
                    }

                    response = doPost(postData, getUrl());
                    if (response == null) {
                        return CANCELLED;
                    }

                    // Parse the jobs out
                    clearList();
                    tableParser.execute(JOBSEARCH_OUTLINE, response);
                    properties.acceptChanges();

                    // Find the number of jobs in the result
                    parser = new SimpleHtmlParser(response);
                    parser.skipText("PSGRIDCOUNTER");
                    String pageInfo = parser.getTextInCurrentElement();
                    try {
                        int ofIndex = pageInfo.lastIndexOf("of");
                        if (getList().isEmpty()) {
                            numJobs = 0;
                        } else {
                            numJobs = Integer.parseInt(pageInfo.substring(ofIndex + 3));
                        }
                    } catch (NumberFormatException e) {
                        throw new JbmnplsParsingException("Cannot find the number of jobs in the search result");
                    }

                    // Find number of pages
                    totalPages = numJobs == 0 ? 1 : (int)Math.ceil(numJobs * 1.0 / INITIAL_RESULT_COUNT);

                    // Make a new task to view 100 jobs if there are more than 25 jobs
                    if (numJobs > INITIAL_RESULT_COUNT) {
                        addTask(VIEW100);
                    }
                    break;
                case VIEW100:
                    if (view100(postData) == CANCELLED) {
                        return CANCELLED;
                    }
                    // Load next page
                    fetchMoreIfNeeded();
                    break;
                case VIEW25:
                    break;
                case NEXTPAGE:
                    result = getNextPage(postData);
                    if (result == CANCELLED) {
                        return CANCELLED;
                    }

                    // If finished loading all the pages, then remove the scroll event
                    if (currentPage != totalPages) {
                        fetchMoreIfNeeded();
                    }
                    return result;
                case PREVPAGE:
                    break;
                case LASTPAGE:
                    break;
                case FIRSTPAGE:
                    break;
                case SHORTLIST:
                    break;
                case JOBTYPE:
                    postData.add(new BasicNameValuePair("ICAction", "TYPE_COOP"));
                    postData.add(new BasicNameValuePair("TYPE_COOP",
                            properties.jobType.get().getIndex() + ""));

                    response = doPost(postData, getUrl());
                    if (response == null) {
                        return CANCELLED;
                    }
                    if (response.contains(getString(R.string.job_search_type_unauth))) {
                        properties.jobType.rejectChange();
                        return JOBTYPE_UNAUTH;
                    } else {
                        properties.jobType.updateChange();
                    }
                    break;
                case SORT:
                    if (!hasLoaded100) {
                        result = view100(postData);
                        if (result == CANCELLED) {
                            return CANCELLED;
                        }
                    }
                    while (currentPage < totalPages) {
                        result = getNextPage(postData);
                        if (result == CANCELLED) {
                            return CANCELLED;
                        }
                    }
                    break;
                default:
                    return UNKNOWN_COMMAND;
                }
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
                return null;
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

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            client.abort();

            switch(currentCommand) {
            case SORT:
                resetSortingMenu();
                break;
            }
        }

        private int view100(List<NameValuePair> data) throws JbmnplsLoggedOutException, IOException {
            data.clear();
            data.add(new BasicNameValuePair("ICAction", "UW_CO_JOBRES_VW$hviewall$0"));
            response = doPost(data, getUrl());
            if (response == null) {
                return CANCELLED;
            }
            tableParser.execute(JOBSEARCH_OUTLINE, response);

            // Update the page total
            totalPages = (int) Math.ceil(numJobs * 1.0 / RESULT_COUNT_100);
            hasLoaded100 = true;
            return NO_PROBLEM;
        }

        private int getNextPage(List<NameValuePair> data) throws JbmnplsLoggedOutException, IOException {
            data.clear();
            data.add(new BasicNameValuePair("ICAction", "UW_CO_JOBRES_VW$hdown$0"));
            response = doPost(data, getUrl());
            if (response == null) {
                return CANCELLED;
            }
            tableParser.execute(JOBSEARCH_OUTLINE, response);
            currentPage++;
            return NO_PROBLEM;
        }

        @Override
        protected void onPostExecute(Integer result) {
            response = null;
            super.onPostExecute(result);
            if (result == CANCELLED) {
                taskQueue.clear();
                return;
            }

            // Finish off some of the commands
            switch(currentCommand) {
            case SEARCH:
                // Just finished search, reset all flags
                getListView().setOnScrollListener(activity);
                allJobsLoaded = false;
                currentPage = 1;
                hasLoaded100 = false;
                resetSortingMenu();

                getSupportActionBar().setSubtitle(null);        // Remove subtitle after coming from offline
                searchDialog.dismiss();
                ((JobSearchAdapter)getAdapter()).showLoadingAtEnd(true);
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
                break;
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
