package com.jobmineplus.mobile.widgets;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.jobmineplus.mobile.activities.SimpleActivityBase;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

public final class JobminePlusMobileLog {
    private static Object lock = new Object();
    public static final String SEND_HANDLED_EXCEPTION_URL = "http://jobminepluslog.herokuapp.com/commonexceptions/";
    private static JobminePlusMobileLog instance;
    private static String version = null;

    private JbmnplsHttpClient client;
    //private LogPostTask task;

    private JobminePlusMobileLog() {
        client = new JbmnplsHttpClient();
    }

    static private void insureInstance() {
        synchronized (lock) {
            if (instance == null) {
                synchronized (lock) {
                    instance = new JobminePlusMobileLog();
                }
            }
        }
    }

    public static void sendException(SimpleActivityBase activity, String text, Exception e) {
        if (activity.isDebug()) {
            return;
        }
        insureInstance();

        if (version == null) {
            try {
                version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
            } catch (NameNotFoundException e1) {
                e1.printStackTrace();
                version = "Unknown";
            }
        }
        String stackTrace = Log.getStackTraceString(e);
        instance.spawnTask(text, e.getLocalizedMessage(), stackTrace);
    }

    private void spawnTask(String text, String exceptionMessage, String stackTrace) {
        LogPostTask task = new LogPostTask();
        task.execute(text, exceptionMessage, stackTrace);
    }

    private final class LogPostTask extends AsyncTask<String, Void, Void> {
        private HttpResponse internalSendException(String text, String exceptionMessage, String stackTrace) {
            List<NameValuePair> postData = new ArrayList<NameValuePair>(2);
            postData.add(new BasicNameValuePair("message", exceptionMessage));
            postData.add(new BasicNameValuePair("version", version));
            postData.add(new BasicNameValuePair("trace", stackTrace));
            postData.add(new BasicNameValuePair("text", text != null ? text : ""));
            return instance.client.post(postData, SEND_HANDLED_EXCEPTION_URL);
        }

        @Override
        protected Void doInBackground(String... params) {
            // Try again, if fail again then give up
            if (internalSendException(params[0], params[1], params[2]) == null) {
                internalSendException(params[0], params[1], params[2]);
            }
            return null;
        }
    }
}
