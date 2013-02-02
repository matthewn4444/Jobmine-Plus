package com.jobmineplus.mobile.activities;

import com.jobmineplus.mobile.R;

import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public abstract class SimpleActivityBase extends FragmentActivity {
    private static boolean isOnlineMode = true;

    protected void setOnlineMode(boolean flag) {
        synchronized (this) {
            isOnlineMode = flag;
        }
        onlineModeChanged(flag);
    }

    protected boolean isOnline() {
        return isOnlineMode;
    }

    // Override this function to detect online status change, call super as well
    protected void onlineModeChanged(boolean isOnline){}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mInflater = getMenuInflater();
        mInflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menuitem_online_mode);
        if (isOnline()) {
            item.setTitle("Go Offline");
        } else {
            item.setTitle("Go Online");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menuitem_online_mode:
                setOnlineMode(!isOnlineMode);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

