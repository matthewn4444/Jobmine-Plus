package com.jobmineplus.mobile.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.database.users.UserDataSource;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import com.jobmineplus.mobile.widgets.ProgressDialogAsyncTaskBase;
import com.jobmineplus.mobile.widgets.StopWatch;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient.LOGGED;

public class LoginActivity extends AlertActivity implements OnClickListener, TextWatcher {
    private UserDataSource userDataSource;
    private StopWatch sw;

    //UI objects
    protected Button loginBtn;
    EditText usernameEdtbl, passwordEdtbl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        userDataSource = new UserDataSource(this);
        userDataSource.open();

        // Check for login credentials
        // If this fails on startup, make a launcher activity instead to read credentials on thread
        Pair<String, String> credentials = userDataSource.getLastUser();
        if (credentials != null) {
            if (isOnline()) {
                new LoginTask().execute(credentials.first, credentials.second);
            } else {
                client.setLoginCredentials(credentials.first, credentials.second);
            }
            goToHomeActivity();
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

    private void defindUiAndAttachEvents() {
        loginBtn = (Button) findViewById(R.id.login_button);
        usernameEdtbl = (EditText) findViewById(R.id.username_field);
        passwordEdtbl = (EditText) findViewById(R.id.password_field);
        loginBtn.setOnClickListener(this);
        loginBtn.setEnabled(false);

        usernameEdtbl.addTextChangedListener(this);
        passwordEdtbl.addTextChangedListener(this);
    }

    public void afterTextChanged(Editable arg0) {
        Boolean enable = usernameEdtbl.getText().length() > 0
                      && passwordEdtbl.getText().length() > 0;
        loginBtn.setEnabled(enable);
    }
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}


    public void onClick(View v) {
        doLogin();
    }

    public void log(Object text) {
        System.out.println(text);
    }

    protected void doLogin() {
        String username = usernameEdtbl.getText().toString();
        String password = passwordEdtbl.getText().toString();

        // Hide virtual keyboard
        InputMethodManager inputManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null) {
            inputManager.hideSoftInputFromWindow(this.getCurrentFocus()
                    .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        doLogin(username, password);
    }

    protected void doLogin(String username, String password) {
        sw = new StopWatch(true);
        if (isOnline()) {
            new AsyncLoginTask(this).execute(username, password);
        } else {
            log("offline login");
            boolean loggedIn = userDataSource.checkCredentials(username, password);
            if (loggedIn) {
                client.setLoginCredentials(username, password);
            }
            postExecuteLogin(loggedIn ? LOGGED.IN : LOGGED.OUT);
        }
    }

    protected void goToHomeActivity() {
        Intent myIntent = new Intent(this, HomeActivity.class);
        startActivity(myIntent);
        finish();
    }

    protected void postExecuteLogin(LOGGED loginState) {
        if (loginState == JbmnplsHttpClient.LOGGED.IN) {
            Toast.makeText(this, "You are logged in! " + sw.elapsed() + " ms",
                    Toast.LENGTH_SHORT).show();
            goToHomeActivity();
        } else if (loginState == JbmnplsHttpClient.LOGGED.OUT) {
            Toast.makeText(this, getString(R.string.login_fail_message),
                    Toast.LENGTH_SHORT).show();
        } else {    // LOGGED.OFFLINE
            Toast.makeText(this, getString(R.string.login_not_available),
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected final class LoginTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            // Do not allow this login to be aborted
            client.canAbort(false);
            JbmnplsHttpClient.LOGGED result = client.login(params[0], params[1]);
            client.canAbort(true);
            if (result != LOGGED.IN) {
                throw new IllegalStateException("Prior logins credentials do not work or isOnline() does not work");
            }
            return null;
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