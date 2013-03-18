package com.jobmineplus.mobile.activities;

import android.os.Bundle;

import com.jobmineplus.mobile.services.InterviewsAlarm;

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
}
