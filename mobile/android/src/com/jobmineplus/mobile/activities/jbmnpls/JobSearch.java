package com.jobmineplus.mobile.activities.jbmnpls;

import java.io.IOException;
import java.security.InvalidParameterException;
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
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.exceptions.JbmnplsLostStateException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
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
                            OnJobSearchListener, TableParser.OnTableParseListener {

    //======================
    //  Declaration Objects
    //======================
    public final static String PAGE_NAME = JobSearch.class.getName();

    // Task list
    private final Queue<Pair<Integer, String>> taskQueue = new LinkedList<Pair<Integer,String>>();

    private JobSearchProperties properties;
    private SearchRequestTask jobSearchPageTask;

    private boolean enableSearch = true;

    private JobSearchDialog searchDialog;

    private AlertDialog.Builder alert;

    // Job search post properties
    private String icsID;
    private String stateNum;

    public final static HEADER[] SORT_HEADERS = {
        HEADER.JOB_TITLE,
        HEADER.EMPLOYER_NAME,
        HEADER.LOCATION,
        HEADER.OPENINGS,
        HEADER.APPLY,
        HEADER.SHORTLIST,
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
    protected void defineUI(Bundle savedInstanceState) {
        super.defineUI(savedInstanceState);
        setAdapter(new JobSearchAdapter(this, R.layout.job_widget, WIDGET_RESOURCE_LIST, getList()));
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
        Job job = new Job(
                (Integer)   jobData[0], (String)    jobData[1], (String)jobData[2],
                (String)    jobData[4], (Integer)   jobData[5], (Date)  jobData[9],
                (Integer)   jobData[8]);
        addJob(job);
    }

    @Override
    protected void onRequestComplete(boolean pullData) {
        // Coming from HomeActivity
        if (searchDialog == null && pullData) {     // TODO Change to first time here boolean
            showSearchDialog();
        } else {
            super.onRequestComplete(pullData);
        }
    }

    @Override
    protected void onlineModeChanged(boolean flag) {
        setSearchEnabled(flag);
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
        synchronized (this) {
            enableSearch = enable;
            supportInvalidateOptionsMenu();
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

    private void showSearchDialog() {
        if (properties == null) {
            throw new InvalidParameterException(
                    "Cannot show search dialog if data is not parsed and set yet.");
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

    @Override
    public void onJobTypeChange(Spinner spinner, JobSearchProperties.JOBTYPE type) {
        properties.jobType.set(type);
        addTask(SearchRequestTask.JOBTYPE);
    }

    @Override
    public void onSearch(JobSearchProperties prop) {
        addTask(SearchRequestTask.SEARCH, "Searching...");
    }

    protected String getUrl() {
        return JbmnplsHttpClient.GET_LINKS.SEARCH;
    }

    protected void showAlert(String message) {
        alert.setMessage(message);
        alert.show();
    }

    //=================
    //  List Adapter
    //=================
    private class JobSearchAdapter extends JbmnplsAdapterBase {
        public JobSearchAdapter(Activity a, int listViewResourceId, int[] viewResourceIdListInWidget,
                ArrayList<Job> list) {
            super(a, listViewResourceId, viewResourceIdListInWidget, list);
        }

        @Override
        protected HIGHLIGHTING setJobWidgetValues(Job job, View[] elements, View layout) {
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

        // Task Response states
        public static final int NO_PROBLEM = 0;
        public static final int UNKNOWN_COMMAND = 1;
        public static final int JOBTYPE_UNAUTH = 2;
        public static final int LOGOUT_RESULT = 3;
        public static final int LOST_STATE_RESULT = 4;
        public static final int CANCELLED = 5;

        public static final String LOGOUT_STRING = "uw_signin.css";
        public static final String LOST_STATE_STRING = "return to your most recent active page";

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

                    // Find the new state number
                    parser = new SimpleHtmlParser(response);
                    parser.skipText("ICStateNum");
                    stateNum = parser.getAttributeInCurrentElement("value");

                    clearList();
                    tableParser.execute(JOBSEARCH_OUTLINE, response);
                    properties.acceptChanges();
                    break;
                case VIEW100:
                    break;
                case VIEW25:
                    break;
                case NEXTPAGE:
                    break;
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

                    // TODO this request will also change some of the job levels, parse that

                    // Find the new state number
                    parser = new SimpleHtmlParser(response);
                    parser.skipText("id='ICStateNum'");
                    stateNum = parser.getAttributeInCurrentElement("value");

                    if (response.contains(getString(R.string.job_search_type_unauth))) {
                        properties.jobType.rejectChange();
                        return JOBTYPE_UNAUTH;
                    } else {
                        properties.jobType.updateChange();
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
            } catch (IOException e) {
                e.printStackTrace();
                BugSenseHandler.sendException(e);
                JobminePlusMobileLog.sendException(activity, response, e);
            }
            return NO_PROBLEM;
        }

        protected String doPost(List<NameValuePair> data, String url)
                throws JbmnplsLoggedOutException, IOException {
            data.add(new BasicNameValuePair("ICStateNum", stateNum));
            data.add(new BasicNameValuePair("ICSID", icsID));
            response = client.postJobmineHtml(data, url);

            // Check response for authorization
            if (response == null) {
                return null;
            } else if (response.contains(LOGOUT_STRING)) {
                throw new JbmnplsLoggedOutException();
            } else if (response.contains(LOST_STATE_STRING)) {
                throw new JbmnplsLostStateException();
            }
            return response;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            client.abort();
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
                searchDialog.dismiss();
                onRequestComplete(true);
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
                toast("Not possible to come here");
                break;
            case LOST_STATE_RESULT:
                toast("Went to lost state and failed");
                break;
            case LOGOUT_RESULT:
                // Go back to home screen? and show a fail?
                toast("Went to logout and failed");
                break;
            }

            // Execute next task in the list
            runTask();
        }
    }
}
