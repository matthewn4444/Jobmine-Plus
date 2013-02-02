package com.jobmineplus.mobile.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class InterviewsAlarm extends BroadcastReceiver {
    private final int SEC_TO_MILLISEC = 1000;
    private final int MINIMUM_TIMEOUT = 10;
    public static final String BUNDLE_TIMEOUT = "InterviewAlarm.timeout";
    public static final String BUNDLE_NAME = "InterviewAlarm.bundle";

    private Context ctx;

    // Calls by alarm manager
    public InterviewsAlarm() {
    }

    /**
     * Call this to start the alarm.
     * @param context
     * @param extras
     * @param timeoutSeconds
     */
    public InterviewsAlarm(Context context, Bundle extras) {
        ctx = context;
    }

    public void scheduleNextAlarm(int timeoutSeconds) {
        long triggerTime = System.currentTimeMillis() + timeoutSeconds * SEC_TO_MILLISEC;
        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_TIMEOUT, timeoutSeconds);
        getAlarmManager().set(AlarmManager.RTC_WAKEUP, triggerTime, getPendingIntent(bundle));
    }

    public void cancel() {
        getAlarmManager().cancel(getPendingIntent());
    }

    private PendingIntent getPendingIntent() {
        return getPendingIntent(null);
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    }

    private PendingIntent getPendingIntent(Bundle bundle) {
        Intent in = new Intent(ctx, InterviewsAlarm.class);
        if (bundle != null) {
            in.putExtra(BUNDLE_NAME, bundle);
        }
        return PendingIntent.getBroadcast(ctx, 0, in, PendingIntent.FLAG_ONE_SHOT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ctx = context;
        Toast.makeText(context, "Alarm went off", Toast.LENGTH_SHORT).show();

        // Get the stored timeout
        Bundle bundle = intent.getBundleExtra(BUNDLE_NAME);
        if (bundle == null) {
            // If there is no bundle, then the service scheduled next crawl but we cancelled it
            return;
        }
        int nextTimeout = bundle.getInt(BUNDLE_TIMEOUT);
        if (nextTimeout == 0) {
            nextTimeout = MINIMUM_TIMEOUT;
        }

        // Start the service
        Intent interviewsService = new Intent(context,
                InterviewsNotifierService.class);
        interviewsService.putExtra(BUNDLE_TIMEOUT, nextTimeout);
        context.startService(interviewsService);        // TODO try catch here
    }
}
