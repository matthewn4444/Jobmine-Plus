package com.jobmineplus.mobile.widgets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.bugsense.trace.BugSenseHandler;
import com.jobmineplus.mobile.activities.jbmnpls.JbmnplsActivityBase;
import com.jobmineplus.mobile.exceptions.JbmnplsCancelledException;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.exceptions.JbmnplsLostStateException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.table.SimpleHtmlParser;


public abstract class JbmnplsRequestQueue<TProgress> {

    // Task Response states
    protected static final int NO_PROBLEM = 0;
    protected static final int UNKNOWN_COMMAND = 1;
    protected static final int LOGOUT_RESULT = 2;
    protected static final int LOST_STATE_RESULT = 3;
    protected static final int CANCELLED = 4;
    protected static final int PARSING_ERROR = 5;
    protected static final int INACTIVE_ERROR = 6;

    protected static final String LOST_STATE_STRING = "return to your most recent active page";
    protected static final String EXCEPTION_STRING = "uw_exception.html";

    protected final Queue<Task> taskQueue = new LinkedList<Task>();
    private RequestTask currentTask;
    protected JbmnplsHttpClient client;
    private final String url;
    private final JbmnplsActivityBase activity;
    private final JbmnplsRequestQueue<TProgress> self;

    // Post Request States
    private String stateNum;
    private final String icsID;

    public JbmnplsRequestQueue(JbmnplsActivityBase a, JbmnplsHttpClient c, String postUrl, String stateNumber, String id) {
        url = postUrl;
        activity = a;
        self = this;
        client = c;
        stateNum = stateNumber;
        icsID = id;
    }

    // Accessors
    public int getCurrentCommand() {
        return currentTask != null ? currentTask.currentCommand : -1;
    }

    public boolean isRunning() {
        return currentTask != null ? currentTask.isRunning() : false;
    }

    public void cancelCurrent(boolean mayInterruptIfRunning) {
        if (currentTask != null) {
            currentTask.cancel(mayInterruptIfRunning);
        }
    }

    protected JbmnplsActivityBase getActivity() {
        return activity;
    }

    // Abstract Task Functions
    protected abstract Integer doInBackground(Integer...params) throws IOException;
    protected abstract boolean checkForInActivity();

    // Optional Task Functions
    protected void onPreExecute() {
    }
    protected void onProgressUpdate(TProgress...values) {
    }
    protected void onPostExecute(Integer result) {
    }
    protected void onCancelled() {
    }
    protected void onCancelled(Integer result) {
    }

    // Public task methods
    public void addTask(int jobCode) {
        addTask(jobCode, null);
    }

    public void addTask(int jobCode, String message) {
        addTask(jobCode, message, null);
    }

    public void addTask(int jobCode, String message, int...moreData) {
        taskQueue.add(new Task(jobCode, message, moreData));
        runTask();
    }

    public void runTask() {
        if (!taskQueue.isEmpty() && (currentTask == null || !currentTask.isRunning())) {
            Task taskInfo = taskQueue.poll();
            currentTask = new RequestTask(taskInfo.message);

            // Add extra data if exists
            Integer[] params = null;
            int[] extra = taskInfo.extraData;
            if (extra != null) {
                params = new Integer[extra.length + 1];
                params[0] = taskInfo.jobCode;
                for (int i = 0; i < extra.length; i++) {
                    params[i + 1] = extra[i];
                }
            } else {
                params = new Integer[]{taskInfo.jobCode};
            }
            currentTask.execute(params);
        }
    }

    public void cancelAllTasks() {
        taskQueue.clear();
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }

    // Call these functions within doInBackground
    protected String doPost(String icAction, NameValuePair... moreData)
            throws JbmnplsLoggedOutException, IOException {
        return doPost(icAction, Arrays.asList(moreData));
    }
    protected String doPost(String icAction, List<NameValuePair> moreData)
            throws JbmnplsLoggedOutException, IOException {
        String response = currentTask.response;

        // Create the post data
        final List<NameValuePair> postData = moreData != null
                    ? new ArrayList<NameValuePair>(moreData)
                    : new ArrayList<NameValuePair>(moreData);
        postData.add(new BasicNameValuePair("ICAction", icAction));
        postData.add(new BasicNameValuePair("ICElementNum", "0"));
        postData.add(new BasicNameValuePair("ICAJAX", "1"));
        postData.add(new BasicNameValuePair("ICStateNum", stateNum));
        postData.add(new BasicNameValuePair("ICSID", icsID));

        // Do Post
        response = client.postJobmineHtml(postData, url);

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

    // Request Task that gets spawned by this class
    private final class RequestTask extends ProgressDialogAsyncTaskBase<Integer, TProgress, Integer> {

        private String response;
        private int currentCommand;

        public RequestTask(String message) {
            super(activity, message, message != null);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            self.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            if (checkForInActivity()) {
                return INACTIVE_ERROR;
            }
            if (!client.verifyLogin()) {
                return LOGOUT_RESULT;
            }
            currentCommand = params[0];
            Integer result = NO_PROBLEM;
            try {
                result = self.doInBackground(params);
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
            return result;
        }

        @Override
        protected void onProgressUpdate(TProgress... values) {
            super.onProgressUpdate(values);
            self.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            self.onPostExecute(result);

            // Execute next task in the list
            runTask();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            self.onCancelled();
        }

        @Override
        protected void onCancelled(Integer result) {
            super.onCancelled(result);
            self.onCancelled(result);
        }
    }

    private final class Task {
        public int jobCode;
        public String message;
        public int[] extraData;

        public Task(int code, String msg, int... data) {
            jobCode = code;
            message = msg;
            extraData = data;
        }

    }
}
