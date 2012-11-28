package com.jobmineplus.mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import com.jobmineplus.mobile.widgets.Alert;


public abstract class AlertActivity extends SimpleActivityBase{
    final protected String INTENT_EXTRA_REASON = "reason";

    private Alert alert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        alert = new Alert(this);
        super.onCreate(savedInstanceState);

        Intent passedIntent = getIntent();
        if (passedIntent != null && passedIntent.hasExtra(INTENT_EXTRA_REASON)) {
            String reason = passedIntent.getStringExtra(INTENT_EXTRA_REASON);
            showMessage(reason);
        }
    }

    protected Alert getAlert() {
        return alert;
    }

    protected boolean isShowingAlert() {
        return alert.isShowing();
    }

    protected void showMessage(String message)  {
        alert.show(message);
    }

    protected void hideMessage() {
        alert.cancel();
    }
}
