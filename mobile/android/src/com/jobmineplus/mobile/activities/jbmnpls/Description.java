package com.jobmineplus.mobile.activities.jbmnpls;

import java.io.IOException;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.Job;

public class Description extends JbmnplsTabActivityBase {

    private static class LISTS {
        public static String DESCRIPTION = "description";
        public static String DETAILS = "details";
    }

    protected Job job;

    // ===============
    // Ui Objects
    // ===============
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

    ScrollView descriptionLayout;
    ScrollView detailsLayout;

    FrameLayout container;

    // ====================
    // Override Methods
    // ====================

    @Override
    protected String setUp(Bundle savedInstanceState)
            throws JbmnplsParsingException {
        setContentView(R.layout.job_description);
        int id = Integer.parseInt(getIntent().getStringExtra("jobId"));
        if (id == 0) {
            throw new JbmnplsParsingException(
                    "Did not receive an id going here.");
        }
        job = jobDataSource.getJob(id);
        if (job == null) {
            throw new JbmnplsParsingException(
                    "This id does not have a job object");
        }
        return ""; // Can be anything because we override onRequestData()
    }

    @Override
    protected void defineUI(Bundle savedInstanceState) {
        super.defineUI(savedInstanceState);
        container = (FrameLayout) findViewById(android.R.id.tabcontent);

        // Description Tab
        employer = (TextView) findViewById(R.id.employer);
        title = (TextView) findViewById(R.id.title);
        location = (TextView) findViewById(R.id.location);
        openings = (TextView) findViewById(R.id.openings);
        grades = (TextView) findViewById(R.id.grades);
        warning = (TextView) findViewById(R.id.warning);
        description = (TextView) findViewById(R.id.description);
        descriptionLayout = (ScrollView) findViewById(R.id.description_layout);

        // Details Tab
        levels = (TextView) findViewById(R.id.levels);
        openDate = (TextView) findViewById(R.id.open_date);
        closedDate = (TextView) findViewById(R.id.last_day);
        hiringSupport = (TextView) findViewById(R.id.hiring_support);
        worktermSupport = (TextView) findViewById(R.id.work_term);
        disciplines = (TextView) findViewById(R.id.discplines);
        detailsLayout = (ScrollView) findViewById(R.id.details_layout);

        // Make tabs

        createTab(LISTS.DESCRIPTION, "Description", descriptionLayout);
        createTab(LISTS.DETAILS, "Details", detailsLayout);

        container.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void requestData() throws RuntimeException {
        // If Job has description then load from data, if not then get it
        if (job.hasDescriptionData()) {
            fillInDescription();
        } else {
            super.requestData();
        }
    }

    @Override
    protected String onRequestData(String[] args) throws IOException {
        String descriptionData = job.grabDescriptionData();
        log(job.hasDescriptionData(), "Write to job");
        jobDataSource.addJob(job);      // updates with the description data
        return descriptionData;
    }

    @Override
    protected void parseWebpage(String html) {
        // Not needed because it is all done in onRequestData
    }

    @Override
    protected void onRequestComplete() {
        fillInDescription();
    }

    // =====================
    // Protected Methods
    // =====================
    protected void fillInDescription() {
        // Description Tab
        employer.setText(job.getEmployerFullName());
        title.setText(job.getTitle());
        location.setText(job.getLocation());
        int opennings = job.getNumberOfOpenings();
        openings.setText("Opennings: "
                + (opennings == 0 ? "0" : Integer.toString(opennings)));
        grades.setText(job.areGradesRequired() ? "Required" : "[none]");
        warning.setText(job.getDescriptionWarning());
        description.setText(job.getDescription());

        // Details Tab
        openDate.setText(DISPLAY_DATE_FORMAT.format(job.getOpenDateToApply()));
        closedDate
                .setText(DISPLAY_DATE_FORMAT.format(job.getLastDateToApply()));
        hiringSupport.setText(job.getHiringSupportName());
        worktermSupport.setText(job.getWorkSupportName());
        levels.setText(job.getLevelsAsString());
        disciplines.setText(job.getDisciplinesAsString());

        container.setVisibility(View.VISIBLE);
    }
}
