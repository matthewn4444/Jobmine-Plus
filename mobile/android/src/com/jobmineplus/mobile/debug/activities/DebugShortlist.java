package com.jobmineplus.mobile.debug.activities;

import org.apache.http.message.BasicNameValuePair;

import android.os.Bundle;

import com.jobmineplus.mobile.activities.jbmnpls.Shortlist;
import com.jobmineplus.mobile.debug.DebugApplication;
import com.jobmineplus.mobile.debug.DebugHomeActivity;
import com.jobmineplus.mobile.debug.DebugLoginActivity;

public class DebugShortlist extends Shortlist{
    private DebugApplication app;

    public static final String FAKE_SHORTLIST = 
            "http://eatthis.iblogger.org/jobmineplusmobile/list.html";

    @Override
    protected String setUp(Bundle savedInstanceState) {
        app = (DebugApplication) getApplication();

        super.setUp(savedInstanceState);
        return FAKE_SHORTLIST;
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
