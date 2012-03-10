package com.jobmineplus.mobile.widgets;

import android.app.Activity;
import android.os.AsyncTask;

public abstract class JbmnplsAsyncTaskBase<TParams, TProgress, TResult> 
							extends AsyncTask<TParams, TProgress, TResult>{
	//===============
	//	Declaration
	//===============
	protected Activity activity;
	protected boolean isRunning = false;
	
	//===============
	//	Constructor
	//===============
	public JbmnplsAsyncTaskBase(Activity activity) {
		attach(activity);
	}
	
	//==================
	//	Public Methods
	//==================
	public void attach(Activity activity) {
		this.activity = activity;
		if (activity == null) {
			onActivityDetached();
		} else {
			onActivityAttached();
		}
	}
	
	public void detach() {
		this.activity = null;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	//=====================	
	//	Protected Methods
	//=====================
	protected void onActivityAttached() {}
	protected void onActivityDetached() {}
	
	//====================
	//	Override Methods
	//====================
	@Override
    protected void onPreExecute() {
		isRunning = true;
    }
 
    @Override
    protected void onPostExecute(TResult result) {
    	isRunning = false;
    }
 
    @Override
    protected void onCancelled() {
    	isRunning = false;
    }
}
