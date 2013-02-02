package com.jobmineplus.mobile.activities;

import android.os.Bundle;

import com.jobmineplus.mobile.services.InterviewsAlarm;

public abstract class LoggedInActivityBase extends SimpleActivityBase {
    protected static InterviewsAlarm interviewsAlarm = null;

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
        if (interviewsAlarm == null) {
            synchronized (this) {
                if (interviewsAlarm == null) {
                    interviewsAlarm = new InterviewsAlarm(this, arg0);

                    // TODO read database for setting to turn this feature on
                    if (isOnline()) {
                        startInterviewsAlarm();
                    }
                }
            }
        }
        super.onCreate(arg0);
    }

    protected void cancelInterviewsAlarm() {
        interviewsAlarm.cancel();
    }

    protected void startInterviewsAlarm() {
        interviewsAlarm.scheduleNextAlarm(InterviewsAlarm.MINIMUM_TIMEOUT,
                client.getUsername(), client.getPassword());
    }
}
