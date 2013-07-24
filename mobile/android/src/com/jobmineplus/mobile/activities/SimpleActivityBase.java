package com.jobmineplus.mobile.activities;

import java.util.Date;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;
import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public abstract class SimpleActivityBase extends SherlockFragmentActivity {
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!preferences.getBoolean("settingsEnableDataCrashReports", false)) {
            BugSenseHandler.sendDataOverWiFiOnly();
        }
        if (!isDebug()) {
            BugSenseHandler.initAndStartSession(SimpleActivityBase.this, getString(R.string.bugsence_api_key));
        }
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
        boolean wifiConnected = mWifi != null ? mWifi.isConnected() : false;
        boolean mobileConnected = mMobile != null ? mMobile.isConnected() : false;
        return (wifiConnected || mobileConnected) && isNetworkAvailable();
    }

    public static boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = connManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public boolean isDebug() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    protected void setOnlineMode(boolean flag) {
        synchronized (this) {
            isOnlineMode = flag;
            supportInvalidateOptionsMenu();
        }
        onlineModeChanged(flag);
        if (flag) {
            Toast.makeText(this, R.string.set_online_mode_message, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.set_offline_mode_message, Toast.LENGTH_SHORT).show();
        }
    }

    // Online mode set by user
    protected boolean isOnline() {
        return isOnlineMode;
    }

    // Not truly online unless network is connecting and working
    protected boolean isReallyOnline() {
        return isOnlineMode && isJobmineOnline() && isNetworkConnected();
    }

    // Override this function to detect online status change, call super as well
    protected void onlineModeChanged(boolean isOnline){}

    private void setOnlineIcon(MenuItem button) {
        if(isOnline()){
            button.setIcon(R.drawable.ic_online);
        }else{
            button.setIcon(R.drawable.ic_offline);
        }
    }

    /*
     * Options menu creation
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    protected int getActionBarId() {
        return R.menu.actionbar;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       MenuInflater inflater = getSupportMenuInflater();
       inflater.inflate(getActionBarId(), menu);
       return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem switchButton = menu.findItem(R.id.action_online);
        setOnlineIcon(switchButton);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        case R.id.action_online:
            setOnlineMode(!isOnlineMode);
            setOnlineIcon(item);
            break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        supportInvalidateOptionsMenu();
        super.onResume();

        // If adbanner exists, then show it
        final AdView adview = ((AdView)findViewById(R.id.adView));
        if (adview != null) {
            (new Thread() {
                public void run() {
                    Looper.prepare();
                    adview.loadAd(new AdRequest());
                }
            }).start();
        }
    }

    protected void toast(String message) {
        if (isDebug()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

