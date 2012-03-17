package com.jobmineplus.mobile.debug.activities;

import java.util.Date;

import android.os.Bundle;

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
        
        if (app.isOffline()) {
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
    protected String onRequestData(String[] args) {
        if (app.isOffline()) {
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
        if (app.isOffline()) {
            return true;
        } else {
            return super.verifyLogin();
        }
    }
}
