package com.jobmineplus.mobile;

import java.util.HashMap;
import java.util.Map;

import android.app.Application;

import com.jobmineplus.mobile.widgets.Job;

public class JbmnplsApplication extends Application{
    private Object lock = new Object();
    
    public JbmnplsApplication() {
        jobManagerInit();
    }
    
    //================
    //  Job Manager
    //================
    private Map<Integer, Job> jobMap;
    
    private void jobManagerInit() {
        jobMap = new HashMap<Integer, Job>();
    }
    
    public synchronized void clearJobs() {
        jobMap.clear();
    }
    
    public synchronized void addJob(Job job) {
        int id = job.getId();
        jobMap.put(id, job);
    }
    
    public synchronized Job removeJob(int id) {
        return jobMap.remove(id);
    }
    
    public synchronized Job getJob(int id) {
        return jobMap.get(id);
    }
    
    public synchronized boolean hasJobId(int id) {
        return jobMap.containsKey(id);
    }
    
    public synchronized boolean containsJob(Job job) {
        return jobMap.containsValue(job);
    }
    
    
    
    
    
    
    
    
    
    
    
    
}
