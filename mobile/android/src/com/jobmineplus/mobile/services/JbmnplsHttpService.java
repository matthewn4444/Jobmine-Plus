
package com.jobmineplus.mobile.services;

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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.widgets.StopWatch;

public final class JbmnplsHttpService {

    /*
     * Bugs:
     *  There is a problem with logging back in when leaving the app and coming back
     *      Might be that this class is recreated?
     *      Might be that it thought it was logged in and failed to parse?
     *
     *
     *
     * New stuff
     *      Signin HTML for JobMine. <--- this is for
     *      not authorized for this time period
     *      Invalid URL - no Node found in <--- broken url
     *      Invalid signon time for user
     */

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
    private static final int AUTO_LOGOUT_TIME           = 1000 * 60 * 60 * 20;   //20 min
    private static final int BUFFER_READER_SIZE         = 1024;

    // Login constants
    private static final String LOGIN_UNIQUE_STRING     = "Signin HTML for JobMine.";
    private static final String LOGIN_INVALID_CRED      = "Your User ID and/or Password are invalid.";
    private static final String DEFAULT_HTML_ENCODER    = "UTF-8";
    private static final String FAILED_URL              = "Invalid URL - no Node found in";
    private static final int    LOGIN_READ_LENGTH       = 300;
    private static final int    LOGIN_ERROR_MSG_SKIP    = 3500;


    //=====================
    //  Private Variables
    //=====================
    private static HttpClient client;
    private static JbmnplsHttpService instance = null;
    private static Object lock = new Object();
    private static long loginTimeStamp = 0;
    private static String username = "";
    private static String password = "";

    //=========================
    //  Singleton Definition
    //=========================
    private JbmnplsHttpService() {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0");
        client = new DefaultHttpClient(params);
    }

