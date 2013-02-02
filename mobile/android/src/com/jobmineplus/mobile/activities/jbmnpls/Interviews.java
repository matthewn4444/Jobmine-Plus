package com.jobmineplus.mobile.activities.jbmnpls;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.ViewAdapterBase;
import com.jobmineplus.mobile.widgets.table.ColumnInfo;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;

public class Interviews extends JbmnplsListActivityBase implements TableParser.OnTableParseListener {

    //======================
    //  Declaration Objects
    //======================
    public final static String PAGE_NAME = Interviews.class.getName();
    protected final static String DATE_FORMAT = "d MMM yyyy";
    protected final static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
    private final TableParser parser = new TableParser();

    //======================
    // Table Definitions
    //======================
    private final static ColumnInfo COLUMN_1_INFO_ID   = new ColumnInfo(1, ColumnInfo.ID);
    private final static ColumnInfo COLUMN_2_INFO_TEXT = new ColumnInfo(2, ColumnInfo.TEXT);
    private final static ColumnInfo COLUMN_3_INFO_TEXT = new ColumnInfo(3, ColumnInfo.TEXT);
    private final static ColumnInfo COLUMN_4_INFO_DATE = new ColumnInfo(4, ColumnInfo.DATE, DATE_FORMAT);
    private final static ColumnInfo COLUMN_7_INFO_DATE = new ColumnInfo(7, ColumnInfo.TEXT);

    // First Table
    public static final TableParserOutline INTERVIEWS_OUTLINE = new TableParserOutline(
            "UW_CO_STUD_INTV$scroll$0", 13,
            COLUMN_1_INFO_ID,                              // Job Id
            COLUMN_2_INFO_TEXT,                             // Employer
            COLUMN_3_INFO_TEXT,                             // Title
            COLUMN_4_INFO_DATE,                             // Date
            new ColumnInfo(5, ColumnInfo.INTERVIEW_TYPE),   // Type
            COLUMN_7_INFO_DATE,                             // Start time
            new ColumnInfo(8, ColumnInfo.NUMERIC),          // Length
            new ColumnInfo(9, ColumnInfo.TEXT),             // Room
            new ColumnInfo(10, ColumnInfo.TEXT),            // Instructions
            new ColumnInfo(11, ColumnInfo.TEXT));           // Interviewer

    // Second Table
    public static final TableParserOutline GROUPS_OUTLINE = new TableParserOutline(
            "UW_CO_GRP_STU_V$scroll$0", 9,
            COLUMN_1_INFO_ID,                              // Job Id
            COLUMN_2_INFO_TEXT,                             // Employer
            COLUMN_3_INFO_TEXT,                             // Title
            COLUMN_4_INFO_DATE,                             // Date
            new ColumnInfo(5, ColumnInfo.TEXT),             // Start time
            new ColumnInfo(6, ColumnInfo.TEXT),             // End time
            COLUMN_7_INFO_DATE,                             // Room
            new ColumnInfo(8, ColumnInfo.TEXT));            // Instruction

    // Third Table
    public static final TableParserOutline SPECIAL_OUTLINE = new TableParserOutline(
            "UW_CO_NSCHD_JOB$scroll$0", 5,
            COLUMN_1_INFO_ID,                               // Job Id
            COLUMN_2_INFO_TEXT,                             // Employer
            COLUMN_3_INFO_TEXT,                             // Title
            new ColumnInfo(4, ColumnInfo.TEXT));            // Instruction

    // Fourth Table
    public static final TableParserOutline CANCELLED_OUTLINE = new TableParserOutline(
            "UW_CO_SINT_CANC$scroll$0", 4,
            COLUMN_1_INFO_ID,                               // Job Id
            COLUMN_2_INFO_TEXT,                             // Employer
            COLUMN_3_INFO_TEXT);                            // Title

    //================================
    //  Widget Resource List Outline
    //================================
    protected final int[] WIDGET_RESOURCE_LIST = {
            R.id.job_title, R.id.job_employer, R.id.date, R.id.type, R.id.time,
            R.id.interviewer, R.id.room, R.id.instructions};


