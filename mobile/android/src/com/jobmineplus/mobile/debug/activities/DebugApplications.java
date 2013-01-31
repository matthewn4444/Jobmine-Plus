package com.jobmineplus.mobile.debug.activities;

import org.apache.http.message.BasicNameValuePair;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.jbmnpls.Applications;
import com.jobmineplus.mobile.debug.DebugApplication;
import com.jobmineplus.mobile.debug.DebugHomeActivity;
import com.jobmineplus.mobile.debug.DebugLoginActivity;
import com.jobmineplus.mobile.debug.Debuggable;

public class DebugApplications extends Applications implements Debuggable {

    private DebugApplication app;

    public static final String FAKE_APPLICATIONS = "http://10.0.2.2:1111/applications/";
        // "http://eatthis.iblogger.org/jobmineplusmobile/Applications.html";

    @Override
    protected String setUp(Bundle savedInstanceState) {
        String returnVal = super.setUp(savedInstanceState);
        app = (DebugApplication) getApplication();
        if (app.isOnline()) {
            return returnVal;
        }
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
        if (!app.isOnline()) {
            return true;
        } else {
            return super.verifyLogin();
        }
    }

    // =================
    //  Menu buttons
    // =================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mInflater = getMenuInflater();
        mInflater.inflate(R.menu.debug_main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menuitem_real_site);
        if (app.isOnline()) {
            item.setTitle("Go Fake Site");
        } else {
            item.setTitle("Go Real Site");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menuitem_online_mode:
                IS_ONLINE_MODE = !IS_ONLINE_MODE;
                return true;
            case R.id.menuitem_real_site:
                app.toggleOnline();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
