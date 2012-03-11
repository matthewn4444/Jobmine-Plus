package com.jobmineplus.mobile.activities.jbmnpls;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jobmineplus.mobile.JbmnplsApplication;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.ProgressDialogAsyncTaskBase;

public abstract class JbmnplsActivityBase extends FragmentActivity {

	//=================
	// 	Declarations
	//=================
	protected static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy");
	
	private String dataUrl = null; 		//Use JbmnPlsHttpService.GET_LINKS.<url>

	private JbmnplsHttpService service;
	protected JbmnplsApplication app;
	protected GetHtmlTask task = null;
	protected final String LOADING_MESSAGE = "Fetching data...";
	
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
	protected abstract void defineUI(Bundle savedInstanceState);
	
	/**
	 * Here you are given the document of the dataUrl page
	 * specified in setUp(). Also render the layout with the 
	 * data here.
	 * 
	 * @param doc
	 */
	protected abstract Object parseWebpage(Document doc);
	
	/**
	 * Calling this when the parseWebpage is complete.
	 * This is used when you need to update any visual element
	 * that you cannot do in parseWebpage
	 * 
	 * @param doc
	 */
	protected abstract void onRequestComplete(Object arg);
	
	//====================
	// 	Override Methods
	//====================
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (JbmnplsApplication) getApplication();
        service = JbmnplsHttpService.getInstance();
        dataUrl = setUp(savedInstanceState);
        defineUI(savedInstanceState);
		requestData();
	}
	
	protected void requestData() throws RuntimeException {
		verifyLogin();
		
		if (task == null) {
			task = new GetHtmlTask(this, LOADING_MESSAGE);
			if (dataUrl == null) {
				throw new RuntimeException("Class that extended JbmnPlsActivityBase without specifying a dataurl.");
			}
		}
		task.execute(dataUrl);
	}

	//=================================
	//	Class Public/Protected Methods
	//=================================
	
	/**
	 * You can override this function if you need to fetch
	 * something else besides the default url.
	 * Return null if it failed and this class will throw 
	 * a dialog saying it failed otherwise return the html
	 * @param url
	 * @return null if failed or String that is the html
	 */
	protected String onRequestData(String[] args) {
		String url = args[0];
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
	
	protected boolean isLoading() {
		return task != null && task.isRunning();
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
	
	private class GetHtmlTask extends ProgressDialogAsyncTaskBase<String, Void, Object>{
		
		public GetHtmlTask(Activity activity, String dialogueMessage) {
			super(activity, dialogueMessage);
		}

		@Override
		protected Object doInBackground(String... params) {
			String html = ((JbmnplsActivityBase) activity).onRequestData(params);
			Document doc = null;
			if (html == null) {
				//TODO handle with message
			} else {
				doc = Jsoup.parse(html);
				((JbmnplsActivityBase) activity).parseWebpage(doc);
			}
			return doc;
		}
		
		@Override
		protected void onPostExecute(Object result) {
			super.onPostExecute(result);
			onRequestComplete(result);
		}
	}
	
	//==================
	//	JobListAdapter
	//==================
	
	protected class JobListAdapter extends ArrayAdapter<Integer>{
    	private ArrayList<Integer> entries;
    	private Activity activity;
    	
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
    		final Job entry = app.getJob(id);
    		if (entry != null) {
    			elements.job_title.setText(entry.getTitle());
    			elements.job_employer.setText(entry.getEmployer());
    			elements.job_status.setText(entry.getDisplayStatus());
    			elements.last_date.setText(DISPLAY_DATE_FORMAT.format(entry.getLastDateToApply()));
    			elements.numApps.setText(Integer.toString(entry.getNumberOfApplications()));
    		}
    		return item;
    	}
    }
}	

	
	
	
	
	
	
