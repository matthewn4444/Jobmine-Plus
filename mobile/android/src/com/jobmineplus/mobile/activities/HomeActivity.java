package com.jobmineplus.mobile.activities;

import android.app.Activity;
import android.os.Bundle;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.services.JbmnplsHttpService;

public class HomeActivity extends Activity {
	JbmnplsHttpService service;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        service = JbmnplsHttpService.getInstance();
    }
}