package com.jobmineplus.mobile.debug;

import com.jobmineplus.mobile.activities.SimpleActivityBase;

import android.app.Application;

public class DebugApplication extends Application{

    public DebugApplication() {
        offlineFlag = internalIsOnline();
    }

    protected boolean offlineFlag;

    private boolean internalIsOnline() {
        boolean isOnline = SimpleActivityBase.isJobmineOnline();
        return isOnline;
    }

    public void toggleOnline() {
        offlineFlag = !offlineFlag;
    }

    public boolean isOnline () {
        return offlineFlag;
    }

    public void setOnline(boolean flag) {
        offlineFlag = flag;
    }
}


