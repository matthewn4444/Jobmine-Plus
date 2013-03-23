package com.jobmineplus.mobile.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.database.users.UserDataSource;
import com.jobmineplus.mobile.services.InterviewsAlarm;
import com.jobmineplus.mobile.widgets.DatabaseTask;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import com.jobmineplus.mobile.widgets.DatabaseTask.Action;
import com.jobmineplus.mobile.widgets.DatabaseTask.IDatabaseTask;

public abstract class LoggedInActivityBase extends SimpleActivityBase {
    private static InterviewsAlarm interviewsAlarm = null;

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
        int timeoutSec = Integer.parseInt(preferences.getString("settingsCheckFreq", "10"));
        interviewsAlarm.scheduleNextAlarm(timeoutSec * 60, client.getUsername(), client.getPassword());
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getSupportMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menuitem_logout:
                // Conduct logout now, clear the client
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
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
