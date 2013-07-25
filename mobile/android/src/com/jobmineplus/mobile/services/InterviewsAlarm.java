package com.jobmineplus.mobile.services;

import com.jobmineplus.mobile.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class InterviewsAlarm extends BroadcastReceiver {
    private final int SEC_TO_MILLISEC = 1000;
    public static final String BUNDLE_TIMEOUT = "InterviewAlarm.timeout";
    public static final String BUNDLE_USERNAME = "InterviewAlarm.username";
    public static final String BUNDLE_PASSWORD = "InterviewAlarm.password";
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

    public void scheduleNextAlarm(int timeoutSeconds, String username, String password) {
        long triggerTime = System.currentTimeMillis() + timeoutSeconds * SEC_TO_MILLISEC;
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_USERNAME, username);
        bundle.putString(BUNDLE_PASSWORD, password);
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

        // Get the stored timeout
        Bundle bundle = intent.getBundleExtra(BUNDLE_NAME);
        if (bundle == null) {
            // If there is no bundle, then the service scheduled next crawl but we cancelled it
            return;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        int nextTimeout = Integer.parseInt(preferences.getString("settingsCheckFreq",
                context.getString(R.string.settings_check_freq_default_value))) * 60;

        // Start the service
        Intent interviewsService = new Intent(context, InterviewsNotifierService.class);
        interviewsService.putExtra(BUNDLE_TIMEOUT, nextTimeout);
        interviewsService.putExtra(BUNDLE_USERNAME, bundle.getString(BUNDLE_USERNAME));
        interviewsService.putExtra(BUNDLE_PASSWORD, bundle.getString(BUNDLE_PASSWORD));

        context.startService(interviewsService);        // TODO try catch here
    }
}
