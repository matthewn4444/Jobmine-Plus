package com.jobmineplus.mobile.debug;

import java.util.Date;

import com.jobmineplus.mobile.JbmnplsApplication;

public class DebugApplication extends JbmnplsApplication{
    
    final public int OFFLINE_TIME = 21;     //24 hour clock
    final public int ONLINE_TIME = 6;        //Opens at 6am
    
    public boolean isOffline () {
        Date now = new Date();
        // hehe I am in Japan :P
        int hour = now.getHours();
        hour -= 13;
        now.setHours(hour);
        hour = now.getHours();
        System.out.println("Is offline: " + (now.getDay() != 6 && now.getDay() != 0 && (hour >= OFFLINE_TIME || hour <= ONLINE_TIME)));
        return now.getDay() != 6 && now.getDay() != 0 && (hour >= OFFLINE_TIME || hour <= ONLINE_TIME);
    }
}
