package com.jobmineplus.mobile.debug;

import java.util.Date;

import android.app.Application;

public class DebugApplication extends Application{

    public DebugApplication() {
        offlineFlag = internalIsOnline();
    }

    final public int OFFLINE_TIME = 24;     //24 hour clock
    final public int ONLINE_TIME = 7;        //Opens at 6am

    protected boolean offlineFlag;

    private boolean internalIsOnline() {
        Date now = new Date();
        // hehe I am in Japan :P
        int hour = now.getHours();
        hour -= 14;
        now.setHours(hour);
        hour = now.getHours();
        int day = now.getDay();
        boolean isOnline = (day == 6 && hour >= ONLINE_TIME || day == 0) || (hour >= ONLINE_TIME && hour < OFFLINE_TIME);
        System.out.println("Is offline: " + (!isOnline));
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


