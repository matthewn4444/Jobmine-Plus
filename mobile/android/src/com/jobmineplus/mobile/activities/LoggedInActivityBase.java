package com.jobmineplus.mobile.activities;

import android.os.Bundle;

import com.jobmineplus.mobile.services.InterviewsAlarm;

public abstract class LoggedInActivityBase extends SimpleActivityBase {
    protected static InterviewsAlarm interviewsAlarm = null;

    protected static final int MINIMUM_ALARM_TIMEOUT = 10;       // TODO change this to 10 min

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
        interviewsAlarm.scheduleNextAlarm(MINIMUM_ALARM_TIMEOUT);
    }
}
