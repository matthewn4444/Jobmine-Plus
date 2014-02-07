package com.jobmineplus.mobile.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.jobmineplus.mobile.R;

public class TutorialHelper implements OnClickListener{
    private final String prefKey;
    private ViewGroup tutorialView;
    private final FrameLayout frameLayout;
    private final SharedPreferences pref;
    private TextView clickToCloseText;

    public TutorialHelper(Activity activity, int activityLayoutResId, int tutorialLayoutResId, int preferenceResId) {
        this(activity, activityLayoutResId, tutorialLayoutResId, activity.getString(preferenceResId));
    }

    public TutorialHelper(Activity activity, int layoutResId, int tutorialLayoutResId, String preferenceKey) {
        prefKey = preferenceKey;

        frameLayout = new FrameLayout(activity);
        LayoutInflater inflator = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflator.inflate(layoutResId, frameLayout);
        activity.setContentView(frameLayout);

        // If first time, then build the tutorial and show it
        pref = PreferenceManager.getDefaultSharedPreferences(activity);
        if (!pref.getBoolean(preferenceKey, false)) {
            int numChildren = frameLayout.getChildCount();
            inflator.inflate(tutorialLayoutResId, frameLayout);

            try {
                tutorialView = (ViewGroup)frameLayout.getChildAt(numChildren);

                // Attach the event and background
                tutorialView.setOnClickListener(this);
                tutorialView.setBackgroundColor(Color.argb(99, 0, 0, 0));
                tutorialView.setClickable(true);

                // Attach the touch to close text
                clickToCloseText = new TextView(activity);
                clickToCloseText.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                        LayoutParams.FILL_PARENT));
                clickToCloseText.setGravity(Gravity.CENTER);
                clickToCloseText.setTextColor(Color.WHITE);
                clickToCloseText.setText(R.string.tutorial_end);
                frameLayout.addView(clickToCloseText);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public View getContentView() {
        return frameLayout;
    }

    @Override
    public void onClick(View v) {
        // Once clicked the tutorial, we do not need to see it again
        if (v.equals(tutorialView)) {
            // Tutorial is finished
            frameLayout.removeView(clickToCloseText);
            frameLayout.removeView(tutorialView);
            Editor editor = pref.edit();
            editor.putBoolean(prefKey, true);
            editor.commit();
        }
    }
}
