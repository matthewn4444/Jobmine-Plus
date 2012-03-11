package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TabHost;

import com.jobmineplus.mobile.exceptions.JbmnplsException;

public abstract class JbmnplsTabActivityBase extends JbmnplsActivityManagerBase 
						implements TabHost.TabContentFactory, TabHost.OnTabChangeListener{

	//=========================
	// 	TabInfo to store data
	//=========================
	protected class TabInfo {
		private String tag, displayName;
		private Class<?> classs;
		private Fragment fragment;
		private boolean isFragmentClass;
		
		public TabInfo(String tag, String displayName) {
			this.tag = tag;
			this.displayName = displayName;
		}
		public TabInfo(String tag, String displayName, Class<?> cls, boolean isFragmentClass) {
			this.tag = tag;
			this.displayName = displayName;
			this.classs = cls;
			this.isFragmentClass = isFragmentClass;
		}
		
		public String getTag() {
			return tag;
		}
		public String getName() {
			return displayName;
		}
		public Class<?> getClasss() {
			return classs;
		}
		public Fragment getFragment() {
			return fragment;
		}
		public boolean hasClass() {
			return classs != null;
		}
		public boolean isFragmentClass() {
			return isFragmentClass;
		}
	}
	
	//========================
	// 	Tab Private Variables
	//========================
	private TabHost tabHost;
	private Bundle instanceState;
	private TabInfo currentTabInfo;
	private Set<String> instantiateTabSet;
	
	protected Map<String, TabInfo> tabInfoMap;
	
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
	/**
	 * Anything that is overriding this function MUST run super
	 * or else tabs will NOT WORK.
	 * 
	 * @Override
	 * 	protected void defineUI(Bundle savedInstanceState) {
	 * 		super.defineUI();
	 * 		//Do some crazy stuff
	 *	}
	 */
	@Override
	protected void defineUI(Bundle savedInstanceState) {
		instanceState = savedInstanceState;
		setUpTabHost();
	}
	
	@Override
	public View createTabContent(String tag) throws NullPointerException{
		View v = null;
		try {
			v = onTabSwitched(tag);
		} catch (NullPointerException e) {
			e.printStackTrace();
			throw new NullPointerException("There was a null object in your tab creation, " +
					"probably a UI object has not been initalized, please wait for it.");
		}
		if (v == null) {
			v = new View(this);		//Shows nothing or activity takes hold;
		}
		return v; 
	}
	
	@Override
	public void onTabChanged(String tag) {
		TabInfo newTab = tabInfoMap.get(tag);
		if (newTab != currentTabInfo) {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			if (currentTabInfo != null && currentTabInfo.hasClass()) {
				if (currentTabInfo.fragment != null) {
					ft.detach(currentTabInfo.fragment);
				}
			}
			if (newTab.hasClass() && newTab.isFragmentClass && newTab != null) {
				if (newTab.fragment == null) {
					log(newTab.classs.getName());
					newTab.fragment = Fragment.instantiate(this, newTab.classs.getName(), instanceState);
					ft.add(android.R.id.tabcontent, newTab.fragment, tag);
				} else {
					ft.attach(newTab.fragment);
				}
			}
			
			ft.commit();
			currentTabInfo = newTab;
			getSupportFragmentManager().executePendingTransactions();
		}
		
		// The set allows us not to call the tab switch twice
		if (instantiateTabSet == null || !instantiateTabSet.contains(tag)) {
			onTabSwitched(tag);
		} else {
			instantiateTabSet.remove(tag);
			if (instantiateTabSet.isEmpty()) {
				instantiateTabSet = null;
			}
		}
	}

	//=================
	// 	Tab Creation
	//=================
	
	/**
	 * This creates a tab. Use this for View objects (compatibility).
	 * You must call this in defineUI() or else you will get an error!
	 * @param id
	 * @param displayName
	 */
	protected void createTab(String tag, String displayName) {
		TabInfo tab = new TabInfo(tag, displayName);
		setUpTab(tab);
	}
	
	/**
	 * This creates a tab. Use this for Activities (compatibility) or Fragments.
	 * You must call this in defineUI() or else you will get an error!
	 * @param tag
	 * @param displayName
	 * @param cls
	 * @param isFragmentClass is if the class name extends from a Fragment class (TRUE) or activity (FALSE)
	 */
	protected void createTab(String tag, String displayName, Class<?> cls, boolean isFragmentClass) {
		TabInfo tab = new TabInfo(tag, displayName, cls, isFragmentClass);
		setUpTab(tab);
	}
	
	protected void setUpTab(TabInfo tab) {
		if (instantiateTabSet == null) {
			instantiateTabSet = new HashSet<String>();
		}
		instantiateTabSet.add(tab.getTag());
		
		if (tab.classs != null) {
			tab.fragment = getSupportFragmentManager().findFragmentByTag(tab.tag);
			if (tab.fragment != null && !tab.fragment.isDetached()) {
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.detach(tab.fragment);
				ft.commit();
				getSupportFragmentManager().executePendingTransactions();
			}
		}
		tabInfoMap.put(tab.tag, tab);
		
		if (tab.hasClass() && !tab.isFragmentClass) {
			tabHost.addTab(tabHost.newTabSpec(tab.tag)
					.setIndicator(tab.displayName).setContent(new Intent(this, tab.classs)));
		} else {
			tabHost.addTab(tabHost.newTabSpec(tab.tag)
					.setIndicator(tab.displayName).setContent(this));
		}
	}
	
	/**
	 * Sets up the tabhost object
	 * Calls this by overriding the base class' defineUI()
	 * because it needs the layout to be initialized before 
	 * getting the tabhost.
	 * It will throw an error if no tabs are specified.
	 * @throws JbmnplsException
	 */
	private void setUpTabHost(){
		tabInfoMap = new HashMap<String, TabInfo>();
		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		if (tabHost == null) {
			throw new NullPointerException("The xml file does not have a tabhost object, please check it!");
		}
		tabHost.setup(getLocalActivityManager());
		tabHost.setOnTabChangedListener(this);
	}
}