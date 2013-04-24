package com.jobmineplus.mobile.database.jobs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import com.jobmineplus.mobile.database.DataSourceBase;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.Job.APPLY_STATUS;
import com.jobmineplus.mobile.widgets.Job.STATE;
import com.jobmineplus.mobile.widgets.Job.STATUS;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Pair;
import android.util.SparseArray;

public final class JobDataSource extends DataSourceBase {

    // Database fields
    private JobDatabaseHelper dbHelper;

    private final String[] allColumns = {
            JobTable.COLUMN_ID,
            JobTable.COLUMN_TITLE,
            JobTable.COLUMN_EMPLOYER,
            JobTable.COLUMN_TERM,
            JobTable.COLUMN_STATE,
            JobTable.COLUMN_STATUS,
            JobTable.COLUMN_APP_STATUS,
            JobTable.COLUMN_LAST_DATE_APPLY,
            JobTable.COLUMN_NUM_APPS,
            JobTable.COLUMN_OPENINGS,
            JobTable.COLUMN_OPEN_DATE_APPLY,
            JobTable.COLUMN_EMPLOYER_FULL,
            JobTable.COLUMN_GRADE_REQUIRED,
            JobTable.COLUMN_LOCATION,
            JobTable.COLUMN_DISCIPLINES,
            JobTable.COLUMN_LEVELS,
            JobTable.COLUMN_HIRING_SUPPORT,
            JobTable.COLUMN_WORK_SUPPORT,
            JobTable.COLUMN_DESCRIPTION,
            JobTable.COLUMN_DESCRIPTION_WARNING,
            JobTable.COLUMN_INTERVIEW_START_TIME,
            JobTable.COLUMN_INTERVIEW_END_TIME,
            JobTable.COLUMN_INTERVIEW_TYPE,
            JobTable.COLUMN_INTERVIEW_ROOM,
            JobTable.COLUMN_INTERVIEW_INSTRUCTIONS,
            JobTable.COLUMN_INTERVIEWER
    };

    public JobDataSource(Context context) {
        dbHelper = new JobDatabaseHelper(context);
    }

    @Override
    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    @Override
    public void close() {
        dbHelper.close();
    }

    // =================
    //  Additions
    // =================
    public synchronized void addJob(Job job) {
        internalAddJob(job);
    }

    public synchronized void addJobs(ArrayList<Job> jobs) {
        if (!jobs.isEmpty()) {
            try {
                database.setLockingEnabled(false);
                database.beginTransaction();
                for (Job job : jobs) {
                    internalAddJob(job);
                }
                database.setTransactionSuccessful();
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                database.endTransaction();
                database.setLockingEnabled(true);
            }
        }
    }

    // =================
    //  Accessors
    // =================

    public Job getJob(int id) {
        Cursor cursor = getCursorByJobId(id);
        if (cursor == null) {
            return null;
        }

        Job job = cursorToJob(cursor);
        cursor.close();
        return job;
    }

    public ArrayList<Job> getJobsByIdList(String idList) {
        return cursorToJobListAndClose(getCursorJobsByIdList(idList));
    }

    public ArrayList<Job> getJobsByIdList(Iterable<Integer> ids) {
        // Join the ids
        String idList = "";
        for (int id : ids) {
            idList += id + ",";
        }
        idList = idList.substring(0, idList.length() - 1);

        return getJobsByIdList(idList);
    }

