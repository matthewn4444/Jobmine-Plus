package com.jobmineplus.mobile.activities.jbmnpls;

import org.jsoup.nodes.Document;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.JbmnplsException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.services.JobService;
import com.jobmineplus.mobile.widgets.Job;

public class Description extends JbmnplsTabActivityBase{
	
	protected Job job;
	
	//===============
	// 	Ui Objects 
	//===============
	TextView employer;
	TextView title;
	TextView openings;
	TextView location;
	TextView levels;
	TextView grades;
	TextView warning;
	TextView openDate;
	TextView closedDate;
	TextView hiringSupport;
	TextView worktermSupport;
	TextView disciplines;
	TextView description;
	
	//====================
	// 	Override Methods
	//====================
	
	@Override
	protected String setUp(Bundle savedInstanceState) throws JbmnplsParsingException{
		setContentView(R.layout.job_description);
		int id = Integer.parseInt(getIntent().getStringExtra("jobId"));
		if (id == 0) {
			throw new JbmnplsParsingException("Did not receive an id going here.");
		}
		job = jobService.getJobById(id);
		if (job == null) {
			throw new JbmnplsParsingException("This id does not have a job object");
		}
		return "";		//Can be anything because we override onRequestData()
	}
	
	@Override
	protected void defineUI() {
		employer = 			(TextView) findViewById(R.id.employer);
//		title = 			(TextView) findViewById(R.id.title);
//		location = 			(TextView) findViewById(R.id.location);
//		openings = 			(TextView) findViewById(R.id.openings);
//		levels = 			(TextView) findViewById(R.id.levels);
//		grades = 			(TextView) findViewById(R.id.grades);
//		warning = 			(TextView) findViewById(R.id.warning);
//		openDate = 			(TextView) findViewById(R.id.open_date);
//		closedDate = 		(TextView) findViewById(R.id.last_day);
//		hiringSupport = 	(TextView) findViewById(R.id.hiring_support);
//		worktermSupport = 	(TextView) findViewById(R.id.work_term);
//		disciplines = 		(TextView) findViewById(R.id.discplines);
//		description = 		(TextView) findViewById(R.id.description);
//		
		createTab("description", "Description");
		createTab("employer", "Employer Info");
		
		super.defineUI();
	}

	@Override
	public View onTabSwitched(String tag) {
		// TODO Auto-generated method stub
		return employer;
	}
	
	@Override
	protected String onRequestData(String url) {
		return job.grabDescriptionData();
	}
	
	//Not needed because overriding requestData()
	@Override
	protected void parseWebpage(Document doc) {
		System.out.println("Workes?");
	}	
	
	//=====================
	// 	Protected Methods
	//=====================
	
	

	


	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	
}