    public static JbmnplsHttpService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new JbmnplsHttpService();
                }
            }
        }
        return instance;
    }

    //==============
    //  Login Data
    //==============
    public synchronized void setLoginCredentials(String username, String password) {
        JbmnplsHttpService.username = username;
        JbmnplsHttpService.password = password;
    }

    public synchronized String getUsername() {
        return username;
    }

    public synchronized String getPassword() {
        return password;
    }

    public synchronized boolean isLoggedIn() {
        long timeNow = System.nanoTime() / 1000000;
        return loginTimeStamp != 0 && (timeNow - loginTimeStamp) < AUTO_LOGOUT_TIME;
    }

    public LOGGED login() {
        return login(username, password);
    }

    public synchronized LOGGED login(String username, String password) {
        loginTimeStamp = 0;
        if (username.length() == 0 || password.length() == 0) {
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
            HttpResponse response = post(nameValuePairs, JbmnplsHttpService.POST_LINKS.LOGIN);
            System.out.println(s.elapsed() + " ms login post");
            if (response == null || response.getStatusLine().getStatusCode() != 200) {
                return LOGGED.OUT;
            }

            reader = getReaderFromResponse(response);
            char[] buffer = new char[LOGIN_READ_LENGTH];
            reader.read(buffer, 0, LOGIN_READ_LENGTH);
            String text = new String(buffer);

            if (text.contains(LOGIN_UNIQUE_STRING)) {
                // On login page
                reader.skip(LOGIN_ERROR_MSG_SKIP);
                reader.read(buffer, 0, LOGIN_READ_LENGTH);
                text = new String(buffer);
                // Check for offline error
                if (text.contains(LOGIN_INVALID_CRED)) {
                    // Failed login and password
                    return LOGGED.OUT;
                }
                // Offline or some error
                return LOGGED.OFFLINE;

            } else if (text.contains(FAILED_URL)) {
                System.out.println("Failed login post/url");
                return LOGGED.OUT;
            }
            // Successful login
            setLoginCredentials(username, password);
            updateTimestamp();
            return LOGGED.IN;
        } catch (IOException e) {
            e.printStackTrace();
            return LOGGED.OUT;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch(IOException e) {
                e.printStackTrace();
                return LOGGED.OUT;
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
    public synchronized HttpResponse get(String url) {
        HttpResponse response = null;
        try {
            HttpGet getRequest = new HttpGet(url);
            StopWatch s = new StopWatch(true);
            response = client.execute(getRequest);
            System.out.println(s.elapsed() + " ms get request");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return response;
    }

    public synchronized JSONObject getJSON(String url) {
        HttpResponse response = get(url);
        if (response == null) {
            return null;
        }
        return getJSONFromResponse( response );
    }

    public synchronized String getJobmineHtml (String url) throws JbmnplsLoggedOutException{
        HttpResponse response = get(url);
        String html;
        if (response == null) {
            return null;
        }
        try {
            StopWatch s = new StopWatch(true);
            html = getJbmnHtmlFromHttpResponse(response);
            System.out.println(s.elapsed() + " ms to turn into html");
        } catch (Exception e) {
            e.printStackTrace();
            throw new JbmnplsLoggedOutException();
        }
        return html;
    }

    //======================
    //  POST HTTP Requests
    //======================
    public synchronized HttpResponse post(List<NameValuePair> postData, String url) {
        HttpResponse response = null;
        try {
            HttpPost postRequest = new HttpPost(url);
            postRequest.setEntity(new UrlEncodedFormEntity(postData));
            response = client.execute(postRequest);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized JSONObject postJSON(List<NameValuePair> postData, String url) {
        HttpResponse response = post(postData, url);
        if (response == null) {
            return null;
        }
        return getJSONFromResponse( response );
    }

    public synchronized String postJobmineHtml (List<NameValuePair> postData, String url) throws JbmnplsLoggedOutException {
        HttpResponse response = post(postData, url);
        String html;
        if (response == null) {
            return null;
        }
        try {
            html = getJbmnHtmlFromHttpResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JbmnplsLoggedOutException();
        }
        return html;
    }

    //========================
    //  HTML/JSON Conversion
    //========================

    public String getHtmlFromHttpResponse(HttpResponse response) throws IllegalStateException, IOException {
        return getHtmlFromHttpResponse(response, DEFAULT_HTML_ENCODER);
    }
    public String getHtmlFromHttpResponse(HttpResponse response, String encoder) throws IllegalStateException, IOException {
        InputStream in = response.getEntity().getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoder), BUFFER_READER_SIZE);
        StringBuilder str = new StringBuilder(in.available());
        String line = null;
        while((line = reader.readLine()) != null) {
            str.append(line);
        }
        in.close();
        return str.toString();
    }

    public String getJbmnHtmlFromHttpResponse(HttpResponse response) throws IllegalStateException, IOException, JbmnplsLoggedOutException {
        return getJbmnHtmlFromHttpResponse(response, DEFAULT_HTML_ENCODER);
    }
    public String getJbmnHtmlFromHttpResponse(HttpResponse response, String encoder) throws JbmnplsLoggedOutException, IllegalStateException, IOException {
        InputStream in = response.getEntity().getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoder), BUFFER_READER_SIZE);
        StringBuilder str = new StringBuilder();
        String line = null;

        while((line = reader.readLine()) != null) {
            if (line.contains(LOGIN_UNIQUE_STRING)) {
                throw new JbmnplsLoggedOutException();
            }
            str.append(line);
        }
        in.close();

        updateTimestamp();
        return str.toString();
    }

    private JSONObject getJSONFromResponse(HttpResponse response) {
        String html;
        JSONObject json;
        try {
            html = getHtmlFromHttpResponse(response);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        try {
            json = new JSONObject(new JSONTokener(html));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }

    //===================
    //  Private Methods
    //===================
    private BufferedReader getReaderFromResponse(HttpResponse response) throws IllegalStateException, IOException {
        return getReaderFromResponse(response, DEFAULT_HTML_ENCODER);
    }

    private BufferedReader getReaderFromResponse(HttpResponse response, String encoder) throws IllegalStateException, IOException {
        InputStream in = response.getEntity().getContent();
        return new BufferedReader(new InputStreamReader(in, encoder), BUFFER_READER_SIZE);
    }

    private int findLinesOfWordsFromResponse(HttpResponse response, List<String> lines) throws IllegalStateException, IOException {
        if (lines.isEmpty()) {
            return 0;
        }
        ArrayList<String> aLines = new ArrayList<String>(lines);
        InputStream in = response.getEntity().getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, DEFAULT_HTML_ENCODER), BUFFER_READER_SIZE);
        String bufferLine = null;
        int numFound = 0;

        while((bufferLine = reader.readLine()) != null) {
            for (int i = aLines.size() - 1; i >= 0; i--) {
                String line = aLines.get(i);
                if (bufferLine.contains(line)) {
                    numFound++;
                    aLines.remove(i);
                }
            }
            if (aLines.isEmpty()) {
                break;
            }
        }
        reader.close();
        return numFound;
    }

    private synchronized void updateTimestamp() {
        loginTimeStamp = System.nanoTime() / 1000000;
    }
}