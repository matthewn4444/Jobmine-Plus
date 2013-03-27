package com.jobmineplus.mobile.activities.jbmnpls;

import java.io.IOException;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
        descrFragment.setJob(job);
        detFragment.setJob(job);
    }

    public static final class JobDescription extends TabItemFragment<Job> {
        public static JobDescription newInstance() {
            return new JobDescription();
        }

        public JobDescription() {
            init(R.layout.job_description_content, new int[]{
                    R.id.employer,
                    R.id.title,
                    R.id.location,
                    R.id.openings,
                    R.id.grades,
                    R.id.warning,
                    R.id.description
            });
        }

        public void setJob(Job job) {
            setData(job);
        }

        public void setValues(View[] views, Job job) {
            int opennings = job.getNumberOfOpenings();
            ((TextView)views[0]).setText(job.getEmployerFullName());
            ((TextView)views[1]).setText(job.getTitle());
            ((TextView)views[2]).setText(job.getLocation());
            ((TextView)views[3]).setText("Opennings: " + (opennings == 0 ? "0" : Integer.toString(opennings)));
            ((TextView)views[4]).setText(job.areGradesRequired() ? "Required" : "[none]");
            ((TextView)views[5]).setText(job.getDescriptionWarning());
            ((TextView)views[6]).setText(job.getDescription());
        }
    }

    public static final class JobDetails extends TabItemFragment<Job> {
        public static JobDetails newInstance() {
            return new JobDetails();
        }

        public JobDetails() {
            init(R.layout.job_description_details, new int[]{
                    R.id.open_date,
                    R.id.last_day,
                    R.id.hiring_support,
                    R.id.work_term,
                    R.id.levels,
                    R.id.discplines
            });
        }

        public void setJob(Job job) {
            setData(job);
        }

        public void setValues(View[] views, Job job) {
            ((TextView)views[0]).setText(DISPLAY_DATE_FORMAT.format(job.getOpenDateToApply()));
            ((TextView)views[1]).setText(DISPLAY_DATE_FORMAT.format(job.getLastDateToApply()));
            ((TextView)views[2]).setText(job.getHiringSupportName());
            ((TextView)views[3]).setText(job.getWorkSupportName());
            ((TextView)views[4]).setText(job.getLevelsAsString());
            ((TextView)views[5]).setText(job.getDisciplinesAsString());
        }
    }
}
