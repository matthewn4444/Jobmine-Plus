package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.JbmnplsException;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.services.JobService;

import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public abstract class JbmnplsTabActivityBase extends JbmnplsActivityBase 
						implements TabHost.TabContentFactory, TabHost.OnTabChangeListener{
	
	//========================
	// 	Tab Private Variables
	//========================
	private TabHost tabHost;
	protected ArrayList<NameValuePair> tabInfo = new ArrayList<NameValuePair>();
	
	//====================
	// 	Abstract Methods
	//====================
	/**
	 * Very similar to onTabChanged and createTab, just use this
	 * to activate the change when you click tabs
	 * @param tag
	 * @return
	 */
	public abstract View onTabSwitched(String tag);
	
	//===================
	// 	Override Method
	//===================
	
	@Override
	/**
	 * Anything that is overriding this function MUST run super
	 * or else tabs will NOT WORK.
	 * 
	 * @Override
	 * 	protected void defineUI() {
	 * 		//Do some crazy stuff
	 *		super.defineUI();
	 *	}
	 */
	protected void defineUI() {
		setUpTabs();
	}
	
	@Override
	public View createTabContent(String tag) throws NullPointerException{
		View v = onTabSwitched(tag);
		if (v == null) {
			throw new NullPointerException("Cannot create new tabs when you return a null view object.");
		}
		return v; 
	}

	@Override
	public void onTabChanged(String tag) {
		onTabSwitched(tag);
	}
	
	//=================
	// 	Tab Creation
	//=================
	
	/**
	 * This creates a tab.
	 * You can only call this in setUp(); Calling elsewhere will
	 * return an error so you cannot make new tabs after instantiated.
	 * @param id
	 * @param displayName
	 */
	protected void createTab(String id, String displayName) {
		tabInfo.add(new BasicNameValuePair(id, displayName));
	}
	
	/**
	 * Sets up the tabhost object and initializes the tabs.
	 * Calls this by overriding the base class' defineUI()
	 * because it needs the layout to be initialized before 
	 * getting the tabhost.
	 * It will throw an error if no tabs are specified.
	 * @throws JbmnplsException
	 */
	protected void setUpTabs(){
		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		if (tabHost == null) {
			throw new NullPointerException("The xml file does not have a tabhost object, please check it!");
		}
		tabHost.setup();
		tabHost.setOnTabChangedListener(this);
		
		for (NameValuePair tab: tabInfo) {
			tabHost.addTab(tabHost.newTabSpec(tab.getName())
					.setIndicator(tab.getValue()).setContent(this));
		}
	}
}
