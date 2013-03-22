package com.jobmineplus.mobile.debug;

import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.jobmineplus.mobile.activities.HomeActivity;

public class DebugHomeActivity extends HomeActivity implements Debuggable{
    Application app;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        app = getApplication();
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View arg0) {
        String name = ((Button) arg0).getText().toString();
        if (name.equals("Settings")) {
            super.onClick(arg0);
        } else {
            goToActivity(".debug.activities.Debug" + name);
        }
    }

    // =================
    //  Menu buttons
    // =================
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater mInflater = getMenuInflater();
//        mInflater.inflate(R.menu.debug_main_menu, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        MenuItem item = menu.findItem(R.id.menuitem_real_site);
//        if (app.isOnline()) {
//            item.setTitle("Go Fake Site");
//        } else {
//            item.setTitle("Go Real Site");
//        }
//        return super.onPrepareOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle item selection
//        switch (item.getItemId()) {
//            case R.id.menuitem_online_mode:
//                setOnlineMode(!isOnline());
//                return true;
//            case R.id.menuitem_real_site:
//                app.toggleOnline();
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }
}
