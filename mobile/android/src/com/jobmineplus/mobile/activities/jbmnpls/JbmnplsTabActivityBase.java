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
    //     TabInfo to store data
    //=========================
    protected class TabInfo {
        private String tag, displayName;
        private Class<?> classs;
        private Fragment fragment;
        private View view;
        private boolean isFragmentClass;
        private Bundle extras;

        public TabInfo(String tag, String displayName, View view) {
            this.tag = tag;
            this.displayName = displayName;
            this.view = view;
        }
        public TabInfo(String tag, String displayName, Class<?> cls, boolean isFragmentClass, Bundle extras) {
            this.tag = tag;
            this.displayName = displayName;
            this.classs = cls;
            this.isFragmentClass = isFragmentClass;
            this.extras = extras;
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
        public View getView() {
            return view;
        }
        public boolean hasClass() {
            return classs != null;
        }
        public boolean isFragmentClass() {
            return isFragmentClass;
        }
    }

    //========================
    //     Tab Private Variables
    //========================
    private TabHost tabHost;
    private TabInfo currentTabInfo;
    private Set<String> instantiateTabSet;

    protected Map<String, TabInfo> tabInfoMap;

    private View EMPTY_VIEW;

    //===================
    //     Override Method
    //===================
    /**
     * Anything that is overriding this function MUST run super
     * or else tabs will NOT WORK.
     *
     * @Override
     *     protected void defineUI(Bundle savedInstanceState) {
     *         super.defineUI();
     *         //Do some crazy stuff
     *    }
     */
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        EMPTY_VIEW = new View(this);
        setUpTabHost();
    }

    public View createTabContent(String tag) {
        onTabSwitched(tag);
        TabInfo tab = tabInfoMap.get(tag);
        View v = tab.view;
        if (v != null) {
            return v;
        } else {
            return EMPTY_VIEW;
        }
    }

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
                    newTab.fragment = Fragment.instantiate(this, newTab.classs.getName(), newTab.extras);
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
    //  Tab Creation
    //=================

    /**
     * This creates a tab. Use this for View objects (compatibility).
     * You must call this in defineUI() or else you will get an error!
     * @param id
     * @param displayName
     */
    protected void createTab(String tag, String displayName, View view) {
        TabInfo tab = new TabInfo(tag, displayName, view);
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
    protected void createTab(String tag, String displayName, Class<?> cls, boolean isFragmentClass, Bundle extras) {
        TabInfo tab = new TabInfo(tag, displayName, cls, isFragmentClass, extras);
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
            Intent i = new Intent(this, tab.classs);
            Bundle extras = tab.extras;
            if (extras != null) {
                i.putExtras(extras);
            }
            tabHost.addTab(tabHost.newTabSpec(tab.tag)
                    .setIndicator(tab.displayName).setContent(i));
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

    //=====================
    //    Protected Methods
    //=====================
    /**
     * Very similar to onTabChanged and createTab, just use this
     * to activate the change when you click tabs
     * @param tag
     * @return
     */
    public void onTabSwitched(String tag){}
}