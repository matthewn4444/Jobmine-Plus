package com.jobmineplus.mobile.activities;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.jobmineplus.mobile.R;

public class HomeActivity extends LoggedInActivityBase implements OnClickListener{
    protected int[] buttonLayouts = {
            R.id.apps_button,
            R.id.shortlist_button,
            R.id.interviews_button,
            R.id.settings_button
    };
    private boolean prevEnabledInterviewCheck = false;

    private static final String PREFIX_PATH = "com.jobmineplus.mobile";
    private static final int RESULT_FROM_SETTINGS = 1;
    public static final String INTENT_REASON = "reason";
    public static final String PREF_SEEN_TUTORIAL = "settingsSeenTutorial";

    private Builder alert;
    private View tutorialView;
    private FrameLayout mainLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the home layout, if needed, inflate the tutorial layout
        mainLayout = new FrameLayout(this);
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflator.inflate(R.layout.home, mainLayout);

        // See if we should show the tutorial
        if (!preferences.getBoolean(PREF_SEEN_TUTORIAL, false)) {
            View view = inflator.inflate(R.layout.tutorial, mainLayout);
            tutorialView = view.findViewById(R.id.layout);
            tutorialView.setOnClickListener(this);
        }
        setContentView(mainLayout);
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
            name = Class.forName(PREFIX_PATH + activityName);
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
            name = Class.forName(PREFIX_PATH + activityName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        Intent in = new Intent(this, name);
        startActivity(in);
        return true;
    }

    public void onClick(View arg0) {
        if (arg0.equals(tutorialView)) {
            // Tutorial is finished
            mainLayout.removeView(tutorialView);
            Editor editor = preferences.edit();
            editor.putBoolean(PREF_SEEN_TUTORIAL, true);
            editor.commit();
        } else {
            String name = ((TextView)arg0.findViewWithTag("text")).getText().toString();
            if (name.equals("Settings")) {
                prevEnabledInterviewCheck = preferences.getBoolean("settingsEnableInterCheck", false);
                goToActivityForResult(".activities.jbmnpls." + name);
            } else {
                goToActivity(".activities.jbmnpls." + name);
            }
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