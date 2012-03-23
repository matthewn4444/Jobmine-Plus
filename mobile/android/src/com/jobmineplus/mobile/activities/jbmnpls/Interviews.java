package com.jobmineplus.mobile.activities.jbmnpls;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.jsoup.nodes.Document;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.widgets.InterviewData;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.ViewAdapterBase;

public class Interviews extends JbmnplsListActivityBase {

    // ======================
    // Declaration Objects
    // ======================
    protected final String DATE_FORMAT = "d MMM yyyy";
    protected final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.ENGLISH);

    // ======================
    // Table Definitions
    // ======================
    private final ColumnInfo COLUMN_2_INFO_TEXT = new ColumnInfo(2, ColumnInfo.TEXT);
    private final ColumnInfo COLUMN_3_INFO_TEXT = new ColumnInfo(3, ColumnInfo.TEXT);
    private final ColumnInfo COLUMN_4_INFO_DATE = new ColumnInfo(4, ColumnInfo.DATE, DATE_FORMAT);
    private final ColumnInfo COLUMN_7_INFO_DATE = new ColumnInfo(7, ColumnInfo.TEXT);

    // First Table
    protected final TableParsingOutline INTERVIEWS_OUTLINE = new TableParsingOutline(
            "UW_CO_STUD_INTV$scroll$0", 13, 1, 
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
    protected final TableParsingOutline GROUPS_OUTLINE = new TableParsingOutline(
            "UW_CO_GRP_STU_V$scroll$0", 9, 1,
            COLUMN_2_INFO_TEXT,                             // Employer
            COLUMN_3_INFO_TEXT,                             // Title
            COLUMN_4_INFO_DATE,                             // Date
            new ColumnInfo(5, ColumnInfo.TEXT),             // Start time
            new ColumnInfo(6, ColumnInfo.TEXT),             // End time
            COLUMN_7_INFO_DATE,                             // Room
            new ColumnInfo(8, ColumnInfo.TEXT));            // Instruction

    // Third Table
    protected final TableParsingOutline SPECIAL_OUTLINE = new TableParsingOutline(
            "UW_CO_NSCHD_JOB$scroll$0", 5, 1,
            COLUMN_2_INFO_TEXT,                             // Employer
            COLUMN_3_INFO_TEXT,                             // Title
            new ColumnInfo(4, ColumnInfo.TEXT));            // Instruction

    // Fourth Table
    protected final TableParsingOutline CANCELLED_OUTLINE = new TableParsingOutline(
            "UW_CO_SINT_CANC$scroll$0", 4, 1,
            COLUMN_2_INFO_TEXT,                             // Employer
            COLUMN_3_INFO_TEXT);                            // Title
    
    //================================
    //  Widget Resource List Outline
    //================================
    protected final int[] WIDGET_RESOURCE_LIST = { 
            R.id.job_title, R.id.job_employer, R.id.date, R.id.type, R.id.time,
            R.id.interviewer, R.id.room, R.id.instructions};
    
    //====================
    //  Override Methods
    //====================
    @Override
    protected void defineUI(Bundle savedInstanceState) {
        super.defineUI(savedInstanceState);
        setAdapter(new InterviewsAdapter(this, android.R.id.list, 
                R.layout.interview_widget, WIDGET_RESOURCE_LIST, getList()));
    }
    
    @Override
    protected void onRowParse(TableParsingOutline outline, Object... jobData) {
        InterviewData data = null;
        int id = (Integer) jobData[0];
        String employer = (String) jobData[1];
        String title = (String) jobData[2];
        
        if (outline.equals(SPECIAL_OUTLINE)) {
            data = new InterviewData(id, employer, title, (String) jobData[3]);
        } else if (outline.equals(CANCELLED_OUTLINE)) {
            data = new InterviewData(id, employer, title);
        } else {
            Date interviewDay = (Date) jobData[3];
            if (outline.equals(INTERVIEWS_OUTLINE)) {
                data = new InterviewData(id, employer, title, 
                        getDateFromDateWithTimeString(interviewDay, (String) jobData[5], 0), 
                        getDateFromDateWithTimeString(interviewDay, (String) jobData[5], (Integer) jobData[6]), 
                        (InterviewData.TYPE) jobData[4], (String) jobData[7], (String) jobData[8], 
                        (String) jobData[9]);
            } else {    //GROUPS_OUTLINE
                data = new InterviewData(id, employer, title, 
                        getDateFromDateWithTimeString(interviewDay, (String) jobData[4], 0),  
                        getDateFromDateWithTimeString(interviewDay, (String) jobData[5], 0),  
                        (String) jobData[6], (String) jobData[7]);
            }
        }
        addInterview(data);
    }
    
    protected void addInterview(InterviewData data) {
        app.addInterview(data);
        super.addJob( new Job(data.getJobId(), data.getTitle(), data.getEmployer()) );
    } 
    
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        int jobId = getList().get(arg2);
        goToDescription(jobId);
    }

    @Override
    protected String setUp(Bundle savedInstanceState) {
        setContentView(R.layout.interviews);
        return JbmnplsHttpService.GET_LINKS.INTERVIEWS;
    }

    @Override
    protected void parseWebpage(Document doc) {
        clearList();
        INTERVIEWS_OUTLINE.execute(doc);
        GROUPS_OUTLINE.execute(doc);
        SPECIAL_OUTLINE.execute(doc);
        CANCELLED_OUTLINE.execute(doc);
    }
    
    //===================
    //  Private Methods
    //===================
    private Date getDateFromDateWithTimeString(Date date, String timeString, 
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
    private class InterviewsAdapter extends ViewAdapterBase<Integer> {
        public InterviewsAdapter(Activity a, int listViewResourceId,
                int widgetResourceLayout, int[] viewResourceIdListInWidget,
                ArrayList<Integer> list) {
            super(a, listViewResourceId, widgetResourceLayout, viewResourceIdListInWidget,
                    list);
        }

        @Override
        protected void setWidgetValues(Integer id, View[] elements) {
            final InterviewData data = app.getInterviewData(id);
            if (data != null) {
                Date start = data.getStartTime();
                Date end = data.getEndTime();
                InterviewData.TYPE type = data.getType();
                String roominfo = data.getRoomInfo();
                String instructions = data.getInstructions();
                String interviewer = data.getInterviewer();
                ((TextView) elements[0]).setText(data.getTitle());
                ((TextView) elements[1]).setText(data.getEmployer());
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
