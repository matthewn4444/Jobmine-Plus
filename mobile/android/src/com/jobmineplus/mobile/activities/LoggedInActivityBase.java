package com.jobmineplus.mobile.activities;

import android.os.Bundle;

import com.jobmineplus.mobile.services.InterviewsAlarm;

public abstract class LoggedInActivityBase extends SimpleActivityBase {
    protected static InterviewsAlarm interviewsAlarm = null;

    protected static final int INTERVIEWS_ALARM_TIMEOUT = 4;

    @Override
    protected void onCreate(Bundle arg0) {
        if (interviewsAlarm == null) {
            synchronized (this) {
                if (interviewsAlarm == null) {
                    interviewsAlarm = new InterviewsAlarm(this, arg0);

                    // TODO: Get the applications from db and see if we need to crawl
                    // Make sure there is at least one active app with no employed included
                    interviewsAlarm.scheduleNextAlarm(INTERVIEWS_ALARM_TIMEOUT);
                }
            }
        }
        super.onCreate(arg0);
    }

    protected void cancelInterviewsAlarm() {
        if (!interviewsAlarm.isAlarmActive()) {
            synchronized (this) {
                if (!interviewsAlarm.isAlarmActive()) {
                    interviewsAlarm.cancel();
                }
            }
        }
    }

    protected void startInterviewsAlarm() {
        startInterviewsAlarm(null);
    }

    protected void startInterviewsAlarm(Bundle arg0) {
        if (!interviewsAlarm.isAlarmActive()) {
            synchronized (this) {
                if (!interviewsAlarm.isAlarmActive()) {
                    interviewsAlarm.scheduleNextAlarm(INTERVIEWS_ALARM_TIMEOUT);
                }
            }
        }
    }
}
