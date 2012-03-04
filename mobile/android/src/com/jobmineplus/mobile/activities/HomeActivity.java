package com.jobmineplus.mobile.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.services.JbmnplsHttpService;

public class HomeActivity extends Activity implements OnClickListener{
	protected JbmnplsHttpService service;
	protected Button appsButton;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        service = JbmnplsHttpService.getInstance();
        
        connectUI();
    }
	
	protected void connectUI() {
		appsButton = (Button) findViewById(R.id.apps_button);
		
		appsButton.setOnClickListener(this);
	}
	
	public boolean goToActivity(String activity) {
		Class name = null;
		try {
			name = Class.forName("com.jobmineplus.mobile.activities.jbmnpls." + activity);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		Intent in = new Intent(this, name);
		startActivity(in);
		return true;
	}

	@Override
	public void onClick(View arg0) {
		Button button = (Button) arg0;
		String name = button.getText().toString();
		goToActivity(name);
	}
}