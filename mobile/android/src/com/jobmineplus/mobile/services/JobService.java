package com.jobmineplus.mobile.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import com.jobmineplus.mobile.widgets.Job;

public class JobService {
	private static HashMap<Integer, Job> jobList;
	private static JobService instance;
	private static Object lock = new Object();
	
	private JobService(){}
	
	public static JobService getInstance() {
		if (instance == null) {
			synchronized (lock) {
				if (instance == null) {
					jobList = new HashMap<Integer, Job>();
					instance = new JobService();
				}
			}
		}
		return instance;
	}
	
	//====================
	// 	Addition Methods
	//====================
	
	public synchronized void addJob(Job job) {
		int id = job.getId();
		jobList.put(id, job);
	}
	 
	public synchronized void addJobs(Job[] jobs) {
		for (Job job: jobs) {
			addJob(job);
		}
	}
	
	public synchronized void addJobs(ArrayList<Job> jobs) {
		ListIterator<Job> iterator = jobs.listIterator();
		while (iterator.hasNext()) {
			addJob(iterator.next());
		}
	}
	
	//===================
	// 	Getter Methods
	//===================
	
	public synchronized Job getJobById(int id) {
		return jobList.get(id);
	}
	
	public synchronized Job[] getJobsByIds(int[] ids) {
		Job[] jobs = new Job[ids.length];
		int counter = 0;
		for (int id: ids) {
			jobs[counter++] = getJobById(id);
		}
		return jobs;
	}
	
	public synchronized ArrayList<Job> getJobListByIds(int[] ids) {
		ArrayList<Job> jobs = new ArrayList<Job>(ids.length);
		for (int id: ids) {
			jobs.add(getJobById(id));
		}
		return jobs;
	}
	
	//===================
	// 	Removal Methods
	//===================
	
	public synchronized Job removeJob(int id) {
		return jobList.remove(id);
	}
	
	public synchronized Job[] removeJobs(int[] ids) {
		Job[] jobs = new Job[ids.length];
		int counter = 0;
		Job job;
		for (int id: ids) {
			job = removeJob(id);
			jobs[counter++] = job; 
		}
		return jobs;
	}
	
	public synchronized Job[] removeJobs(ArrayList<Integer> ids) {
		Job[] jobs = new Job[ids.size()];
		int counter = 0;
		Job job;
		for (int id: ids) {
			job = removeJob(id);
			jobs[counter++] = job; 
		}
		return jobs;
	}
	
	public synchronized boolean containsKey(int id) {
		return jobList.containsKey(id);
	}
	
	public synchronized boolean containsValue(Job job) {
		return jobList.containsValue(job);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
}
