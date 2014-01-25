package com.jobmineplus.mobile.activities;

import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.jobmineplus.mobile.R;
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

    protected static final String PREFIX_PATH = "com.jobmineplus.mobile.";
    protected static final String PREFIX_ACTIVITY_PATH = "activities.jbmnpls.";

    private Builder alert;

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
    }

    @Override
    public void onBackPressed() {
//        AppBrain.getAds().showInterstitial(this);
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
            goToActivity(name.replace(" ", ""));
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