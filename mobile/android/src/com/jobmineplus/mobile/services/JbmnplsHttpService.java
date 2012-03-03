package com.jobmineplus.mobile.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.security.Timestamp;
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

import com.jobmineplus.mobile.R;

import android.widget.ScrollView;
import android.widget.TextView;

public final class JbmnplsHttpService {
	static public final class GET_LINKS {
		public static final String DOCUMENTS	= "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_STUDDOCS";
		public static final String PROFILE 		= "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_STUDENT";
		public static final String SKILLS 		= "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_STUDENT?PAGE=UW_CO_STU_SKL_MTN";
		public static final String SEARCH 		= "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_JOBSRCH";
		public static final String SHORTLIST 	= "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_JOB_SLIST";
		public static final String APPLICATIONS = "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_APP_SUMMARY";
		public static final String INTERVIEWS 	= "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_STU_INTVS";
		public static final String RANKINGS 	= "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_RNK2";
		public static final String DESCRIP_PRE 	= "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_JOBDTLS?UW_CO_JOB_ID=";
	}
	
	static public final class POST_LINKS {
		public static final String LOGIN = "https://jobmine.ccol.uwaterloo.ca/psp/SS/?cmd=login&languageCd=ENG&sessionId=";
		public static final String LOGOUT = "https://jobmine.ccol.uwaterloo.ca/psp/SS/?cmd=login&languageCd=ENG&";
	}
	
	private static final int AUTO_LOGOUT_TIME = 1000 * 60 * 60 * 20; 	//20 min 
	private static final int BUFFER_READER_SIZE = 1024/2;				//512 Characters/line
	private static final String LOGIN_UNIQUE_STRING = "document.login.userid.focus";
	
	private static JbmnplsHttpService instance = null;
	private static Object lock = new Object();
	
	private static final String DEFAULT_HTML_ENCODER = "UTF-8";
	private static long loginTimeStamp = 0;
	
	//Cannot Declare this as a class, use getInstance()
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
	
	public synchronized boolean isLoggedIn() {
		long timeNow = new java.util.Date().getTime();
		return loginTimeStamp != 0 && (timeNow - loginTimeStamp) < AUTO_LOGOUT_TIME;
	}
	
	public Boolean syncLogin(String username, String password) {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("httpPort", ""));
        nameValuePairs.add(new BasicNameValuePair("submit", "Submit"));
        nameValuePairs.add(new BasicNameValuePair("timezoneOffset", "480"));
        nameValuePairs.add(new BasicNameValuePair("pwd", password));
        nameValuePairs.add(new BasicNameValuePair("userid", username));
        
        Boolean loggedIn = false;
        synchronized (lock) {
	        try {
	        	HttpResponse response = post(nameValuePairs, JbmnplsHttpService.POST_LINKS.LOGIN);
	        	if (response.getStatusLine().getStatusCode() != 200) {
	        		return false;
	        	}
	        	loggedIn = !isTextOnLineOfHttpResponse(response, LOGIN_UNIQUE_STRING);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				//return false;
			} catch (UnknownHostException e) {
				System.out.println("Cannot find host!");
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
        }
        updateTimestamp();
		return loggedIn;
	}
	
	public boolean syncLogout() {
		Boolean loggedIn = false;
			synchronized (lock) {
		 	try {
		    	HttpResponse response = get(JbmnplsHttpService.POST_LINKS.LOGOUT);
		    	if (response.getStatusLine().getStatusCode() != 200) {
		    		return false;
		    	}
		    	loggedIn = isTextOnLineOfHttpResponse(response, "document.login.userid.focus");
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		 	loginTimeStamp = 0;
		}
		return loggedIn;
	}
	
	private boolean isTextOnLineOfHttpResponse(HttpResponse response, String word) throws IllegalStateException, IOException {
		InputStream in = response.getEntity().getContent();
    	BufferedReader reader = new BufferedReader(new InputStreamReader(in, DEFAULT_HTML_ENCODER), BUFFER_READER_SIZE);
    	Boolean found = false;
    	String line = null; 
    	while((line = reader.readLine()) != null) {
    		System.out.println(line);
    		if(line.contains(word)) {
    			found = true;
    			break;
    		}
    	}
    	in.close();
    	return found;
	}
	
	public HttpResponse post(List<NameValuePair> postData, String url) throws ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();
		HttpPost postRequest = new HttpPost(url);
		postRequest.setEntity(new UrlEncodedFormEntity(postData));
		HttpResponse response = client.execute(postRequest);
		updateTimestamp();
		return response;
	}
	
	public HttpResponse get(String url) throws ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();
		HttpGet getRequest = new HttpGet(url);
        return client.execute(getRequest);
	}
	
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
	
	private synchronized void updateTimestamp() {
		loginTimeStamp = new java.util.Date().getTime();
	}
	
}