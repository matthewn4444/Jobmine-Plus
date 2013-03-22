package com.jobmineplus.mobile.activities.jbmnpls;

import java.util.ArrayList;
import java.util.HashMap;

import com.jobmineplus.mobile.R;
import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TitlePageIndicator;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public abstract class JbmnplsPageActivityBase extends JbmnplsActivityBase {
    private TabAdapter mAdapter;
    private ViewPager mPager;
    private PageIndicator mIndicator;

    @Override
    protected void defineUI(Bundle savedInstanceState) {
        setContentView(R.layout.tabs);
        mAdapter = new TabAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mIndicator = (TitlePageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
    }

    public void createTab(String title, Fragment fragment) {
        mAdapter.createTab(title, fragment);
    }

    protected final class TabAdapter extends FragmentPagerAdapter {
        public TabAdapter(FragmentManager fm) {
            super(fm);
        }

        private final HashMap<String, Fragment> tabFragments = new HashMap<String, Fragment>();
        private final ArrayList<String> tabTitles = new ArrayList<String>();

        public void createTab(String title, Fragment fragment) {
            tabFragments.put(title, fragment);
            tabTitles.add(title);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return tabTitles.size();
        }

        @Override
        public int getItemPosition(Object object) {
            return FragmentPagerAdapter.POSITION_NONE;
        }

        public Fragment getItem(String title) {
            return tabFragments.get(title);
        }

        @Override
        public Fragment getItem(int position) {
            String key = (String) getPageTitle(position);
            return getItem(key);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles.get(position);
        }
    }
}
