package com.jobmineplus.mobile.activities;

import java.util.ArrayList;
import java.util.Date;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.widgets.Job;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnClickListener, TextWatcher{
	
	//UI objects
	protected Button loginBtn;
	EditText usernameEdtbl, passwordEdtbl;
	TextView output;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        defindUiAndAttachEvents();
    }
    
    private void defindUiAndAttachEvents() {
    	loginBtn = (Button) findViewById(R.id.login_button);
    	usernameEdtbl = (EditText) findViewById(R.id.username_field);
    	passwordEdtbl = (EditText) findViewById(R.id.password_field);
    	loginBtn.setOnClickListener(this);
//    	loginBtn.setEnabled(false);
    	
    	usernameEdtbl.addTextChangedListener(this);
    	passwordEdtbl.addTextChangedListener(this);
    	
    	//output = (TextView) findViewById(R.id.output);
    	//output.setMovementMethod(new ScrollingMovementMethod());
    }
    
    public void afterTextChanged(Editable arg0) {
    	Boolean enable = usernameEdtbl.getText().length() > 0 
    				  && passwordEdtbl.getText().length() > 0;
    	loginBtn.setEnabled(enable);
    }
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

    
    public void onClick(View v) {
    	String username = usernameEdtbl.getText().toString();
    	String password = passwordEdtbl.getText().toString();

    	//Hide virtual keyboard
    	InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE); 
    	if (inputManager != null) {
    		inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    	}
    	
    	new AsyncLoginTask(this).execute(username, password);
   // 	new AsyncLoginTask(this).execute(a, b);
    }
   
    public void print(Object text) {
    	System.out.println(text);
    }
    
    private class AsyncLoginTask extends AsyncTask<String, Void, Boolean> {
    	private Activity activity;
    	private ProgressDialog progress;
    	protected JbmnplsHttpService service;
    	
    	public AsyncLoginTask(Activity activity) {
    		this.activity = activity;
    		service = JbmnplsHttpService.getInstance();
    	}
    	
    	@Override
		protected void onPreExecute(){ 
    		super.onPreExecute();
    		progress = ProgressDialog.show(activity, "", 
					activity.getString(R.string.login_message), true);
		}
    	
		@Override
		protected Boolean doInBackground(String... args) {
			return service.login(args[0], args[1]);
		}

		@Override
		protected void onPostExecute(Boolean isLoggedin){
			super.onPostExecute(isLoggedin);
			if (progress.isShowing()) {
				progress.dismiss();
			}
			if (isLoggedin) {
				Toast.makeText(activity, "You are logged in!", Toast.LENGTH_SHORT).show();
				Intent myIntent = new Intent(activity, HomeActivity.class);
				startActivity(myIntent);
			} else {
				Toast.makeText(activity, activity.getString(R.string.login_fail_message), Toast.LENGTH_SHORT).show();
				Intent myIntent = new Intent(activity, HomeActivity.class);
				startActivity(myIntent);
			}
		}
    }
}