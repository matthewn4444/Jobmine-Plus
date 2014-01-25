package com.jobmineplus.mobile.activities;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.database.users.UserDataSource;
import com.jobmineplus.mobile.services.InterviewsAlarm;
import com.jobmineplus.mobile.widgets.DatabaseTask;
import com.jobmineplus.mobile.widgets.DatabaseTask.Action;
import com.jobmineplus.mobile.widgets.DatabaseTask.IDatabaseTask;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;

public abstract class LoggedInActivityBase extends SimpleActivityBase {
    private static InterviewsAlarm interviewsAlarm = null;
    private AlertDialog.Builder aboutDialog;

    @Override
    protected void onlineModeChanged(boolean isOnline){
        if (!isOnline) {
            cancelInterviewsAlarm();
        } else {
            startInterviewsAlarm();
        }
        super.onlineModeChanged(isOnline);
    }

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        // Setup about dialog
        aboutDialog = new Builder(this);
        aboutDialog.setNeutralButton(android.R.string.ok, null);
        aboutDialog.setTitle(R.string.about_dialog_title);
        aboutDialog.setMessage(R.string.about_dialog_message);

        // Setup the interview notifier
        if (interviewsAlarm == null) {
            synchronized (this) {
                if (interviewsAlarm == null) {
                    interviewsAlarm = new InterviewsAlarm(this, arg0);

                    if (preferences.getBoolean("settingsEnableInterCheck", false) && isOnline()) {
                        startInterviewsAlarm();
                    }
                }
            }
        }
    }

    protected void cancelInterviewsAlarm() {
        interviewsAlarm.cancel();
    }

    protected void startInterviewsAlarm() {
        int timeoutSec = Integer.parseInt(preferences.getString("settingsCheckFreq",
                getString(R.string.settings_check_freq_default_value)));
        interviewsAlarm.scheduleNextAlarm(timeoutSec * 60, client.getUsername(), client.getPassword());
    }

    protected void logout() {
        client = new JbmnplsHttpClient();

        // Going to login, do not let it auto login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(LoginActivity.DO_AUTO_LOGIN_EXTRA, false);
        startActivity(intent);

        // Remove the last user status from the database
        final Context that = this;
        new DatabaseTask<Void>(new IDatabaseTask<Void>() {
            @Override
            public Void doPutTask() {
                UserDataSource source = new UserDataSource(that);
                source.open();
                source.clearLastUser();
                source.close();
                return null;
            }

            @Override
            public Void doGetTask() {
                return null;
            }

            @Override
            public void finishedTask(Void result, Action action) {
            }
        }).executePut();
        finish();
    }

    protected int getMenuId() {
        return R.menu.main_menu;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getSupportMenuInflater();
        menuInflater.inflate(getMenuId(), menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menuitem_logout:
                logout();
                break;
            case R.id.menuitem_about:
                aboutDialog.show();
                break;
            case R.id.menuitem_no_ads:
                showJbmnplsmblProInMarket();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void showJbmnplsmblProInMarket() {
        final String appPackageName = getString(R.string.promo_package_name);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }
}
