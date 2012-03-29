package com.jobmineplus.mobile;

import java.util.HashMap;
import java.util.Map;

import android.app.Application;

import com.jobmineplus.mobile.widgets.InterviewData;
import com.jobmineplus.mobile.widgets.Job;

public class JbmnplsApplication extends Application{
//    private Object lock = new Object();
    
    public JbmnplsApplication() {
        jobManagerInit();
        interviewManagerInit();
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
        if (hasJobId(id)) {
            Job oldJob = getJob(id);
            oldJob.merge(job);
        } else {
            jobMap.put(id, job);
        }
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
    
    //======================
    //  Interviews Manager
    //======================
    private Map<Integer, InterviewData> interviewsMap;
    
    private void interviewManagerInit() {
        interviewsMap = new HashMap<Integer, InterviewData>();
    }
    
    public synchronized void clearInterviewData() {
        interviewsMap.clear();
    }
    public synchronized void addInterview(InterviewData data) {
        int id = data.getJobId();
        if (hasInterview(id)) {
            InterviewData oldData = getInterviewData(id);
            oldData.merge(data);
        } else {
            interviewsMap.put(id, data);
        }
    }
    public synchronized InterviewData removeInterview(int id) {
        return interviewsMap.remove(id);
    }
    public synchronized InterviewData getInterviewData(int id) {
        return interviewsMap.get(id);
    }
    public synchronized boolean hasInterview(int id) {
        return interviewsMap.containsKey(id);
    }
    public synchronized boolean containsInterviewData(Job job) {
        return interviewsMap.containsValue(job);
    }
    
    
    
    
    
    
    
    
    
}
