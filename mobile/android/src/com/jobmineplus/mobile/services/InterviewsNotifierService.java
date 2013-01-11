package com.jobmineplus.mobile.services;

import java.io.IOException;
import java.util.ArrayList;
import com.jobmineplus.mobile.activities.jbmnpls.Interviews;
import com.jobmineplus.mobile.database.pages.PageDataSource;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;

public class InterviewsNotifierService extends Service {
    private final PageDataSource pageSource = new PageDataSource(this);
    private final JbmnplsHttpService service = JbmnplsHttpService.getInstance();


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        GetInterviewsTask task = new GetInterviewsTask();
        task.execute(intent.getIntExtra(InterviewsAlarm.BUNDLE_TIMEOUT, 0));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void scheduleNextAlarm(int timeoutSeconds) {
        // Bundle the next time inside the intent
        long triggerTime = System.currentTimeMillis() + timeoutSeconds * 1000;
        Bundle bundle = new Bundle();
        Intent in = new Intent(this, InterviewsAlarm.class);
        bundle.putInt(InterviewsAlarm.BUNDLE_TIMEOUT, timeoutSeconds);
        in.putExtra(InterviewsAlarm.BUNDLE_NAME, bundle);

        // Start the next alarm
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, in, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi);
    }

    private class GetInterviewsTask extends AsyncTask<Integer, Void, Boolean>
        implements TableParser.OnTableParseListener {
        private final TableParser parser = new TableParser();
        private ArrayList<Job> pulledJobs;
        private int nextTimeout = 0;

        public GetInterviewsTask() {
            parser.setOnTableRowParse(this);
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            nextTimeout = params[0];
            pageSource.open();
            pulledJobs = new ArrayList<Job>();
            ArrayList<Job> newInterviews = new ArrayList<Job>();

            // Get interviews data from the database
            ArrayList<Integer> ids = pageSource.getJobsIds(Interviews.PAGE_NAME);

            // Pull the interview data off the website
            String html;
            try {
//                html = service.getJobmineHtml(DebugInterviews.FAKE_INTERVIEWS);        // Fix for debugging
                html = service.getJobmineHtml("http://10.0.2.2:20840/test");        // Fix for debugging
//                html = service.getJobmineHtml(JbmnplsHttpService.GET_LINKS.INTERVIEWS);        // Fix for debugging
            } catch (JbmnplsLoggedOutException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            // Parse the html into jobs
            try {
                parser.execute(Interviews.INTERVIEWS_OUTLINE, html);
                parser.execute(Interviews.GROUPS_OUTLINE, html);
                parser.execute(Interviews.SPECIAL_OUTLINE, html);
                parser.execute(Interviews.CANCELLED_OUTLINE, html);
            } catch (JbmnplsParsingException e) {
                e.printStackTrace();
                return false;
            }
            long timestamp = System.currentTimeMillis();

            // Parse out which are the new interviews
            if (ids != null) {
                // Both empty
                if (pulledJobs.isEmpty() && ids.isEmpty()) {
                    return true;
                }

                // Parse the new interviews; remove all jobs that are already existing
                for (int i = 0; i < pulledJobs.size(); i++) {
                    if (!ids.contains(pulledJobs.get(i).getId())) {
                        newInterviews.add(pulledJobs.get(i));
                    }
                }

                log(newInterviews.size() + " of new items");

                // Same jobs as last time
                if (newInterviews.isEmpty()) {
                    return true;
                }

                // TODO throw notification
            }
            pageSource.addPage(Interviews.PAGE_NAME, pulledJobs, timestamp);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean shouldScheduleAlarm) {
            pageSource.close();
            if (shouldScheduleAlarm && nextTimeout != 0) {
//                scheduleNextAlarm(nextTimeout);   // TODO should enable when not testing
            }
            super.onPostExecute(shouldScheduleAlarm);
        }

        public void onRowParse(TableParserOutline outline, Object... jobData) {
            Job job = Interviews.parseRowTableOutline(outline, jobData);
            pulledJobs.add(job);
        }
    }

    protected void log(Object... txt) {
        String returnStr = "";
        int i = 1;
        int size = txt.length;
        if (size != 0) {
            returnStr = txt[0] == null ? "null" : txt[0].toString();
            for (; i < size; i++) {
                returnStr += ", "
                        + (txt[i] == null ? "null" : txt[i].toString());
            }
        }
        System.out.println(returnStr);
    }
}
