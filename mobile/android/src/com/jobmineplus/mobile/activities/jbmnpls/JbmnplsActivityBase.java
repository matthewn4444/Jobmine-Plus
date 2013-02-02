package com.jobmineplus.mobile.activities.jbmnpls;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.HomeActivity;
import com.jobmineplus.mobile.activities.LoggedInActivityBase;
import com.jobmineplus.mobile.activities.LoginActivity;
import com.jobmineplus.mobile.database.jobs.JobDataSource;
import com.jobmineplus.mobile.database.pages.PageDataSource;
import com.jobmineplus.mobile.exceptions.HiddenColumnsException;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.Alert;
import com.jobmineplus.mobile.widgets.DatabaseTask;
import com.jobmineplus.mobile.widgets.DatabaseTask.Action;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.ProgressDialogAsyncTaskBase;
import com.jobmineplus.mobile.widgets.DatabaseTask.IDatabaseTask;
import com.jobmineplus.mobile.widgets.StopWatch;

public abstract class JbmnplsActivityBase extends LoggedInActivityBase implements IDatabaseTask<Void> {

    // =================
    // Declarations
    // =================
    protected static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy");

    private String dataUrl = null; // Use JbmnPlsHttpService.GET_LINKS.<url>

    protected ArrayList<Job> allJobs;
    protected GetHtmlTask task = null;
    protected JobDataSource jobDataSource;
    protected PageDataSource pageDataSource;
    protected final String LOADING_MESSAGE = "Fetching data...";
    protected long timestamp;
    protected String pageName;
    private Alert alert;
    private DatabaseTask<Void> databaseTask;

    // ====================
    // Abstract Methods
    // ====================

    /**
     * Running setUp() you need to specify the "layout" and "dataUrl".
     * For example:
     *      protected void setUp() {
     *          setContentView(layout);
     *          String url = JbmnplsHttpService.GET_LINKS.APPLICATIONS;
     *          return url;
     *      }
     *
     * @param savedInstanceState
     * @return dataUrl (String) that gets the data that will be parsed
     */
    protected abstract String setUp(Bundle savedInstanceState);

    /**
     * This allows the user to define all their UI object variables here. is
     * necessary but does not always need it. Please specify layout in setUp();
     */
    protected abstract void defineUI(Bundle savedInstanceState);

    /**
     * Here you are given the document of the dataUrl page specified in setUp().
     * Also render the layout with the data here.
     *
     * @param doc
     */
    protected abstract void parseWebpage(String html);

    /**
     * Calling this when the parseWebpage is complete. This is used when you
     * need to update any visual element that you cannot do in parseWebpage
     *
     * @param doc
     */
    protected abstract void onRequestComplete();

    protected abstract void doOffine();


