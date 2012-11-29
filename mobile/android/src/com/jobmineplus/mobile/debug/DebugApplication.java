package com.jobmineplus.mobile.debug;

import java.util.Date;

import android.app.Application;

public class DebugApplication extends Application{

    final public int OFFLINE_TIME = 24;     //24 hour clock
    final public int ONLINE_TIME = 7;        //Opens at 6am

    public boolean isOffline () {
        Date now = new Date();
        // hehe I am in Japan :P
        int hour = now.getHours();
        hour -= 14;
        now.setHours(hour);
        hour = now.getHours();
        int day = now.getDay();
        boolean isOnline = (day == 6 && hour >= ONLINE_TIME || day == 0) || (hour >= ONLINE_TIME && hour < OFFLINE_TIME);
        System.out.println("Is offline: " + (!isOnline));
        return !isOnline;
    }
}


