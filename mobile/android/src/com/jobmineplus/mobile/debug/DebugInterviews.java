package com.jobmineplus.mobile.debug;

import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.jbmnpls.Interviews;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;

public class DebugInterviews extends Interviews {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("Interviews (Debug)");
    }

    @Override
    public String getUrl() {
        return DebugHomeActivity.debugLocalhost ?
                "http://10.0.2.2:1111/interviews/"
                : JbmnplsHttpClient.GET_LINKS.INTERVIEWS;
    }

    @Override
    protected boolean verifyLogin() {
        if (DebugHomeActivity.debugLocalhost) {
            return true;
        }
        return super.verifyLogin();
    }

    @Override
    protected boolean isReallyOnline() {
        if (DebugHomeActivity.debugLocalhost) {
            return isOnline() && isNetworkConnected();
        } else {
            return super.isReallyOnline();
        }
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
            DebugHomeActivity.debugLocalhost = !DebugHomeActivity.debugLocalhost;
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem localhostBtn = menu.findItem(R.id.menuitem_localhost);
        localhostBtn.setTitle(getString(DebugHomeActivity.debugLocalhost
                ? R.string.menuitem_online : R.string.menuitem_localhost));
        return super.onPrepareOptionsMenu(menu);
    }
}
