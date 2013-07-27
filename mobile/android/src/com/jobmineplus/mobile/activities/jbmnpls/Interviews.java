package com.jobmineplus.mobile.activities.jbmnpls;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.bugsense.trace.BugSenseHandler;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.JbmnplsAdapterBase;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.TutorialHelper;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;
import com.jobmineplus.mobile.widgets.Job.HEADER;

public class Interviews extends JbmnplsPageListActivityBase implements TableParser.OnTableParseListener {

    //======================
    //  Declaration Objects
    //======================
    public final static String PAGE_NAME = Interviews.class.getName();
    protected final static String DATE_FORMAT = "d MMM yyyy";
    protected final static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
    private final TableParser parser = new TableParser();
    private final JobInterviewStartDateComparer isdComparer = new JobInterviewStartDateComparer();

    public final static class TABS {
        final public static String COMING_UP = "Coming Up";
        final public static String FINISHED = "Finished";
    }

    public final static HEADER[] SORT_HEADERS = {
        HEADER.JOB_TITLE,
        HEADER.EMPLOYER,
        HEADER.INTER_TYPE,
        HEADER.START_TIME,
        HEADER.INTERVIEWER
    };

    //======================
    // Table Definitions
    //======================

    // First Table
    public static final TableParserOutline INTERVIEWS_OUTLINE = new TableParserOutline(
            "UW_CO_STUD_INTV$scroll$0",
            HEADER.BLANK,
            HEADER.JOB_ID,
            HEADER.EMPLOYER_NAME,
            HEADER.JOB_TITLE,
            HEADER.DATE,
            HEADER.INTER_TYPE,
            HEADER.SELECT_TIME,
            HEADER.START_TIME,
            HEADER.LENGTH,
            HEADER.ROOM,
            HEADER.INSTRUCTIONS,
            HEADER.INTERVIEWER,
            HEADER.JOB_STATUS);

    // Second Table
    public static final TableParserOutline GROUPS_OUTLINE = new TableParserOutline(
            "UW_CO_GRP_STU_V$scroll$0",
            HEADER.BLANK,
            HEADER.JOB_ID,
            HEADER.EMPLOYER_NAME,
            HEADER.JOB_TITLE,
            HEADER.DATE,
            HEADER.START_TIME,
            HEADER.END_TIME,
            HEADER.ROOM,
            HEADER.INSTRUCTIONS);

    // Third Table
    public static final TableParserOutline SPECIAL_OUTLINE = new TableParserOutline(
            "UW_CO_NSCHD_JOB$scroll$0",
            HEADER.BLANK,
            HEADER.JOB_IDENTIFIER,
            HEADER.EMPLOYER_NAME,
            HEADER.JOB_TITLE,
            HEADER.INSTRUCTIONS);

    // Fourth Table
    public static final TableParserOutline CANCELLED_OUTLINE = new TableParserOutline(
            "UW_CO_SINT_CANC$scroll$0",
            HEADER.BLANK,
            HEADER.JOB_ID,
            HEADER.EMPLOYER,
            HEADER.JOB_TITLE);

    //================================
    //  Sorting Jobs
    //================================
    final class JobInterviewStartDateComparer implements Comparator<Job> {
        private boolean descending = false;

        public void shouldDescend(boolean d) {
            descending = d;
        }

        @Override
        public int compare(Job lhs, Job rhs) {
            if (lhs == rhs || lhs.equals(rhs)) {
                return 0;
            }
            Date l = lhs.getInterviewStartTime();
            Date r = rhs.getInterviewStartTime();

            if (l == null && r == null) {
                return 0;
            } else if (l == null) {
                return descending ? -1 : 1;
            } else if (r == null) {
                return descending ? 1 : -1;
            }

            if (descending) {
                return l.after(r) ? -1 : 1;
            } else {
                return l.before(r) ? -1 : 1;
            }
        }
    }

    //================================
    //  Widget Resource List Outline
    //================================
    protected final int[] WIDGET_RESOURCE_LIST = {
            R.id.job_title, R.id.job_employer, R.id.interviewer, R.id.date, R.id.time,
            R.id.room, R.id.type, R.id.instructions};


