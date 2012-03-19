package com.jobmineplus.mobile.debug.activities;

import org.apache.http.message.BasicNameValuePair;

import android.os.Bundle;

import com.jobmineplus.mobile.activities.jbmnpls.Applications;
import com.jobmineplus.mobile.debug.DebugApplication;
import com.jobmineplus.mobile.debug.DebugHomeActivity;
import com.jobmineplus.mobile.debug.DebugLoginActivity;
import com.jobmineplus.mobile.debug.Debuggable;

public class DebugApplications extends Applications implements Debuggable {

    private DebugApplication app;

    public static final String FAKE_APPLICATIONS = 
            "http://eatthis.iblogger.org/jobmineplusmobile/Applications.html";

    @Override
    protected String setUp(Bundle savedInstanceState) {
        app = (DebugApplication) getApplication();

        super.setUp(savedInstanceState);
        return FAKE_APPLICATIONS;
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