    /**
     * This does its own get job map from id map. The thing is we need to do one database transaction
     * instead of one for each tab. Therefore we build our sql string, execute it and then add the jobs
     * into a hashmap. Because the tabs have a mix of jobs, it would be inefficient to get and create the
     * same job multiple time, we do it once here for the expensive of slightly more loops. Loops should be
     * faster than more than one transaction.
     *
     * @param idMap
     * @return
     */
    public HashMap<String, ArrayList<Job>> getJobsMap(HashMap<String, ArrayList<Integer>> idMap) {
        HashMap<String, ArrayList<Job>> jobTabs = new HashMap<String, ArrayList<Job>>();

        // Build the ids String to get joblist
        SparseArray<Job> jobs = new SparseArray<Job>();
        StringBuilder sb = new StringBuilder();
        for (String tag : idMap.keySet()) {
           ArrayList<Integer> ids = idMap.get(tag);
            if (ids != null && !ids.isEmpty()) {
                for (int id : ids) {
                    if (jobs.indexOfKey(id) < 0) {
                        jobs.put(id, null);
                        sb.append(id).append(',');
                    }
                }
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        sb.deleteCharAt(sb.length() - 1);       // Remove last comma

        // Get all the jobs
        Cursor cursor = getCursorJobsByIdList(sb.toString());
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast() == false) {
                Job job = cursorToJob(cursor);
                jobs.put(job.getId(), job);
                cursor.moveToNext();
            }
        }
        cursor.close();

        // Finally build the map
        for (String tag : idMap.keySet()) {
            ArrayList<Job> list = new ArrayList<Job>();
            ArrayList<Integer> ids = idMap.get(tag);
            if (ids != null && !ids.isEmpty()) {
                for (int id : ids) {
                    list.add(jobs.get(id));
                }
            }
            jobTabs.put(tag, list);
        }

        return jobTabs;
    }

    public ArrayList<Job> getAllJobs() {
        Cursor cursor;
        synchronized (this) {
            cursor = database.rawQuery("select * from " + JobTable.TABLE_JOB, null);
        }
        return cursorToJobListAndClose(cursor);
    }

    // =================
    //  Deletions
    // =================
    public synchronized void deleteJob(int id) {
        database.delete(JobTable.TABLE_JOB, JobTable.COLUMN_ID + "=?", new String[] { id + "" });
    }

    public synchronized void deleteJob(Job job) {
        deleteJob(job.getId());
    }

    // =================
    //  Private
    // =================

    private Cursor getCursorByJobId(int id) {
        Cursor cursor = null;
        synchronized (this) {
            cursor = database.query(JobTable.TABLE_JOB,
                    allColumns, JobTable.COLUMN_ID + " = " + id, null,
                    null, null, null);
        }
        if (cursor != null) {
            cursor.moveToFirst();
        }

        // No job available
        if (cursor.isAfterLast()) {
            return null;
        }
        return cursor;
    }

    private synchronized Cursor getCursorJobsByIdList(String idList) {
        // Do query
        return database.rawQuery(String.format("select * from %s where %s in (%s)", JobTable.TABLE_JOB, JobTable.COLUMN_ID, idList), null);
    }

