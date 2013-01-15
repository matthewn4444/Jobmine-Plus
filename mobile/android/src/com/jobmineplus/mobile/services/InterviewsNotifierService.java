package com.jobmineplus.mobile.services;

import java.io.IOException;
import java.util.ArrayList;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.jbmnpls.Interviews;
import com.jobmineplus.mobile.database.pages.PageDataSource;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;

public class InterviewsNotifierService extends Service {
    private final PageDataSource pageSource = new PageDataSource(this);
    private final JbmnplsHttpService service = JbmnplsHttpService.getInstance();

    private final int INTERVIEW_NOTIFICATION_ID = 1;

    private Notification notification;

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

    private void showNotification(String title, String content) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notification == null) {
            notification = new Notification(R.drawable.ic_launcher,
                    getString(R.string.interviews_ticket_text), System.currentTimeMillis());
        } else {
            notification.when = System.currentTimeMillis();
        }
        Intent resultIntent = new Intent(this, Interviews.class);
        PendingIntent pin = PendingIntent.getActivity(this, 0, resultIntent, 0);
        notification.setLatestEventInfo(this, title, content, pin);
        mNotificationManager.notify(INTERVIEW_NOTIFICATION_ID, notification);
        log("Sent");
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
                html = service.getJobmineHtml("http://10.0.2.2/test/Interviews.html");        // Fix for debugging
//                html = service.getJobmineHtml(JbmnplsHttpService.GET_LINKS.INTERVIEWS);        // Fix for debugging
            } catch (JbmnplsLoggedOutException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            // Parse the html into jobs (except the
            try {
                parser.execute(Interviews.INTERVIEWS_OUTLINE, html);
                parser.execute(Interviews.GROUPS_OUTLINE, html);
                parser.execute(Interviews.SPECIAL_OUTLINE, html);
            } catch (JbmnplsParsingException e) {
                e.printStackTrace();
                return false;
            }

            // Parse out which are the new interviews
            if (pulledJobs.isEmpty()) {
                return true;
            }

            // Parse the new interviews; remove all jobs that are already existing
            int newCount = 0;
            if (ids != null) {
                for (int i = 0; i < pulledJobs.size(); i++) {
                    if (!ids.contains(pulledJobs.get(i).getId())) {
                        newCount++;
                    }
                }
            } else {
                // No previous jobs
                newCount = pulledJobs.size();
            }

            // No new jobs
            if (newCount == 0) {
                return true;
            }
            String message = newCount + " new interview"
                    + (newCount==1?"":"s");
            showNotification("Jobmine Plus", message);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean shouldScheduleAlarm) {
            pageSource.close();
            log("finished grabbing");
            if (shouldScheduleAlarm && nextTimeout != 0) {
                scheduleNextAlarm(nextTimeout);   // TODO should enable when not testing
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
