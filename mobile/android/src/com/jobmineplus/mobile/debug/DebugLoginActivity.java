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

        String username = getString(R.string.username);
        String password = getString(R.string.password);

        if (!app.isOffline()) {
            doLogin(username, password);
        } else {
            service.setLoginCredentials(username, password);
            goToHomeActivity();
        }
    }

    @Override
    protected void goToHomeActivity() {
        Intent myIntent = new Intent(this, DebugHomeActivity.class);
        startActivity(myIntent);
        finish();
    }
}