    // ====================
    // Override Methods
    // ====================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        allJobs = new ArrayList<Job>();
        jobDataSource = new JobDataSource(this);
        pageDataSource = new PageDataSource(this);
        databaseTask = new DatabaseTask<Void>(this);
        jobDataSource.open();
        pageDataSource.open();
        alert = new Alert(this);
        pageName = null;
        dataUrl = setUp(savedInstanceState);
        defineUI(savedInstanceState);
        requestData();
    }

    @Override
    protected void onResume() {
        jobDataSource.open();
        pageDataSource.open();
        super.onResume();
    }

    @Override
    protected void onPause() {
        jobDataSource.close();
        pageDataSource.close();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        jobDataSource.close();
        pageDataSource.close();
        super.onDestroy();
    }

    // ========================
    // Login/Logout Methods
    // ========================
    protected boolean verifyLogin() {
        return client.verifyLogin();
    }

    // ======================
    // Activity Movements
    // ======================

    protected void goToLoginActivity(String reasonMsg) {
        startActivityWithMessage(LoginActivity.class, reasonMsg);
    }

    protected void goToHomeActivity(String reasonMsg) {
        startActivityWithMessage(HomeActivity.class, reasonMsg);
    }

    protected void goToDescription(int jobId) {
        BasicNameValuePair pass = new BasicNameValuePair("jobId",
                Integer.toString(jobId));
        startActivity(Description.class, pass);
    }

    protected void startActivityWithMessage(Class<?> cls, String reasonMsg) {
        Intent in = new Intent(this, cls);
        in.putExtra("reason", reasonMsg);
        startActivity(in);
        finish();
    }

    protected void startActivity(Class<?> goToClass) {
        NameValuePair[] empty = null;
        startActivity(goToClass, empty);
    }

    protected void startActivity(Class<?> goToClass, NameValuePair... args) {
        Intent in = new Intent(this, goToClass);
        if (args != null) {
            for (NameValuePair arg : args) {
                in.putExtra(arg.getName(), arg.getValue());
            }
        }
        startActivity(in);
    }

    // ======================
    // Database Task Members
    // ======================
    public Void doPutTask() {
        jobDataSource.addJobs(allJobs);
        if (pageName != null) {
            pageDataSource.addPage(client.getUsername(), pageName, allJobs, timestamp);
        }
        return null;
    }

    public Void doGetTask() {
        doOffine();
        return null;
    }

    public void finishedTask(Void result, DatabaseTask.Action action) {
        if (action == Action.GET) {
            onRequestComplete();
        }
    }

    // =================
    // Miscellaneous
    // =================

    protected void jobsToDatabase() {
        if (isOnline()) {
            databaseTask.executePut();
        }
    }

    protected void addJob(Job job) {
        allJobs.add(job);
    }

    protected boolean isLoading() {
        return task != null && task.isRunning();
    }

    protected void log(Object... txt) {
        String returnStr = "";
        int i = 1;
        int size = txt.length;
        if (size != 0) {
            returnStr = txt[0] == null ? "null" : txt[0].toString();
            for (; i < size; i++) {
                returnStr += ", "
                        + (txt[i] == null ? "null" : txt[i].toString());
            }
        }
        System.out.println(returnStr);
    }

    protected void showMessage(String message) {
        alert.show(message);
    }

    // ====================================
    // Data Request Classes and Methods
    // ====================================

    protected void requestData() throws RuntimeException {
        if (dataUrl == null) {
            throw new RuntimeException(
                    "Class that extended JbmnPlsActivityBase without specifying a dataurl.");
        }
        if (isOnline()) {
            task = new GetHtmlTask(this, LOADING_MESSAGE);
            task.execute(dataUrl);
        } else {
            databaseTask.executeGet();
        }
    }

    /**
     * You can override this function if you need to fetch something else
     * besides the default url. Return null if it failed and this class will
     * throw a dialog saying it failed otherwise return the html
     *
     * @param url
     * @return null if failed or String that is the html
     * @throws IOException
     */
    protected String onRequestData(String[] args)
            throws JbmnplsLoggedOutException, IOException {
        String url = args[0];
        return client.getJobmineHtml(url);
    }

    private class GetHtmlTask extends
            ProgressDialogAsyncTaskBase<String, Void, Integer> {

        static final int NO_PROBLEM = 0;
        static final int FORCED_LOGGEDOUT = 1;
        static final int GO_HOME_NO_REASON = 2;
        static final int HIDDEN_COLUMNS_ERROR = 3;
        static final int PARSING_ERROR = 4;
        static final int NETWORK_ERROR = 5;

        private final StopWatch sw = new StopWatch();
        protected Activity a;

        public GetHtmlTask(Activity activity, String dialogueMessage) {
            super(activity, dialogueMessage, isOnline());
            a = activity;
        }

        @Override
        protected Integer doInBackground(String... params) {
            JbmnplsActivityBase activity = (JbmnplsActivityBase) getActivity();
            // We are in online mode
            if (!verifyLogin()) {
                return FORCED_LOGGEDOUT;
            }
            sw.start();
            try {
                String html = activity.onRequestData(params);
                timestamp = System.currentTimeMillis();
                if (html == null) {
                    return PARSING_ERROR;
                }
                activity.parseWebpage(html);
                return NO_PROBLEM;
            } catch (HiddenColumnsException e) {
                e.printStackTrace();
                return HIDDEN_COLUMNS_ERROR;
            } catch (JbmnplsParsingException e) {
                e.printStackTrace();
                return PARSING_ERROR;
            } catch (JbmnplsLoggedOutException e) {
                e.printStackTrace();
                return FORCED_LOGGEDOUT;
            } catch (IOException e) {
                e.printStackTrace();
                return NETWORK_ERROR;
            }
        }

        @Override
        protected void onPostExecute(Integer reasonForFailure) {
            super.onPostExecute(reasonForFailure);
            log(sw.elapsed() + " ms to render");
            Toast.makeText(a, sw.elapsed() + " to get", Toast.LENGTH_SHORT).show();
            if (reasonForFailure == NO_PROBLEM) {
                onRequestComplete();
            } else {
                switch (reasonForFailure) {
                case FORCED_LOGGEDOUT:
                    goToLoginActivity(getString(R.string.jobmine_offline_message));
                    break;
                case PARSING_ERROR:
                    goToHomeActivity(getString(R.string.parsing_error_message));
                    break;
                case HIDDEN_COLUMNS_ERROR:
                    goToHomeActivity(getString(R.string.hidden_column_message));
                    break;
                case NETWORK_ERROR:
                    goToHomeActivity(getString(R.string.network_error));
                    break;
                case GO_HOME_NO_REASON:
                    goToHomeActivity("");
                    break;
                }
                finish();
            }
        }
    }
}