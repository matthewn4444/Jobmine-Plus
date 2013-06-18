package com.jobmineplus.mobile.activities;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.database.users.UserDataSource;
import com.jobmineplus.mobile.widgets.JbmnplsAsyncTaskBase;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import com.jobmineplus.mobile.widgets.ProgressDialogAsyncTaskBase;
import com.jobmineplus.mobile.widgets.StopWatch;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient.LOGGED;

public class LoginActivity extends SimpleActivityBase implements OnClickListener, TextWatcher {
    public static final String DO_AUTO_LOGIN_EXTRA = "do_auto_login_extra";

    private UserDataSource userDataSource;
    private StopWatch sw;

    //UI objects
    protected Button loginBtn;
    protected Builder tou;
    protected TextView touText;
    EditText usernameEdtbl, passwordEdtbl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        userDataSource = new UserDataSource(this);
        userDataSource.open();
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setTitle("Please log in");

        // Check preferences and intent to see if we should autologin
        Intent intent = getIntent();
        boolean doAutoLogin = intent.getBooleanExtra(DO_AUTO_LOGIN_EXTRA, true);
        if (doAutoLogin && preferences.getBoolean("settingsAutoLogin", true)) {
            // Check for login credentials
            // If this fails on startup, make a launcher activity instead to read credentials on thread
            Pair<String, String> credentials = userDataSource.getLastUser();
            if (credentials != null) {
                goToHomeActivityAndLogin(credentials.first, credentials.second);
            } else {
                defindUiAndAttachEvents();
            }
        } else {
            defindUiAndAttachEvents();
        }
    }

    @Override
    protected void onResume() {
        userDataSource.open();
        super.onResume();
    }

    @Override
    protected void onPause() {
        userDataSource.close();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        userDataSource.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void defindUiAndAttachEvents() {
        tou = new Builder(this);
        touText = (TextView) findViewById(R.id.tou_text);
        loginBtn = (Button) findViewById(R.id.login_button);
        usernameEdtbl = (EditText) findViewById(R.id.username_field);
        passwordEdtbl = (EditText) findViewById(R.id.password_field);
        loginBtn.setOnClickListener(this);
        loginBtn.setEnabled(false);

        usernameEdtbl.addTextChangedListener(this);
        passwordEdtbl.addTextChangedListener(this);
        touText.setOnClickListener(this);

        tou.setNeutralButton("Ok", null);
        tou.setTitle(getString(R.string.login_tou_title));
        tou.setMessage(R.string.login_tou_message);
    }

    public void afterTextChanged(Editable arg0) {
        Boolean enable = usernameEdtbl.getText().length() > 0
                      && passwordEdtbl.getText().length() > 0;
        loginBtn.setEnabled(enable);
    }
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}


    public void onClick(View v) {
        if (loginBtn.equals(v)) {
            doLogin();
        } else if (touText.equals(v)) {
            tou.show();
        }
    }

    protected void doLogin() {
        String username = usernameEdtbl.getText().toString();
        String password = passwordEdtbl.getText().toString();

        // Hide virtual keyboard
        InputMethodManager inputManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null && this.getCurrentFocus() != null) {
            inputManager.hideSoftInputFromWindow(this.getCurrentFocus()
                    .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        doLogin(username, password);
    }

    protected void doLogin(String username, String password) {
        sw = new StopWatch(true);
        if (isReallyOnline()) {
            new AsyncLoginTask(this).execute(username, password);
        } else {
            setOnlineMode(false);
            new AsyncOfflineLoginTask(this).execute(username, password);
        }
    }

    protected void goToHomeActivity() {
        Intent myIntent = new Intent(this, HomeActivity.class);
        startActivity(myIntent);
        finish();
    }

    protected void goToHomeActivityAndLogin(String username, String password) {
        Intent in = new Intent(this, HomeActivity.class);
        in.putExtra("username", username);
        in.putExtra("password", password);
        startActivity(in);
        finish();
    }

    protected void postExecuteLogin(LOGGED loginState) {
        if (loginState == JbmnplsHttpClient.LOGGED.IN) {
            Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
            log("Login time:",sw.elapsed());
            goToHomeActivity();
        } else if (loginState == JbmnplsHttpClient.LOGGED.OUT) {
            String message = getString(isReallyOnline() ? R.string.login_fail_message : R.string.login_offline_message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else {    // LOGGED.OFFLINE
            Toast.makeText(this, getString(R.string.login_not_available),
                    Toast.LENGTH_SHORT).show();
        }
    }


    protected class AsyncOfflineLoginTask extends JbmnplsAsyncTaskBase<String, Void, JbmnplsHttpClient.LOGGED> {
        public AsyncOfflineLoginTask(Activity a) {
            super(a);
        }

        @Override
        protected LOGGED doInBackground(String... args) {
            String username = args[0];
            String password = args[1];
            boolean loggedIn = userDataSource.checkCredentials(username, password);
            if (loggedIn) {
                client.setLoginCredentials(username, password);
                userDataSource.putUser(username, password, true);
                return LOGGED.IN;
            }
            return LOGGED.OUT;
        }

        @Override
        protected void onPostExecute(JbmnplsHttpClient.LOGGED loginState){
            super.onPostExecute(loginState);
            ((LoginActivity)getActivity()).postExecuteLogin(loginState);
        }
    }

    protected class AsyncLoginTask extends ProgressDialogAsyncTaskBase<String, Void, JbmnplsHttpClient.LOGGED> {
        public AsyncLoginTask(Activity activity) {
            super(activity, activity.getString(R.string.login_message));
        }

        @Override
        protected JbmnplsHttpClient.LOGGED doInBackground(String... args) {
            JbmnplsHttpClient.LOGGED result = client.login(args[0], args[1]);
            if (result == LOGGED.IN) {
                userDataSource.putUser(args[0], args[1], true);
            }
            return result;
        }

        @Override
        protected void onPostExecute(JbmnplsHttpClient.LOGGED loginState){
            super.onPostExecute(loginState);
            ((LoginActivity)getActivity()).postExecuteLogin(loginState);
        }
    }

}