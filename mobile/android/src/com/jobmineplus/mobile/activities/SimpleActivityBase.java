package com.jobmineplus.mobile.activities;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;

public abstract class SimpleActivityBase extends SherlockFragmentActivity {
    final static public int JBMN_OFFLINE_TIME = 24;     //24 hour clock
    final static public int JBMN_ONLINE_TIME = 7;        //Opens at 6am

    private static ConnectivityManager connManager;
    private static boolean isOnlineMode = true;
    protected static JbmnplsHttpClient client = new JbmnplsHttpClient();
    protected SharedPreferences preferences = null;
    private final IntentFilter filter = new IntentFilter();
    private boolean hasRegisteredNetworkReceiver = false;

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

    //===================
    //  Online Modes
    //===================
    public static boolean isJobmineOnline() {
        Date now = new Date();
        int hour = now.getHours();
        int day = now.getDay();
        return (day == 6 && hour >= JBMN_ONLINE_TIME || day <= 1)
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

    //===================
    //  Network Changed
    //===================
    protected void onNetworkStateChanged(boolean connected, Context context, Intent intent) {
    }
    protected void onNetworkConnectionChanged(boolean connected) {
    }
    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        private boolean connected = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean newConnection = isNetworkConnected();
            onNetworkStateChanged(newConnection, context, intent);
            if (newConnection != connected) {
                onNetworkConnectionChanged(newConnection);
            }
            connected = newConnection;
        }
    };

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
            AdRequest adRequest = new AdRequest.Builder().build();
            adview.loadAd(adRequest);
        }

        // Register the network receiver
        if (!hasRegisteredNetworkReceiver) {
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(networkReceiver, filter);
            hasRegisteredNetworkReceiver = true;
        }
    }

    @Override
    protected void onPause() {
        // Try to unregister the network receiver
        if (hasRegisteredNetworkReceiver) {
            try {
                unregisterReceiver(networkReceiver);
                hasRegisteredNetworkReceiver = false;
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("Reciever not register")) {
                    Log.w("jbmnplsmbl", "Tried to unregister the receiver when not registered.");
                } else {
                    throw e;
                }
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Try to unregister the network receiver
        if (hasRegisteredNetworkReceiver) {
            try {
                unregisterReceiver(networkReceiver);
                hasRegisteredNetworkReceiver = false;
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("Reciever not register")) {
                    Log.w("jbmnplsmbl", "Tried to unregister the receiver when not registered.");
                } else {
                    throw e;
                }
            }
        }
        super.onDestroy();
    }

    protected <T> boolean isOneOf(T value, T... list) {
        for (T item: list) {
            if (value.equals(item)) {
                return true;
            }
        }
        return false;
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

