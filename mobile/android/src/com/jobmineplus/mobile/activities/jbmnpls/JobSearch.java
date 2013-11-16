package com.jobmineplus.mobile.activities.jbmnpls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.SimpleActivityBase;
import com.jobmineplus.mobile.database.pages.PageResult;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.exceptions.JbmnplsLostStateException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
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
import com.jobmineplus.mobile.widgets.table.SimpleHtmlParser;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;

public class JobSearch extends JbmnplsListActivityBase implements
                            OnJobSearchListener, TableParser.OnTableParseListener,
                            OnScrollListener, OnClickListener {

    //======================
    //  Declaration Objects
    //======================
    public final static String PAGE_NAME = JobSearch.class.getName();
    public final static int FETCH_MORE_REACH_BOTTOM_COUNT = 40;

    // Task Queue
    private SearchRequestQueue taskQueue;

    // Shortlist data structures
    private final SparseIntArray jobPageArray = new SparseIntArray(200);
    private Set<Integer> shortlistSet;

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
        taskQueue = new SearchRequestQueue(this, null, null);       // Initially, it will actually get set in parseWebpage()
        firstSearch = true;
        currentListPosition = 0;
        allJobsLoaded = false;
        enableShortlisting = true;
        stateError = Error.NONE;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Job job = getList().get(position);
        int jobId = job.getId();
        if (!job.hasDescriptionData() && isReallyOnline()) {
            taskQueue.addTask(SearchRequestQueue.DESCRIPTION);
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
        clearList();
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
        return jobPageArray.indexOfKey(job.getId()) >= 0;
    }

    @Override
    protected void addJob(Job job) {
        // If it does not exist in the map, then include it
        if (!jobExists(job)) {
            jobPageArray.append(job.getId(), taskQueue.getCurrentPageNumber());
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

        ((JbmnplsLoadingAdapterBase)getAdapter()).showLoadingAtEnd(connected && isOnline() && !allJobsLoaded);
        setSearchEnabled(connected);
        if (connected) {
            getListView().setOnScrollListener(this);
        } else {
            taskQueue.cancelAllTasks();
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
                    ((JbmnplsLoadingAdapterBase)getAdapter()).showLoadingAtEnd(true);
                    getListView().setOnScrollListener(this);
                }
            } else {
                // Go offline
                taskQueue.cancelAllTasks();
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
            taskQueue.addTask(SearchRequestQueue.SHORTLIST);
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

    protected void fetchMoreIfNeeded() {
        int currentlyLoaded = getList().size();
        if (currentlyLoaded - currentListPosition < FETCH_MORE_REACH_BOTTOM_COUNT   // If near the bottom of the list
                && taskQueue.hasLoaded100Jobs()                             // Has loaded 100 items
                && !allJobsLoaded
                && !taskQueue.isRunning()) {                                // Not running any task currently
            taskQueue.addTask(SearchRequestQueue.NEXTPAGE);
        }
    }

    protected void doneLoadingAllJobs() {
        allJobsLoaded = true;
        ((JbmnplsLoadingAdapterBase)getAdapter()).showLoadingAtEnd(false);
        getListView().setOnScrollListener(null);
    }

    protected boolean isShortlisted(Job job) {
        return isShortlisted(job.getId());
    }

    protected boolean isShortlisted(int id) {
        return shortlistSet.contains(id);
    }

    protected void enableShortlisting(boolean flag) {
        if (enableShortlisting != flag) {
            ListView list = getListView();
            for (int i = 0; i < list.getChildCount(); i++) {
                View view = list.getChildAt(i);
                CheckBox box = (CheckBox)view.findViewById(R.id.star);

                // If the row is shortlisted, then check it and enable it
                if (isShortlisted((Job)box.getTag(R.id.CHECKBOX_JOB_TAG_KEY)) || box.isChecked()) {
                    box.setEnabled(false);
                    box.setChecked(true);
                } else {
                    box.setEnabled(flag);
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
        }

        public int getCurrentPageNumber() {
            return currentPage;
        }

        public boolean hasLoaded100Jobs() {
            return hasLoaded100;
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
                view100();
                fetchMoreIfNeeded();
                break;
            case NEXTPAGE:
                getNextPage();
                tableParser.execute(JOBSEARCH_OUTLINE, lastSearchedHtml);

                // If finished loading all the pages, then remove the scroll event
                if (currentPage != totalPages) {
                    fetchMoreIfNeeded();
                }
                break;
            case SHORTLIST:
                int goToPage = jobPageArray.get(idShortlisting);
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
                parser.skipText(idShortlisting + "", "id='UW_CO_SLIST_HL$");
                name = parser.getAttributeInCurrentElement("id");

                response = doPost(name);

                if (!response.contains(SHORTLIST_SUCCESS_STRING)) {
                    throw new JbmnplsLostStateException("Was not able to shortlist");
                }

                // Write the shortlisted ids to the database
                shortlistSet.add(idShortlisting);
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

        private int view100() throws JbmnplsLoggedOutException, IOException {
            lastSearchedHtml = doPost("UW_CO_JOBRES_VW$hviewall$0");
            tableParser.execute(JOBSEARCH_OUTLINE, lastSearchedHtml);

            // Update the page total
            hasLoaded100 = true;
            updatePageJobInfo();
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
            idShortlisting = 0;
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
            switch(getCurrentCommand()) {
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
