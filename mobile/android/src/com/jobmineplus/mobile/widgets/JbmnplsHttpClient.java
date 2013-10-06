
package com.jobmineplus.mobile.widgets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.util.Log;

import com.bugsense.trace.BugSenseHandler;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;

public final class JbmnplsHttpClient {
    //================
    //  Static Links
    //================

    static public final class GET_LINKS {
        public static final String DOCUMENTS    = "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_STUDDOCS";
        public static final String PROFILE      = "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_STUDENT";
        public static final String SKILLS       = "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_STUDENT?PAGE=UW_CO_STU_SKL_MTN";
        public static final String SEARCH       = "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_JOBSRCH";
        public static final String SHORTLIST    = "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_JOB_SLIST";
        public static final String APPLICATIONS = "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_APP_SUMMARY";
        public static final String INTERVIEWS   = "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_STU_INTVS";
        public static final String RANKINGS     = "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_RNK2";
        public static final String DESCRIP_PRE  = "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_JOBDTLS?UW_CO_JOB_ID=";
    }

    static public final class POST_LINKS {
        public static final String LOGIN    = "https://jobmine.ccol.uwaterloo.ca/psp/SS/?cmd=login&languageCd=ENG&sessionId=";
        public static final String LOGOUT   = "https://jobmine.ccol.uwaterloo.ca/psp/SS/?cmd=login&languageCd=ENG&";
    }

    //===========================
    //  Logged in or out states
    //===========================
    static public enum LOGGED { IN, OUT, OFFLINE }


    //=============
    //  Constants
    //=============
    private static final int AUTO_LOGOUT_TIME           = 1000 * 60 * 20;   //20 min
    private static final int BUFFER_READER_SIZE         = 1024;

    // Login constants
    private static final String LOGIN_UNIQUE_STRING     = "Signin HTML for JobMine.";
    private static final String LOGIN_OFFLINE_MESSAGE  = "Invalid signon time for user";
    private static final String DEFAULT_HTML_ENCODER    = "UTF-8";
    private static final String FAILED_URL              = "Invalid URL - no Node found in";
    private static final int    LOGIN_READ_LENGTH       = 400;
    private static final int    LOGIN_ERROR_MSG_SKIP    = 3200;
    private static final int    MAX_LOGIN_ATTEMPTS = 3;


    //=====================
    //  Private Variables
    //=====================
    private Object lock = new Object();
    HttpClient client = new DefaultHttpClient();
    private long loginTimeStamp = 0;
    private String username = "";
    private String password = "";
    private HttpRequestBase currentRequest;
    private boolean canAbort = true;
    private boolean pendingAbort = false;

    //=========================
    //  Constructor
    //=========================
    public JbmnplsHttpClient() {
        reset();
    }

    public JbmnplsHttpClient(String user, String pass) {
        username = user;
        password = pass;
        reset();
    }

    //==============
    //  Login Data
    //==============
    public void setLoginCredentials(String user, String pass) {
        synchronized(lock) {
            username = user;
            password = pass;
        }
    }

    public String getUsername() {
        synchronized(lock) {
            if (username == "") {
                return null;
            }
            return username;
        }
    }

    public String getPassword() {
        synchronized(lock) {
            if (password == "") {
                return null;
            }
            return password;
        }
    }

    public boolean isLoggedIn() {
        synchronized(lock) {
            long timeNow = System.currentTimeMillis();
            return loginTimeStamp != 0 && (timeNow - loginTimeStamp) < AUTO_LOGOUT_TIME;
        }
    }

    public LOGGED login() {
        return login(username, password);
    }

    public boolean verifyLogin() {
        if (!isLoggedIn()) {
            for (int i = 0; i < MAX_LOGIN_ATTEMPTS; i++) {
                JbmnplsHttpClient.LOGGED result = login();
                if (result == JbmnplsHttpClient.LOGGED.IN) {
                    return true;
                } else if (result == JbmnplsHttpClient.LOGGED.OFFLINE) {
                    return false;
                }
            }
            return false;
        }
        return true;
    }

