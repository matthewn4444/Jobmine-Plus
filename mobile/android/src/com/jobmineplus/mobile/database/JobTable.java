package com.jobmineplus.mobile.database;

import java.util.Date;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class JobTable {
    
    /*
     *  Database table
     */
    // Basic Job attributes
    public static final String TAG = JobTable.class.getName();
    public static final String TABLE_JOB = "job";
    
    public static final String COLUMN_ID = "_id";    
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_EMPLOYER = "employer";
    
    // Applications attributes
    public static final String COLUMN_TERM = "term";
    public static final String COLUMN_STATE = "state";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_LAST_DATE_APPLY = "lastToApply";
    public static final String COLUMN_NUM_APPS = "numApps";
    public static final String COLUMN_OPENINGS = "openings";
    
    // Job Description
    public static final String COLUMN_OPEN_DATE_APPLY = "openToApply";
    public static final String COLUMN_EMPLOYER_FULL = "employerFull";
    public static final String COLUMN_GRADE_REQUIRED = "gradeRequired";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_DISCIPLINES = "disciplines";
    public static final String COLUMN_LEVELS = "levels";
    public static final String COLUMN_HIRING_SUPPORT = "hiringSupport";
    public static final String COLUMN_WORK_SUPPORT = "workSupport";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_DESCRIPTION_WARNING = "warning";
    
    // Interview Data
    public static final String COLUMN_INTERVIEW_START_TIME = "startTime";
    public static final String COLUMN_INTERVIEW_END_TIME = "endTime";
    public static final String COLUMN_INTERVIEW_TYPE = "interviewType";
    public static final String COLUMN_INTERVIEW_ROOM = "interviewRoom";
    public static final String COLUMN_INTERVIEW_INSTRUCTIONS = "instructions";
    public static final String COLUMN_INTERVIEWER = "interviewer";
    
    // Database creation SQL statement
    // Dates are integers since we cannot properly store dates
    private static final String DATABASE_CREATE = "create table "
            + TABLE_JOB
            + "("
            + COLUMN_ID + " integer primary key, "    // Jobmine uses their own ids
            + COLUMN_TITLE + " text not null, "
            + COLUMN_EMPLOYER + " text not null, "
            + COLUMN_TERM + " text, "
            + COLUMN_STATE + " text, "
            + COLUMN_STATUS + " text, "
            + COLUMN_LAST_DATE_APPLY + " integer, " 
            + COLUMN_NUM_APPS + " integer default 0, "
            + COLUMN_OPENINGS + " integer default 0, "
            + COLUMN_OPEN_DATE_APPLY + " integer, "
            + COLUMN_EMPLOYER_FULL + " text, "
            + COLUMN_GRADE_REQUIRED + " integer default 0, "
            + COLUMN_LOCATION + " text, "
            + COLUMN_DISCIPLINES + " text, "
            + COLUMN_LEVELS + " text, "
            + COLUMN_HIRING_SUPPORT + " text, " 
            + COLUMN_WORK_SUPPORT + " text, " 
            + COLUMN_DESCRIPTION + " text, "
            + COLUMN_DESCRIPTION_WARNING + " text, "
            + COLUMN_INTERVIEW_START_TIME + " integer, "
            + COLUMN_INTERVIEW_END_TIME + " integer, " 
            + COLUMN_INTERVIEW_TYPE + " text, "
            + COLUMN_INTERVIEW_ROOM + " text, "
            + COLUMN_INTERVIEW_INSTRUCTIONS + " text, "
            + COLUMN_INTERVIEWER + " text "
            + ");";
    
    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }
    
    public static void dropTable(SQLiteDatabase database) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_JOB);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion, 
            int newVersion) {
        Log.w(TAG, "Updating database from version" + oldVersion 
                + " to " + newVersion 
                + ", which will destroy all old data");
        dropTable(database);
        onCreate(database);
    }
}