    //============================
    //  Static Public Methods
    //============================
    public static Job parseRowTableOutline(TableParserOutline outline, Object... jobData) {
        Job job = null;
        int id = (Integer) jobData[0];
        String employer = (String) jobData[1];
        String title = (String) jobData[2];

        if (outline.equals(SPECIAL_OUTLINE)) {
            job = new Job(id, employer, title, (String) jobData[3]);
        } else if (outline.equals(CANCELLED_OUTLINE)) {
            job = new Job(id, employer, title);
        } else {
            Date interviewDay = (Date) jobData[3];
            if (outline.equals(INTERVIEWS_OUTLINE)) {
                job = new Job(id, employer, title,
                        getDateFromDateWithTimeString(interviewDay, (String) jobData[5], 0),
                        getDateFromDateWithTimeString(interviewDay, (String) jobData[5], (Integer) jobData[6]),
                        (Job.INTERVIEW_TYPE) jobData[4], (String) jobData[7], (String) jobData[8],
                        (String) jobData[9]);
            } else {    //GROUPS_OUTLINE
                job = new Job(id, employer, title,
                        getDateFromDateWithTimeString(interviewDay, (String) jobData[4], 0),
                        getDateFromDateWithTimeString(interviewDay, (String) jobData[5], 0),
                        (String) jobData[6], (String) jobData[7]);
            }
        }
        return job;
    }


    //====================
    //  Override Methods
    //====================
    @Override
    protected String setUp(Bundle savedInstanceState) {
        setContentView(R.layout.interviews);
        pageName = PAGE_NAME;
        return JbmnplsHttpClient.GET_LINKS.INTERVIEWS;
    }

    @Override
    protected void defineUI(Bundle savedInstanceState) {
        super.defineUI(savedInstanceState);
        parser.setOnTableRowParse(this);
        setAdapter(new InterviewsAdapter(this, android.R.id.list,
                R.layout.interview_widget, WIDGET_RESOURCE_LIST, getList()));
    }

    public void onRowParse(TableParserOutline outline, Object... jobData) {
        Job job = parseRowTableOutline(outline, jobData);
        addJob(job);
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        int jobId = getList().get(arg2).getId();
        pageName = "Interviews";
        goToDescription(jobId);
    }

    @Override
    protected void parseWebpage(String html) {
        clearList();
        parser.execute(INTERVIEWS_OUTLINE, html);
        parser.execute(GROUPS_OUTLINE, html);
        parser.execute(SPECIAL_OUTLINE, html);
        parser.execute(CANCELLED_OUTLINE, html);
    }

    //===================
    //  Private Methods
    //===================
    private static Date getDateFromDateWithTimeString(Date date, String timeString,
            int minutesOffset) throws JbmnplsParsingException{
        Date retDate = (Date)date.clone();
        Date timeDate;
        try {
            timeDate = TIME_FORMAT.parse(timeString);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new JbmnplsParsingException("Couldn't convert time from interviews page");
        }
        retDate.setHours(timeDate.getHours());
        retDate.setMinutes(timeDate.getMinutes() + minutesOffset);
        return retDate;
    }

    //=================
    //  List Adapter
    //=================
    private class InterviewsAdapter extends ViewAdapterBase<Job> {
        public InterviewsAdapter(Activity a, int listViewResourceId,
                int widgetResourceLayout, int[] viewResourceIdListInWidget,
                ArrayList<Job> list) {
            super(a, listViewResourceId, widgetResourceLayout, viewResourceIdListInWidget,
                    list);
        }

        @Override
        protected void setWidgetValues(Job job, View[] elements) {
            if (job != null) {
                Date start = job.getInterviewStartTime();
                Date end = job.getInterviewEndTime();
                Job.INTERVIEW_TYPE type = job.getInterviewType();
                String roominfo = job.getRoomInfo();
                String instructions = job.getInstructions();
                String interviewer = job.getInterviewer();

                ((TextView) elements[0]).setText(job.getTitle());
                ((TextView) elements[1]).setText(job.getEmployer());
                if (start != null) {
                    ((TextView) elements[2]).setText(DISPLAY_DATE_FORMAT.format(start));
                    if (end != null) {
                        ((TextView) elements[4]).setText(TIME_FORMAT.format(start) + " - " + TIME_FORMAT.format(end));
                    } else {
                        ((TextView) elements[4]).setVisibility(View.GONE);
                    }
                } else {
                    ((TextView) elements[2]).setVisibility(View.GONE);
                    ((TextView) elements[4]).setVisibility(View.GONE);
                }
                if (type != null) {
                    ((TextView) elements[3]).setText(type.toString());
                } else {
                    ((TextView) elements[3]).setVisibility(View.GONE);
                }
                if (interviewer != null) {
                    ((TextView) elements[5]).setText(interviewer);
                } else {
                    ((TextView) elements[5]).setVisibility(View.GONE);
                }
                if (roominfo != null) {
                    ((TextView) elements[6]).setText(roominfo);
                } else {
                    ((TextView) elements[6]).setVisibility(View.GONE);
                }
                if (instructions != null && instructions.length() > 0) {
                    ((TextView) elements[7]).setText(instructions);
                } else {
                    ((TextView) elements[7]).setVisibility(View.GONE);
                }
            }
        }
    }


}
