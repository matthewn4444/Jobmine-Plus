package com.jobmineplus.mobile.debug;

import java.util.Date;

import com.jobmineplus.mobile.JbmnplsApplication;

public class DebugApplication extends JbmnplsApplication{
    
    final public int OFFLINE_TIME = 21;     //24 hour clock
    
    public boolean isOffline () {
        return new Date().getHours() >= OFFLINE_TIME;
    }
    
    
}