    //============================
    //  Static Public Methods
    //============================
    public static Job parseRowTableOutline(TableParserOutline outline, Object... jobData) {
        Job job = null;
        int id = (Integer) jobData[1];
        String employer = (String) jobData[2];
        String title = (String) jobData[3];

        if (outline.equals(SPECIAL_OUTLINE)) {
            job = new Job(id, employer, title, (String) jobData[4]);
        } else if (outline.equals(CANCELLED_OUTLINE)) {
            job = new Job(id, employer, title);
        } else {
            Date interviewDay = (Date) jobData[4];
            if (outline.equals(INTERVIEWS_OUTLINE)) {
                job = new Job(id, employer, title,
                        getDateFromDateWithTimeString(interviewDay, (String) jobData[7], 0),
                        getDateFromDateWithTimeString(interviewDay, (String) jobData[7], (Integer) jobData[8]),
                        (Job.INTERVIEW_TYPE) jobData[5], (String) jobData[9], (String) jobData[10],
                        (String) jobData[11]);
            } else {    //GROUPS_OUTLINE
                job = new Job(id, employer, title,
                        getDateFromDateWithTimeString(interviewDay, (String) jobData[5], 0),
                        getDateFromDateWithTimeString(interviewDay, (String) jobData[6], 0),
                        (String) jobData[7], (String) jobData[8]);
            }
        }
        return job;
    }

    //====================
    //  Override Methods
    //====================
    @Override
    public HEADER[] getTableHeaders() {
        return SORT_HEADERS;
    }

    @Override
    protected String setUp(Bundle savedInstanceState) {
        pageName = PAGE_NAME;
        return JbmnplsHttpClient.GET_LINKS.INTERVIEWS;
    }

    @Override
    protected void defineUI(Bundle savedInstanceState) {
        // Create the tutorial and set the content of this activity
        new TutorialHelper(this, R.layout.tabs,
                R.layout.tutorial_sorting, R.string.pref_seen_sorting_tutorial);

        super.defineUI(savedInstanceState);
        parser.setOnTableRowParse(this);
        createTab(TABS.COMING_UP);
        createTab(TABS.FINISHED);
    }

    @Override
    protected ArrayAdapter<Job> makeAdapterFromList(ArrayList<Job> list) {
        return new InterviewsAdapter(this, R.layout.interview_widget, WIDGET_RESOURCE_LIST, list);
    }

    public void onRowParse(TableParserOutline outline, Object... jobData) {
        Job job = parseRowTableOutline(outline, jobData);
        if (job.pastNow()) {
            addJobToListByTabId(TABS.FINISHED, job);
        } else {
            addJobToListByTabId(TABS.COMING_UP, job);
        }
        addJob(job);
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        int jobId = getCurrentList().get(arg2).getId();
        goToDescription(jobId);
    }

    @Override
    protected void parseWebpage(String html) {
        clearAllLists();
        parser.execute(INTERVIEWS_OUTLINE, html);
        parser.execute(GROUPS_OUTLINE, html);
        parser.execute(SPECIAL_OUTLINE, html);
        parser.execute(CANCELLED_OUTLINE, html);

        // Sort results by date
        isdComparer.shouldDescend(false);
        Collections.sort(getListByTab(TABS.COMING_UP), isdComparer);
        isdComparer.shouldDescend(true);
        Collections.sort(getListByTab(TABS.FINISHED), isdComparer);
    }

    //===================
    //  Private Methods
    //===================
    private static Date getDateFromDateWithTimeString(Date date, String timeString,
            int minutesOffset) throws JbmnplsParsingException{
        Date retDate = (Date)date.clone();
        Date timeDate;
        if (timeString.equals("")) {
            return date;
        }
        try {
            timeDate = TIME_FORMAT.parse(timeString);
        } catch (ParseException e) {
            e.printStackTrace();
            BugSenseHandler.sendExceptionMessage("Cannot convert string, given: ", timeString, e);
            return date;
        }
        retDate.setHours(timeDate.getHours());
        retDate.setMinutes(timeDate.getMinutes() + minutesOffset);
        return retDate;
    }

    //=================
    //  List Adapter
    //=================
    private class InterviewsAdapter extends JbmnplsAdapterBase {
        public InterviewsAdapter(Activity a, int widgetResourceLayout,
                int[] viewResourceIdListInWidget, ArrayList<Job> list) {
            super(a, widgetResourceLayout, viewResourceIdListInWidget, list);
        }

        @Override
        protected HIGHLIGHTING setJobWidgetValues(Job job, View[] elements, View layout) {
            setText(0, job.getTitle());
            setText(1, job.getEmployer(), true);
            setText(2, job.getInterviewer(), "With <b>");
            setDate(3, job.getInterviewStartTime(), "On <b>");
            setDate(4, job.getInterviewStartTime(), job.getInterviewEndTime(), "At <b>", TIME_FORMAT);
            setText(5, job.getRoomInfo(), "At <b>");
            if (job.getInterviewType() != null) {
                setText(6, job.getInterviewType().toString(), true);
            } else {
                hide(6);
            }
            setText(7, job.getInstructions());
            return HIGHLIGHTING.NORMAL;
        }
    }
}
