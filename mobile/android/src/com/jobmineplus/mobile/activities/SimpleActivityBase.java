package com.jobmineplus.mobile.activities;

import com.jobmineplus.mobile.R;

import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public abstract class SimpleActivityBase extends FragmentActivity {
    protected static boolean IS_ONLINE_MODE = true;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mInflater = getMenuInflater();
        mInflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menuitem_online_mode);
        if (IS_ONLINE_MODE) {
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
                IS_ONLINE_MODE = !IS_ONLINE_MODE;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

