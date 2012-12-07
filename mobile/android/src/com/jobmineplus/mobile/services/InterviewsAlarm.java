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
    private final int DEFAULT_TIMEOUT = 10;
    private final String BUNDLE_TIMEOUT = "InterviewAlarm.timeout";
    private final String BUNDLE_NAME = "InterviewAlarm.bundle";

    private boolean isActive = false;
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
        if (!isActive) {
            long triggerTime = System.currentTimeMillis() + timeoutSeconds * SEC_TO_MILLISEC;
            Bundle bundle = new Bundle();
            bundle.putInt(BUNDLE_TIMEOUT, timeoutSeconds);
            getAlarmManager().set(AlarmManager.RTC_WAKEUP, triggerTime, getPendingIntent(bundle));
            isActive = true;
        }
    }

    public boolean isAlarmActive() {
        return isActive;
    }

    public void cancel() {
        getAlarmManager().cancel(getPendingIntent());
        isActive = false;
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
        isActive = false;
        ctx = context;
        Toast.makeText(context, "Alarm went off", Toast.LENGTH_SHORT).show();

        // Start the service
        Intent interviewsService = new Intent(context,
                InterviewsNotifierService.class);
        context.startService(interviewsService);

        // Start the next timeout
        int nextTimeout = intent.getBundleExtra(BUNDLE_NAME).getInt(BUNDLE_TIMEOUT);
        if (nextTimeout == 0) {
            nextTimeout = DEFAULT_TIMEOUT;
        }
//        scheduleNextAlarm(nextTimeout);       // Use later
    }
}
