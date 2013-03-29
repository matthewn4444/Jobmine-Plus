package com.jobmineplus.mobile.activities.jbmnpls;

import java.io.IOException;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.TabItemFragment;

public class Description extends JbmnplsPageActivityBase {

    private static class TABS {
        public static String DESCRIPTION = " Description ";
        public static String DETAILS = "Details";
    }

    protected Job job;
    protected JobDescription descrFragment;
    protected JobDetails detFragment;

    // ====================
    // Override Methods
    // ====================

    @Override
    protected String setUp(Bundle savedInstanceState)
            throws JbmnplsParsingException {
        pageName = Description.class.getName();
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
        descrFragment = JobDescription.newInstance();
        detFragment = JobDetails.newInstance();
        createTab(TABS.DESCRIPTION, descrFragment);
        createTab(TABS.DETAILS, detFragment);

        setEmptyText(getString(R.string.description_no_data));
    }

    @Override
    protected void requestData() throws RuntimeException {
        // If Job has description then load from data, if not then get it
        if (job.hasDescriptionData()) {
            fillInDescription();
        } else {
            ActionBar bar = getSupportActionBar();
            bar.setTitle(getString(R.string.description_getting_data));
            super.requestData();
        }
    }

    @Override
    protected String onRequestData(String[] args) throws IOException {
        String descriptionData = job.grabDescriptionData(client);
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

    @Override
    protected long doOffine() {
        // Not needed because we get the job from the database already
        return 0;
    }

    // =====================
    // Protected Methods
    // =====================
    protected void fillInDescription() {
        ActionBar bar = getSupportActionBar();
        String employer = job.getEmployerFullName() == "" ? job.getEmployerFullName() : job.getEmployer();
        bar.setSubtitle(job.getTitle());
        bar.setTitle(employer);

        if (job.hasDescriptionData()) {
            descrFragment.setJob(job);
            detFragment.setJob(job);
        } else {
            setIsEmpty(true);
        }
    }

    public static final class JobDescription extends TabItemFragment<Job> {
        public static JobDescription newInstance() {
            return new JobDescription();
        }

        public JobDescription() {
            init(R.layout.job_description_content, new int[]{
                    R.id.warning,
                    R.id.divider,
                    R.id.description
            });
        }

        public void setJob(Job job) {
            setData(job);
        }

        public void setValues(View[] views, Job job) {
            String warning = job.getDescriptionWarning();
            if (!warning.equals("")) {
                ((TextView)views[0]).setText(warning);
                views[1].setVisibility(View.VISIBLE);
            } else {
                views[0].setVisibility(View.GONE);
            }
            ((TextView)views[2]).setText(job.getDescription());
        }
    }

    public static final class JobDetails extends TabItemFragment<Job> {
        public static JobDetails newInstance() {
            return new JobDetails();
        }

        public JobDetails() {
            init(R.layout.job_description_details, new int[]{
                    R.id.grades,
                    R.id.openings,
                    R.id.location,
                    R.id.date_range,
                    R.id.work_term,
                    R.id.hiring_support,
                    R.id.discplines,
                    R.id.levels
            });
        }

        public void setJob(Job job) {
            setData(job);
        }

        public void setValues(View[] views, Job job) {
            // Grades
            if (!job.areGradesRequired()) {
                views[0].setVisibility(View.GONE);
            }

            // Opennings
            int opennings = job.getNumberOfOpenings();
            ((TextView)views[1]).setText((opennings == 0 ? "No" : opennings) + " Openning"
                    + (opennings == 1 ? "" : "s"));

            // Location
            ((TextView)views[2]).setText(job.getLocation());

            // Dates
            String openingDate = DISPLAY_DATE_FORMAT.format(job.getOpenDateToApply());
            String lastDate = DISPLAY_DATE_FORMAT.format(job.getLastDateToApply());

            if (openingDate.equals(lastDate)) {
                ((TextView)views[3]).setText(getString(R.string.description_no_dates));
            } else {
                ((TextView)views[3]).setText(openingDate + " - " + lastDate);
            }

            // Work Support
            ((TextView)views[4]).setText(job.getWorkSupportName());

            // Hiring Support
            ((TextView)views[5]).setText(job.getHiringSupportName());

            // Disciplines
            ((TextView)views[6]).setText(job.getDisciplinesAsString("\n"));

            // Levels
            ((TextView)views[7]).setText(job.getLevelsAsString("\n"));
        }
    }
}
