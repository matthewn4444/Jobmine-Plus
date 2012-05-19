package com.jobmineplus.mobile.debug.activities;

import org.apache.http.message.BasicNameValuePair;

import android.os.Bundle;

import com.jobmineplus.mobile.activities.jbmnpls.Interviews;
import com.jobmineplus.mobile.debug.DebugApplication;
import com.jobmineplus.mobile.debug.DebugHomeActivity;
import com.jobmineplus.mobile.debug.DebugLoginActivity;

public class DebugInterviews extends Interviews{
    private DebugApplication app;

    public static final String FAKE_INTERVIEWS = 
            "http://eatthis.iblogger.org/jobmineplusmobile/Interviews.html";

    @Override
    protected String setUp(Bundle savedInstanceState) {
        String returnVal = super.setUp(savedInstanceState);
        app = (DebugApplication) getApplication();

        if (!app.isOffline()) {
            return returnVal;
        }
        return FAKE_INTERVIEWS;
    }

    @Override
    protected void goToDescription(int jobId) {
        BasicNameValuePair pass = new BasicNameValuePair("jobId",
                Integer.toString(jobId));
        startActivity(DebugDescription.class, pass);
    }

    //Must use this for every debug activity
    @Override
    protected void goToLoginActivity(String reasonMsg) {
        startActivityWithMessage(DebugLoginActivity.class, reasonMsg);
    }

    @Override
    protected void goToHomeActivity(String reasonMsg) {
        startActivityWithMessage(DebugHomeActivity.class, reasonMsg);
    }

    @Override
    protected boolean verifyLogin() {
        if (app.isOffline()) {
            return true;
        } else {
            return super.verifyLogin();
        }
    } 
}