    private void internalAddJob(Job job) {
        int jobId = job.getId();
        ContentValues values = new ContentValues();

        Date lastDateToApply = job.getLastDateToApply();
        long lastDateTimestamp = lastDateToApply == null ? 0 : lastDateToApply.getTime();
        Date openDateToApply = job.getOpenDateToApply();
        long openDateTimestamp = openDateToApply == null ? 0 : openDateToApply.getTime();
        Date interviewStart = job.getInterviewStartTime();
        long interviewStartTimestamp = interviewStart == null ? 0 : interviewStart.getTime();
        Date interviewEnd = job.getInterviewEndTime();
        long interviewEndTimestamp = interviewEnd == null ? 0 : interviewEnd.getTime();

        Job.INTERVIEW_TYPE type = job.getInterviewType();

        // Add Date to the columns
        values.put(JobTable.COLUMN_ID, jobId);
        values.put(JobTable.COLUMN_TITLE, job.getTitle());
        values.put(JobTable.COLUMN_EMPLOYER, job.getEmployer());
        addNonNullValue(values, JobTable.COLUMN_TERM, job.getTerm());

//        addNonNullValue(values, JobTable.COLUMN_STATE, job.getState().toString());
//        addNonNullValue(values, JobTable.COLUMN_STATUS, job.getStatus().toString());
//        addNonNullValue(values, JobTable.COLUMN_APP_STATUS, job.getApplicationStatus().toString());

        addNonNullValue(values, JobTable.COLUMN_LAST_DATE_APPLY, lastDateTimestamp);
        addNonNullValue(values, JobTable.COLUMN_NUM_APPS, job.getNumberOfApplications());
        addNonNullValue(values, JobTable.COLUMN_OPENINGS, job.getNumberOfOpenings());
        addNonNullValue(values, JobTable.COLUMN_OPEN_DATE_APPLY, openDateTimestamp);
        addNonNullValue(values, JobTable.COLUMN_EMPLOYER_FULL, job.getEmployerFullName());
        addNonNullValue(values, JobTable.COLUMN_GRADE_REQUIRED, (job.areGradesRequired() ? 1 : 0));
        addNonNullValue(values, JobTable.COLUMN_LOCATION, job.getLocation());
        addNonNullValue(values, JobTable.COLUMN_DISCIPLINES, job.getDisciplinesAsString());
        addNonNullValue(values, JobTable.COLUMN_LEVELS, job.getLevelsAsString());
        addNonNullValue(values, JobTable.COLUMN_HIRING_SUPPORT, job.getHiringSupportName());
        addNonNullValue(values, JobTable.COLUMN_WORK_SUPPORT, job.getWorkSupportName());
        addNonNullValue(values, JobTable.COLUMN_DESCRIPTION, job.getDescription());
        addNonNullValue(values, JobTable.COLUMN_DESCRIPTION_WARNING, job.getDescriptionWarning());

        // Interview Data
        addNonNullValue(values, JobTable.COLUMN_INTERVIEW_START_TIME, interviewStartTimestamp);
        addNonNullValue(values, JobTable.COLUMN_INTERVIEW_END_TIME, interviewEndTimestamp);
        addNonNullValue(values, JobTable.COLUMN_INTERVIEW_TYPE, type == null ? null : type.toString());
        addNonNullValue(values, JobTable.COLUMN_INTERVIEW_ROOM, job.getRoomInfo());
        addNonNullValue(values, JobTable.COLUMN_INTERVIEW_INSTRUCTIONS, job.getInstructions());
        addNonNullValue(values, JobTable.COLUMN_INTERVIEWER, job.getInterviewer());

        // Check the status values
        STATE state = job.getState();
        STATUS status = job.getStatus();
        APPLY_STATUS appStatus = job.getApplicationStatus();
        if (state != STATE.getDefault()) {
            values.put( JobTable.COLUMN_STATE, state.toString());
        }
        if (status != STATUS.getDefault()) {
            values.put( JobTable.COLUMN_STATUS, status.toString());
        }
        if (appStatus != APPLY_STATUS.getDefault()) {
            values.put( JobTable.COLUMN_APP_STATUS, appStatus.toString());
        }

        ArrayList<Pair<String, Object>> where = new ArrayList<Pair<String,Object>>();
        where.add(new Pair<String, Object>("_id", jobId));
        updateElseInsert(JobTable.TABLE_JOB, where, values);
    }

    private ArrayList<Job> cursorToJobListAndClose(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        ArrayList<Job> jobs = new ArrayList<Job>(cursor.getCount());
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast() == false) {
                jobs.add(cursorToJob(cursor));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return jobs;
    }

    private Job cursorToJob(Cursor cursor) {
        return new Job(
                cursor.getInt(0),       // Id
                cursor.getString(1),    // Title
                cursor.getString(2),    // Employer
                cursor.getString(3),    // Term
                cursor.getString(4),    // State
                cursor.getString(5),    // Status
                cursor.getString(6),    // Application Status
                cursor.getLong(7),      // Last date apply
                cursor.getInt(8),       // Number of apps
                cursor.getInt(9),       // Openings
                cursor.getLong(10),      // Open date to apply
                cursor.getString(11),   // Employer fullname
                cursor.getInt(12),      // Grades required
                cursor.getString(13),   // Location
                cursor.getString(14),   // Disciplines
                cursor.getString(15),   // Levels
                cursor.getString(16),   // Hiring support
                cursor.getString(17),   // Work support
                cursor.getString(18),   // Description
                cursor.getString(19),   // Description warning

                // Interview data
                cursor.getLong(20),     // Interview start time
                cursor.getLong(21),     // Interview end time
                cursor.getString(22),   // Interview type
                cursor.getString(23),   // Interview room
                cursor.getString(24),   // Interview instructions
                cursor.getString(25)    // Interviewer
        );
    }

}
