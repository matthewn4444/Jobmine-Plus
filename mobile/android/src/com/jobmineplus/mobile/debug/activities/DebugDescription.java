package com.jobmineplus.mobile.debug.activities;

import java.io.IOException;
import java.util.Date;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.jbmnpls.Description;
import com.jobmineplus.mobile.debug.DebugApplication;
import com.jobmineplus.mobile.debug.DebugHomeActivity;
import com.jobmineplus.mobile.debug.DebugLoginActivity;
import com.jobmineplus.mobile.widgets.Job.LEVEL;

public class DebugDescription extends Description {

    DebugApplication app;

    @Override
    protected String setUp(Bundle savedInstanceState) {
        super.setUp(savedInstanceState);
        app = (DebugApplication) getApplication();

        if (!job.hasDescriptionData() && !app.isOnline()) {
            LEVEL[] levels = {LEVEL.BACHELOR};
            String[] disc = {"System Design"};

            //Place in fake data
            job.setDescriptionData(
                    "Employername",
                    "Title Placeholder",
                    "Toronto",
                    levels,
                    new Date(),
                    new Date(),
                    false,
                    10,
                    disc,
                    "Matt Ng",
                    "Matt Ng",
                    "Nothing",
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "In porta molestie sem, quis volutpat dolor dapibus sed. Fusce " +
                    "nec orci a neque semper tempus et eu neque. Phasellus commodo " +
                    "dapibus lorem, eget volutpat mi pretium vitae. Curabitur posuere " +
                    "urna a tellus tincidunt et vehicula arcu iaculis. Sed venenatis mi" +
                    " a lorem ullamcorper sed euismod lacus euismod.");
        }
        return "";
    }

    @Override
    protected String onRequestData(String[] args) throws IOException {
        if (!app.isOnline()) {
            return "<html></html>";
        }
        return super.onRequestData(args);
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
