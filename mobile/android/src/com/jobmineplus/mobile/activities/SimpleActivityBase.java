package com.jobmineplus.mobile.activities;

import java.util.Date;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;

import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public abstract class SimpleActivityBase extends FragmentActivity {
    final static public int JBMN_OFFLINE_TIME = 24;     //24 hour clock
    final static public int JBMN_ONLINE_TIME = 7;        //Opens at 6am

    private static ConnectivityManager connManager;
    private static boolean isOnlineMode = true;
    protected static JbmnplsHttpClient client = new JbmnplsHttpClient();
    protected SharedPreferences preferences = null;

    @Override
    protected void onCreate(Bundle arg0) {
        connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate(arg0);
    }

    public static boolean isJobmineOnline() {
        Date now = new Date();
        int hour = now.getHours();
        int day = now.getDay();
        return (day == 6 && hour >= JBMN_ONLINE_TIME || day == 0)
                || (hour >= JBMN_ONLINE_TIME && hour < JBMN_OFFLINE_TIME);
    }

    public static boolean isNetworkConnected() {
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mMobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return (mWifi.isConnected() || mMobile.isConnected()) && isNetworkAvailable();
    }

    public static boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = connManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    protected void setOnlineMode(boolean flag) {
        synchronized (this) {
            isOnlineMode = flag;
        }
        onlineModeChanged(flag);
    }

    // Not truly online unless network is connecting and working
    protected boolean isOnline() {
        return isOnlineMode && isJobmineOnline() && isNetworkConnected();
    }

    // Override this function to detect online status change, call super as well
    protected void onlineModeChanged(boolean isOnline){}

    /*
     * Options menu creation
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
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

    protected void log(Object... txt) {
        String returnStr = "";
        int i = 1;
        int size = txt.length;
        if (size != 0) {
            returnStr = txt[0] == null ? "null" : txt[0].toString();
            for (; i < size; i++) {
                returnStr += ", "
                        + (txt[i] == null ? "null" : txt[i].toString());
            }
        }
        Log.i("jbmnplsmbl", returnStr);
    }
}

