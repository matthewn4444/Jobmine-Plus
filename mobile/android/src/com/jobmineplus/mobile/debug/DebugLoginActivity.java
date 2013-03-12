package com.jobmineplus.mobile.debug;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.LoginActivity;

public class DebugLoginActivity extends LoginActivity implements Debuggable {

    protected DebugApplication app;

    @Override
    protected void onStart() {
        super.onStart();
        app = (DebugApplication) getApplication();
    }

    @Override
    protected void goToHomeActivity() {
        Intent myIntent = new Intent(this, DebugHomeActivity.class);
        startActivity(myIntent);
        finish();
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
                setOnlineMode(!isOnline());
                return true;
            case R.id.menuitem_real_site:
                app.toggleOnline();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
