package com.jobmineplus.mobile.debug;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.HomeActivity;

public final class DebugHomeActivity extends HomeActivity {

    public static boolean debugLocalhost = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("Jobmine Plus Mobile (Debug)");
    }

    public boolean goToActivity(String activityName) {
        Class<?> name = null;
        try {
            name = Class.forName(PREFIX_PATH + "debug.Debug" + activityName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        Intent in = new Intent(this, name);
        startActivity(in);
        return true;
    }

    //====================
    //  Localhost Menu
    //====================
    @Override
    protected int getMenuId() {
        return R.menu.debug_main_menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuitem_localhost) {
            debugLocalhost = !debugLocalhost;
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem localhostBtn = menu.findItem(R.id.menuitem_localhost);
        localhostBtn.setTitle(getString(debugLocalhost ? R.string.menuitem_online
                : R.string.menuitem_localhost));
        return super.onPrepareOptionsMenu(menu);
    }
}
