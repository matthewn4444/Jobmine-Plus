package com.jobmineplus.mobile.activities.jbmnpls;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.HomeActivity;
import com.jobmineplus.mobile.activities.LoginActivity;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.services.JobService;
import com.jobmineplus.mobile.widgets.Job;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public abstract class JbmnplsActivityBase extends Activity {

	//=================
	// 	Declarations
	//=================
	private String dataUrl = null; 		//Use JbmnPlsHttpService.GET_LINKS.<url>

	private JbmnplsHttpService service;
	protected JobService jobService;
	private ProgressDialog progress;
	
	//====================
	// 	Override Methods
	//====================
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        service = JbmnplsHttpService.getInstance();
        jobService = JobService.getInstance();
        dataUrl = setUp(savedInstanceState);
        verifyLogin();
        defineUI();
        requestData();
    }
	
	//====================
	//	Abstract Methods
	//====================
	
	/**
	 * Running setUp() you need to specify the "layout" and
	 * "dataUrl".
	 * For example:
	 * 	protected void setUp() {
	 * 		setContentView(layout);
	 *		String url = JbmnplsHttpService.GET_LINKS.APPLICATIONS;
	 *		return url;
	 * 	}
	 * @param savedInstanceState
	 * @return dataUrl (String) that gets the data that will be parsed
	 */
	protected abstract String setUp(Bundle savedInstanceState);
	
	/**
	 * This allows the user to define all their UI object variables here.
	 * is necessary but does not always need it. Please specify layout in
	 * setUp();
	 */
	protected abstract void defineUI();
	
	/**
	 * Here you are given the document of the dataUrl page
	 * specified in setUp(). Also render the layout with the 
	 * data here.
	 * @param doc
	 */
	protected abstract void parseWebpage(Document doc);
	
	//=================================
	//	Class Public/Protected Methods
	//=================================
	
	protected void requestData() throws RuntimeException {
		GetHtmlTask task = new GetHtmlTask(this);
		if (dataUrl == null) {
			throw new RuntimeException("Class that extended JbmnPlsActivityBase without specifying a dataurl.");
		}
		task.execute(dataUrl);
	}
	
	/**
	 * You can override this function if you need to fetch
	 * something else besides the default url.
	 * Return null if it failed and this class will throw 
	 * a dialog saying it failed otherwise return the html
	 * @param url
	 * @return null if failed or String that is the html
	 */
	protected String onRequestData(String url) {
		try {
			return service.getHtmlFromHttpResponse(service.get(url));
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	protected boolean logout() {
		if (service.isLoggedIn()) {
			service.logout();
			goToLoginActivity();
		}
		return true;
	}
	
	protected void goToLoginActivity() {
		System.out.println("Not logged in, so please try again");
		//TODO
	}
	
	protected String getTextFromElement(Element e) {
		return e.text().replaceAll("\\s+", " ").trim();
	}
	
	protected Date getDateFromElement(Element e) {
		String text = getTextFromElement(e);
		try {
			return new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH).parse(text);
		} catch (ParseException error) {
			error.printStackTrace();
			return new Date();
		}
	}
	
	protected int getIntFromElement(Element e) {
		String text = getTextFromElement(e);
		return Integer.parseInt(text);
	}
	
	protected double getDoubleFromTD(Element e) {
		String text = getTextFromElement(e);
		return Double.parseDouble(text);
	}
	
	protected String getUrlFromElement(Element e) {
		Element anchor = e.select("a").first();
		if (anchor == null) {
			return null;
		}
		return anchor.attr("href");
	}
	
	protected void startActivity(Class<?> goToClass) {
		NameValuePair[] empty = null;
		startActivity(goToClass, empty);
	}
	
	protected void startActivity(Class<?> goToClass, NameValuePair ...args) {
		Intent in = new Intent(this, goToClass);
		if (args != null) {
			for (NameValuePair arg: args) {
				in.putExtra(arg.getName(), arg.getValue());
			}
		}
		startActivity(in);
	}
	
	protected Element parseTableById(Document doc, String id) throws JbmnplsParsingException {
		try{
			return doc.getElementById(id).select("tr:eq(1) table table").first();
		} catch(Exception e) {
			throw new JbmnplsParsingException("Problem parsing table.");
		}
	}
	
	protected void log(Object txt) {
		System.out.println(txt);
	}
	
	//=========================
	//	Class Private Methods
	//=========================
	
	private void tryToLoginAgain() {
		
	}
	
	private void verifyLogin() {
		if (!service.isLoggedIn()) {
			tryToLoginAgain();
		}
	}
	
	//=================
	//	Tasks Classes
	//=================
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if (progress != null && progress.isShowing()) {
			progress.dismiss();
		}
		super.onPause();
	}

	private class GetHtmlTask extends AsyncTask<String, Void, String> {
		private JbmnplsActivityBase activity;
		
		public GetHtmlTask (JbmnplsActivityBase a) {
			activity = a;
		}
		
		@Override
		protected void onPreExecute(){ 
    		super.onPreExecute();
    		progress = ProgressDialog.show(activity, "", 
					activity.getString(R.string.wait_post_message), true);
		}
		
		@Override
		protected String doInBackground(String... params) {
			return activity.onRequestData(params[0]);
		}
		
		@Override
		protected void onPostExecute(String html){
			if (progress.isShowing()) {
				progress.dismiss();
			}
			if (html == null) {
				//TODO handle with message
			} else {
				activity.parseWebpage(Jsoup.parse(html));
			}
		}
	}
	
	//==================
	//	JobListAdapter
	//==================
	
	protected class JobListAdapter extends ArrayAdapter<Integer>{
    	private ArrayList<Integer> entries;
    	private Activity activity;
    	private final SimpleDateFormat format = new SimpleDateFormat("MMM d, yyyy");
    	
    	public JobListAdapter(Activity a, int textViewResourceId, ArrayList<Integer> listOfIds) {
    		super(a, textViewResourceId, listOfIds);
    		entries = listOfIds;
    		activity = a;
    	}
    	
    	private class JobListElements {
    		public TextView job_title;
    		public TextView job_employer;
    		public TextView job_status;
    		public TextView last_date;
    		public TextView numApps;
    	}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		View item = convertView;
    		JobListElements elements;
    		if (item == null) {
    			LayoutInflater inflator = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			item = inflator.inflate(R.layout.job_widget, null);
    			elements = new JobListElements();
    			elements.job_title = 	(TextView) item.findViewById(R.id.job_title);
    			elements.job_employer = (TextView) item.findViewById(R.id.job_employer);
    			elements.job_status = 	(TextView) item.findViewById(R.id.job_status);
    			elements.last_date = 	(TextView) item.findViewById(R.id.job_last_day);
    			elements.numApps =		(TextView) item.findViewById(R.id.job_apps);
    			item.setTag(elements);
    		} else {
    			elements = (JobListElements) item.getTag();
    		}
    		
    		final int id = entries.get(position);
    		final Job entry = jobService.getJobById(id);
    		if (entry != null) {
    			elements.job_title.setText(entry.getTitle());
    			elements.job_employer.setText(entry.getEmployer());
    			elements.job_status.setText(entry.getDisplayStatus());
    			elements.last_date.setText(format.format(entry.getLastDateToApply()));
    			elements.numApps.setText(Integer.toString(entry.getNumberOfApplications()));
    		}
    		return item;
    	}
    }
}	

	
	
	
	
	
	
