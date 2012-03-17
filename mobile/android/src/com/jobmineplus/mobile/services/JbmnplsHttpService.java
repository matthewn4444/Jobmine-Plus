package com.jobmineplus.mobile.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;

public final class JbmnplsHttpService {
    
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
    static public final int LOGIN           = 0;
    static public final int LOGGED_OUT      = 1;
    static public final int LOGGED_OFFLINE  = 2;
    
    //=============
    //  Constants
    //=============
    private static final int AUTO_LOGOUT_TIME           = 1000 * 60 * 60 * 20;   //20 min 
    private static final int BUFFER_READER_SIZE         = 1024/2;                //512 Characters/line
    private static final String LOGIN_UNIQUE_STRING     = "document.login.userid.focus";
    private static final String OFFLINE_UNIQUE_STRING1  = "Invalid signon time for user";
    private static final String OFFLINE_UNIQUE_STRING2  = "not authorized for this time period";
    private static final String DEFAULT_HTML_ENCODER    = "UTF-8";
    
    //=====================
    //  Private Variables
    //=====================
    private static HttpClient client = new DefaultHttpClient();
    private static JbmnplsHttpService instance = null;
    private static Object lock = new Object();
    private static long loginTimeStamp = 0;
    private static String username = "";
    private static String password = "";
    
    //=========================
    //  Singleton Definition
    //=========================
    private JbmnplsHttpService() {}
    
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
        long timeNow = new java.util.Date().getTime();
        return loginTimeStamp != 0 && (timeNow - loginTimeStamp) < AUTO_LOGOUT_TIME;
    }
    
    public int login() { 
        return login(username, password);
    }
    
    public synchronized int login(String username, String password) {
        loginTimeStamp = 0;
        if (username.length() == 0 || password.length() == 0) {
            return LOGGED_OUT;
        }
        
        int found = 0;
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("httpPort", ""));
        nameValuePairs.add(new BasicNameValuePair("submit", "Submit"));
        nameValuePairs.add(new BasicNameValuePair("timezoneOffset", "480"));
        nameValuePairs.add(new BasicNameValuePair("pwd", password));
        nameValuePairs.add(new BasicNameValuePair("userid", username));
        
        try {
            HttpResponse response = post(nameValuePairs, JbmnplsHttpService.POST_LINKS.LOGIN);
            if (response.getStatusLine().getStatusCode() != 200) {
                return LOGGED_OUT;
            }
            List<String> list = new ArrayList<String>();
            list.add(OFFLINE_UNIQUE_STRING2);
            list.add(OFFLINE_UNIQUE_STRING1);
            list.add(LOGIN_UNIQUE_STRING);
            found = findLinesOfWordsFromResponse(response, list);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return LOGGED_OUT;
        } catch (UnknownHostException e) {
            System.out.println("Cannot find host!");
            e.printStackTrace();
            return LOGGED_OUT;
        } catch (IOException e) {
            e.printStackTrace();
            return LOGGED_OUT;
        }
        switch (found) {
        case 0:
            setLoginCredentials(username, password);
            updateTimestamp();
            return LOGIN;
        case 1:
            return LOGGED_OUT;
        default:
            return LOGGED_OFFLINE;
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
            response = client.execute(getRequest);
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
            html = getJbmnHtmlFromHttpResponse(response);
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
        StringBuilder str = new StringBuilder();
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
        in.close();
        return numFound;
    }

    private synchronized void updateTimestamp() {
        loginTimeStamp = new java.util.Date().getTime();
    }
}