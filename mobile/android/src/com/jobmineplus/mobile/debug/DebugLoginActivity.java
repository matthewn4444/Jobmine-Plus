package com.jobmineplus.mobile.debug;

import android.content.Intent;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.LoginActivity;

public class DebugLoginActivity extends LoginActivity implements Debuggable {

    protected DebugApplication app;
    
    @Override
    protected void onStart() {
        super.onStart();
        app = (DebugApplication) getApplication();
        
        if (!app.isOffline()) {
            doLogin();
        } else {
            goToHomeActivity();
        }
    }

    @Override
    protected void doLogin() {
        new AsyncLoginTask(this).execute(getString(R.string.username),
                getString(R.string.password));
    }
    
    @Override
    protected void goToHomeActivity() {
        Intent myIntent = new Intent(this, DebugHomeActivity.class);
        startActivity(myIntent);
        finish();
    }
}
