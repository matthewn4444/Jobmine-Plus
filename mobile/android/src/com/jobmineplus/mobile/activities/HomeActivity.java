package com.jobmineplus.mobile.activities;

import java.util.Calendar;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.widgets.InterstitialAdHelper;
import com.jobmineplus.mobile.widgets.TutorialHelper;

public class HomeActivity extends LoggedInActivityBase implements OnClickListener{
    protected int[] buttonLayouts = {
            R.id.search_button,
            R.id.apps_button,
            R.id.shortlist_button,
            R.id.interviews_button,
            R.id.settings_button
    };
    private boolean prevEnabledInterviewCheck = false;

    private static final int RESULT_FROM_SETTINGS = 1;
    public static final String INTENT_REASON = "reason";
    private static final String PREF_KEY_PROMOTION_PAID_DATE = "promotion_paid_date";

    protected static final String PREFIX_PATH = "com.jobmineplus.mobile.";
    protected static final String PREFIX_ACTIVITY_PATH = "activities.jbmnpls.";

    private Builder alert;

    private InterstitialAdHelper interstitialHelper;
    private String nextPage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the tutorial and set the content of this activity
        new TutorialHelper(this, R.layout.home,
                R.layout.tutorial_home, R.string.pref_seen_home_tutorial);

        connectUI();
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);

        Intent passedIntent = getIntent();

        // See if it came with an error
        String errorMessage = passedIntent.getStringExtra(INTENT_REASON);
        if (errorMessage != null) {
            alert = new Builder(this);
            alert.setMessage(errorMessage);
            alert.setNeutralButton("Ok", null);
            alert.show();
        }

        if (passedIntent != null && passedIntent.hasExtra("username")) {
            String username = passedIntent.getStringExtra("username");
            String password = passedIntent.getStringExtra("password");
            if (isReallyOnline()) {
                new LoginTask().execute(username, password);
            } else {
                setOnlineMode(false);
            }
        }
        attachAds();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Show the promotion dialog to tell users to get the paid donation version
        long showDate = preferences.getLong(PREF_KEY_PROMOTION_PAID_DATE, 0);
        Calendar now = Calendar.getInstance();
        if (showDate == 0) {
            // Since they have not seen it, we will show it in the next 3 days
            now.add(Calendar.DATE, 3);
            Editor ed = preferences.edit();
            ed.putLong(PREF_KEY_PROMOTION_PAID_DATE, now.getTimeInMillis());
            ed.commit();
        } else if (showDate > 0 && showDate < now.getTimeInMillis()) {
            // Date has passed, now we can show the dialog and after this, never show it again
            Editor ed = preferences.edit();
            ed.putLong(PREF_KEY_PROMOTION_PAID_DATE, -1);
            ed.commit();

            // Build and show the dialog
            Builder b = new Builder(this);
            b.setTitle(R.string.promo_ad_dialog_title);
            b.setMessage(R.string.promo_ad_dialog_message);
            b.setNegativeButton(R.string.no_thank_you, null);
            b.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showJbmnplsmblProInMarket();
                }
            });
            b.show();
        }
    }

    private void attachAds() {
        interstitialHelper = new InterstitialAdHelper(this, 100);
        interstitialHelper.show();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    protected void connectUI() {
        for (int i = 0; i < buttonLayouts.length; i++) {
            findViewById(buttonLayouts[i]).setOnClickListener(this);
        }
    }

    public boolean goToActivityForResult(String activityName) {
        Class<?> name = null;
        try {
            name = Class.forName(PREFIX_PATH + PREFIX_ACTIVITY_PATH + activityName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        Intent in = new Intent(this, name);
        startActivityForResult(in, RESULT_FROM_SETTINGS);
        return true;
    }

    public boolean goToActivity(String activityName) {
        Class<?> name = null;
        try {
            name = Class.forName(PREFIX_PATH + PREFIX_ACTIVITY_PATH + activityName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        Intent in = new Intent(this, name);
        startActivity(in);
        return true;
    }

    @Override
    public void onClick(View arg0) {
        String name = ((TextView)arg0.findViewWithTag("text")).getText().toString();
        if (name.equals("Settings")) {
            prevEnabledInterviewCheck = preferences.getBoolean("settingsEnableInterCheck", false);
            goToActivityForResult(name);
        } else {
            nextPage = name.replace(" ", "");
            goToActivity(nextPage);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESULT_FROM_SETTINGS:
                // Check the differences coming back from settings
                if (preferences.getBoolean("settingsEnableInterCheck", false) != prevEnabledInterviewCheck) {
                    if (!prevEnabledInterviewCheck) {   // Enable it
                        startInterviewsAlarm();
                    } else {
                        cancelInterviewsAlarm();
                    }
                }
                break;
        }
    }

    protected final class LoginTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            // Do not allow this login to be aborted
            client.canAbort(false);
            client.login(params[0], params[1]);
            client.canAbort(true);
            return null;
        }

    }
}