    public LOGGED login(String username, String password) {
        synchronized(lock) {
            reset();
            if (username.length() == 0 || password.length() == 0) {
                Log.i("jbmnplsmbl", "Logged out no pass and user");
                return LOGGED.OUT;
            }

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("httpPort", ""));
            nameValuePairs.add(new BasicNameValuePair("submit", "Submit"));
            nameValuePairs.add(new BasicNameValuePair("timezoneOffset", "480"));
            nameValuePairs.add(new BasicNameValuePair("pwd", password));
            nameValuePairs.add(new BasicNameValuePair("userid", username));

            BufferedReader reader = null;
            try {
                StopWatch s = new StopWatch(true);
                HttpResponse response = internalPost(nameValuePairs, JbmnplsHttpClient.POST_LINKS.LOGIN);
                s.printElapsed("%s ms login post");
                if (response == null || response.getStatusLine().getStatusCode() != 200) {
                    return LOGGED.OUT;
                }

                reader = getReaderFromResponse(response);
                LOGGED result = validateLoginJobmine(reader);
                if (result != LOGGED.IN) {
                    return result;
                }

                // Successful login
                setLoginCredentials(username, password);
                updateTimestamp();
                return LOGGED.IN;
            } catch (IOException e) {
                BugSenseHandler.sendException(e);
                e.printStackTrace();
                return LOGGED.OFFLINE;
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                    return LOGGED.OUT;
                } finally {
                    if (canAbort && pendingAbort) {
                        pendingAbort = false;
                    }
                }
            }
        }
    }

    public void logout() {
        synchronized (lock) {
            client = new DefaultHttpClient();
            loginTimeStamp = 0;
        }
    }

    //=====================
    //  GET HTTP Requests
    //=====================
    public HttpResponse get(String url) {
        HttpResponse result = internalGet(url);
        if (canAbort && pendingAbort) {
            pendingAbort = false;
        }
        return result;
    }

    private HttpResponse internalGet(String url) {
        if (pendingAbort && canAbort) {
            return null;
        }
        synchronized(lock) {
            HttpResponse response = null;
            try {
                if (pendingAbort && canAbort) {
                    return null;
                }
                currentRequest = new HttpGet(url);
                StopWatch s = new StopWatch(true);
                response = client.execute(currentRequest);
                s.printElapsed("%s ms to get");
                currentRequest = null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return response;
        }
    }

    public String getJobmineHtml (String url) throws JbmnplsLoggedOutException, IOException{
        synchronized(lock) {
            InputStream in = null;
            BufferedReader reader = null;
            try {
                // Attempt 3 times if logged out
                boolean loggedIn = false;
                for (int i = 0; i < MAX_LOGIN_ATTEMPTS; i++) {
                    if (in != null) {
                        in.close();
                        in = null;
                    }

                    HttpResponse response = internalGet(url);
                    if (response != null) {
                        in = response.getEntity().getContent();
                        reader = new BufferedReader(new InputStreamReader(in,
                                DEFAULT_HTML_ENCODER), BUFFER_READER_SIZE);

                        // Validates the html to make sure we logged in
                        // If failed to login, try it again 2 more times
                        LOGGED result = validateLoginJobmine(reader);
                        if (result == LOGGED.IN) {
                            loggedIn = true;
                            break;
                        } else if (result == LOGGED.OFFLINE) {
                            throw new JbmnplsLoggedOutException();
                        }
                    } else {
                        return null;
                    }
                    if (login() == LOGGED.OFFLINE) {
                        throw new JbmnplsLoggedOutException();
                    }
                }
                if (!loggedIn) {
                    throw new JbmnplsLoggedOutException();
                }

                // Successfully logged in
                StringBuilder str = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    str.append(line);
                }
                updateTimestamp();
                return str.toString();
            } catch (IOException e) {
                throw e;
            } finally {
                if (canAbort && pendingAbort) {
                    pendingAbort = false;
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch(IOException e) {}
                }
            }
        }
    }

    //======================
    //  POST HTTP Requests
    //======================
    public HttpResponse post(List<NameValuePair> postData, String url) {
        HttpResponse result = internalPost(postData, url);
        if (canAbort && pendingAbort) {
            pendingAbort = false;
        }
        return result;
    }

    public HttpResponse internalPost(List<NameValuePair> postData, String url) {
        if (canAbort && pendingAbort) {
            return null;
        }
        synchronized(lock) {
            if (canAbort && pendingAbort) {
                return null;
            }
            HttpResponse response = null;
            try {
                currentRequest = new HttpPost(url);
                ((HttpPost)currentRequest).setEntity(new UrlEncodedFormEntity(postData));
                response = client.execute(currentRequest);
                currentRequest = null;
                return response;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public String postJobmineHtml (List<NameValuePair> postData, String url) throws JbmnplsLoggedOutException, IOException {
        synchronized(lock) {
            InputStream in = null;
            BufferedReader reader = null;
            try {
                // Attempt 3 times if logged out
                boolean loggedIn = false;
                for (int i = 0; i < MAX_LOGIN_ATTEMPTS; i++) {
                    if (in != null) {
                        in.close();
                        in = null;
                    }

                    HttpResponse response = internalPost(postData, url);
                    if (response != null) {
                        in = response.getEntity().getContent();
                        reader = new BufferedReader(new InputStreamReader(in,
                                DEFAULT_HTML_ENCODER), BUFFER_READER_SIZE);
                        reader.mark(1);

                        // Validates the html to make sure we logged in
                        // If failed to login, try it again 2 more times
                        LOGGED result = validateLoginJobmine(reader);
                        if (result == LOGGED.IN) {
                            loggedIn = true;
                            break;
                        } else if (result == LOGGED.OFFLINE) {
                            throw new JbmnplsLoggedOutException();
                        }
                    } else {
                        return null;
                    }
                    if (login() == LOGGED.OFFLINE) {
                        throw new JbmnplsLoggedOutException();
                    }
                }
                if (!loggedIn) {
                    throw new JbmnplsLoggedOutException();
                }

                // Successfully logged in
                StringBuilder str = new StringBuilder();
                String line = null;
                reader.reset();
                while ((line = reader.readLine()) != null) {
                    str.append(line);
                }
                updateTimestamp();
                return str.toString();
            } catch (IOException e) {
                throw e;
            } finally {
                if (canAbort && pendingAbort) {
                    pendingAbort = false;
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch(IOException e) {}
                }
            }
        }
    }

    //========================
    //  Abort/Cancel Methods
    //========================
    public void abort() {
        pendingAbort = true;
        if (currentRequest != null && canAbort) {
            currentRequest.abort();     // ignore the warning.
            currentRequest = null;
        }
    }

    public void canAbort(boolean flag) {
        canAbort = flag;
    }

    public boolean isAbortPending() {
        return pendingAbort;
    }

    //===================
    //  Private Methods
    //===================
    private void reset() {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0");
        client = new DefaultHttpClient(params);
        loginTimeStamp = 0;
    }

    private LOGGED validateLoginJobmine(BufferedReader reader) throws IOException {
        char[] buffer = new char[LOGIN_READ_LENGTH];
        reader.read(buffer, 0, LOGIN_READ_LENGTH);
        String text = new String(buffer);

        if (text.contains(LOGIN_UNIQUE_STRING)) {
            // On login page
            reader.skip(LOGIN_ERROR_MSG_SKIP);
            reader.read(buffer, 0, LOGIN_READ_LENGTH);
            text = new String(buffer);

            // Check for offline error
            if (text.contains(LOGIN_OFFLINE_MESSAGE)) {
                return LOGGED.OFFLINE;
            }
            return LOGGED.OUT;

        } else if (text.contains(FAILED_URL)) {
            loginTimeStamp = 0;
            return LOGGED.OUT;
        }
        return LOGGED.IN;
    }

    private BufferedReader getReaderFromResponse(HttpResponse response) throws IllegalStateException, IOException {
        return getReaderFromResponse(response, DEFAULT_HTML_ENCODER);
    }

    private BufferedReader getReaderFromResponse(HttpResponse response, String encoder) throws IllegalStateException, IOException {
        InputStream in = response.getEntity().getContent();
        return new BufferedReader(new InputStreamReader(in, encoder), BUFFER_READER_SIZE);
    }

    private void updateTimestamp() {
        synchronized(lock) {
            loginTimeStamp = System.currentTimeMillis();
        }
    }
}