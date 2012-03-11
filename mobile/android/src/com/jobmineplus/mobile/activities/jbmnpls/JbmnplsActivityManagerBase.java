package com.jobmineplus.mobile.activities.jbmnpls;

import android.app.LocalActivityManager;
import android.os.Bundle;

public abstract class JbmnplsActivityManagerBase extends JbmnplsActivityBase{

	//================
	//	Declarations
	//================
	private LocalActivityManager lam;
	private final String BUNDLE_STATE_KEY = "localactivitymanagerkeystate";
	
	//====================
	//	Override Methods
	//====================
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Bundle state = null;
		if (savedInstanceState != null) {
			state = savedInstanceState.getBundle(BUNDLE_STATE_KEY);
		}
		lam = new LocalActivityManager(this, false);
		lam.dispatchCreate(state);		
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBundle(BUNDLE_STATE_KEY, lam.saveInstanceState());
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		lam.dispatchResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		lam.dispatchPause(this.isFinishing());
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		lam.dispatchStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		lam.dispatchDestroy(this.isFinishing());
	}
	
	//=====================
	//	Protected Methods
	//=====================
	protected LocalActivityManager getLocalActivityManager() {
		return lam;
	}
}