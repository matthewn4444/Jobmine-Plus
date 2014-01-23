package com.jobmineplus.mobile.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import javax.net.ssl.SSLException;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.bugsense.trace.BugSenseHandler;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.SimpleActivityBase;
import com.jobmineplus.mobile.activities.jbmnpls.Applications;
import com.jobmineplus.mobile.activities.jbmnpls.Interviews;
import com.jobmineplus.mobile.database.jobs.JobDataSource;
import com.jobmineplus.mobile.database.pages.PageDataSource;
import com.jobmineplus.mobile.database.pages.PageMapResult;
import com.jobmineplus.mobile.exceptions.JbmnplsException;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.JbmnplsHttpClient;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.table.TableParser;
import com.jobmineplus.mobile.widgets.table.TableParserOutline;

public class InterviewsNotifierService extends Service {
    private final PageDataSource pageSource = new PageDataSource(this);
    private final JobDataSource jobSource = new JobDataSource(this);
    private static final int CHECK_APPS_MAX_COUNT = 10;
    private JbmnplsHttpClient client;
    NotificationManager mNotificationManager;

    // Nofication values
    private final int INTERVIEW_NOTIFICATION_ID = 1;
    private Notification notification;

    // Time constants
    public final int CRAWL_APPLICATIONS_TIMEOUT = 3 * 60 * 60;  // 3 hours
    public final int NO_DATA_RESCHEDULE_TIME    = 5 * 60 * 60;  // 5 hours

    private int originalTimeout;

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        client = new JbmnplsHttpClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        GetInterviewsTask task = new GetInterviewsTask(this);

        if (intent == null) {
            return START_STICKY;
        }
        originalTimeout = intent.getIntExtra(InterviewsAlarm.BUNDLE_TIMEOUT, 0);

        // Handle logging in with username and password
        String username = intent.getStringExtra(InterviewsAlarm.BUNDLE_USERNAME);
        String password = intent.getStringExtra(InterviewsAlarm.BUNDLE_PASSWORD);
        if (username == null || password == null) {
            return START_STICKY;
        }

        // If the username or password has changed, we will make a new client
        String oldUsername = client.getUsername();
        String oldPassword = client.getPassword();
        if (!username.equals(oldUsername) || !password.equals(oldPassword)) {
            client = new JbmnplsHttpClient(username, password);
        }
        if (!client.verifyLogin()) {
            return START_STICKY;
        }
        task.execute(originalTimeout);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private long validateScheduleTime(long timestamp) {
        // This is the next scheduled time
        Calendar nextTime = Calendar.getInstance();
        nextTime.setTimeInMillis(timestamp);

        // Only apply next day if the time is during 12am->9am (offline times)
        int hour = nextTime.get(Calendar.HOUR_OF_DAY);
        if (hour < 9) {
            // Return tomorrow at 9am for the next schedule
            Calendar nextDay = Calendar.getInstance();
            nextDay.add(Calendar.DAY_OF_MONTH, 1);
            nextDay.set(Calendar.HOUR_OF_DAY, 9);
            nextDay.set(Calendar.MINUTE, 0);
            return nextDay.getTimeInMillis();
        }
        return timestamp;
    }

    private void scheduleNextAlarm(int timeoutSeconds) {
        // Bundle the next time inside the intent
        long now = System.currentTimeMillis();
        long triggerTime = validateScheduleTime(now + timeoutSeconds * 1000);

        // Pass back the original timeout
        Bundle bundle = new Bundle();
        Intent in = new Intent(this, InterviewsAlarm.class);
        bundle.putString(InterviewsAlarm.BUNDLE_USERNAME, client.getUsername());
        bundle.putString(InterviewsAlarm.BUNDLE_PASSWORD, client.getPassword());
        in.putExtra(InterviewsAlarm.BUNDLE_NAME, bundle);

        // Start the next alarm
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, in, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi);
    }

    private void showNotification(String title, String content) {
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
    }

    private class GetInterviewsTask extends AsyncTask<Integer, Void, Boolean>
        implements TableParser.OnTableParseListener {
        private final TableParser parser = new TableParser();
        private ArrayList<Job> pulledJobs;
        private HashMap<String, ArrayList<Job>> pulledInterviewsJobs;
        private HashMap<String, ArrayList<Job>> pulledAppsJobs;
        private int nextTimeout = 0;
        private final Context ctx;

        // Results from getting interviews
        private final int NO_SCHEDULE = 0;
        private final int DO_SCHEDULE = 1;
        private final int DO_SCHEDULE_NO_INTERVIEW = 2;

        public GetInterviewsTask(Context context) {
            ctx = context;
            parser.setOnTableRowParse(this);
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            if (!SimpleActivityBase.isJobmineOnline()) {
                return true;
            }

            nextTimeout = params[0];

            pageSource.open();
            jobSource.open();

            // Check connections
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mMobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
            boolean wifiOnly = preferences.getBoolean("settingsWifiCheckOnly", true);

            // TODO check this
            if (Interviews.isNetworkConnected() && (mWifi.isConnected() || mMobile.isConnected() && !wifiOnly)) {
                // Check the applications to then see if we need to crawl interviews
                int result = NO_SCHEDULE;
                try {
                    result = checkApplications();
                } catch (JbmnplsLoggedOutException e) {
                    BugSenseHandler.sendException(e);
                    e.printStackTrace();
                    return false;
                } catch (SSLException e) {      // Ignore sending this exception, it is not an error
                    e.printStackTrace();
                    return false;
                } catch (IOException e) {
                    BugSenseHandler.sendException(e);
                    e.printStackTrace();
                    return false;
                }

                // Parse the results
                if (result == NO_SCHEDULE) {
                    return false;
                } else {
                    if (result == DO_SCHEDULE) {
                        return crawlInterviews();
                    } else {    // Do not run interviews but will schedule
                        return true;
                    }
                }
            } else {
                // Try again later
                return true;
            }
        }

        private int checkApplications() throws JbmnplsLoggedOutException, IOException {
            return checkApplications(0);
        }

        private int checkApplications(int ranCount) throws JbmnplsLoggedOutException, IOException {
            // Avoid stackoverflow because of time calculation errors
            if (ranCount >= CHECK_APPS_MAX_COUNT) {
                return NO_SCHEDULE;
            }

            String username = client.getUsername();
            PageMapResult result = pageSource.getPageDataMap(username, Applications.PAGE_NAME);

            // If no results, get them then
            if (result == null) {
                crawlApplications();
                result = pageSource.getPageDataMap(username, Applications.PAGE_NAME);
                if (result == null) {
                    throw new JbmnplsException("Cannot grab any data from Applications.");
                }
            }

            // Get data from result
            ArrayList<Integer> activeList = result.idMap.get(Applications.LISTS.ACTIVE_JOBS);
            ArrayList<Integer> allList = result.idMap.get(Applications.LISTS.ALL_JOBS);
            long now = System.currentTimeMillis();
            double secDiff = (now - result.timestamp) / 1000;
            Boolean needToGetApps = secDiff > CRAWL_APPLICATIONS_TIMEOUT;

            if (needToGetApps) {
                crawlApplications();
                return checkApplications(ranCount + 1);
            }

            // When active is empty, we do not need to get interviews
            if (activeList == null) {
                // Both lists are empty so we don't do anything or schedule anything
                if (allList == null) {
                    return NO_SCHEDULE;
                } else {
                    // No active apps and now we reschedule at a later time
                    nextTimeout = NO_DATA_RESCHEDULE_TIME;
                    return DO_SCHEDULE_NO_INTERVIEW;
                }
            } else {
                // Check to see if you are employed by chacking the all list for employed
                ArrayList<Job> jobs = jobSource.getJobsByIdList(allList);
                for (Job j : jobs) {
                    if (j.getStatus() == Job.STATUS.EMPLOYED) {
                        // We are employed, no need to check interviews at all
                        return NO_SCHEDULE;
                    }
                }
                return DO_SCHEDULE;
            }
        }

        private void crawlApplications() throws JbmnplsLoggedOutException, IOException {
            // Crawl the appplications
            pulledJobs = new ArrayList<Job>();
            pulledAppsJobs = new HashMap<String, ArrayList<Job>>();
            pulledAppsJobs.put(Applications.LISTS.ACTIVE_JOBS, new ArrayList<Job>());
            pulledAppsJobs.put(Applications.LISTS.ALL_JOBS, new ArrayList<Job>());

            // Pull data from the application webpage
            String html = client.getJobmineHtml(JbmnplsHttpClient.GET_LINKS.APPLICATIONS);
            if (html != null) {
                parser.execute(Applications.ACTIVE_OUTLINES, html);
                parser.execute(Applications.ALL_OUTLINE, html);

                // Put data into storage
                jobSource.addJobs(pulledJobs);
                pageSource.addPage(client.getUsername(), Applications.PAGE_NAME, pulledAppsJobs, System.currentTimeMillis());
            }
        }

        private Boolean crawlInterviews() {
            // Get interviews data from the database
           HashMap<String, ArrayList<Integer>> ids = pageSource.getJobsIdMap(client.getUsername(), Interviews.PAGE_NAME);
           pulledJobs = new ArrayList<Job>();
           pulledInterviewsJobs = new HashMap<String, ArrayList<Job>>();
           pulledInterviewsJobs.put(Interviews.TABS.COMING_UP, new ArrayList<Job>());
           pulledInterviewsJobs.put(Interviews.TABS.FINISHED, new ArrayList<Job>());

           // Pull the interview data off the website
           String html;
           try {
               html = client.getJobmineHtml(JbmnplsHttpClient.GET_LINKS.INTERVIEWS);
           } catch (JbmnplsLoggedOutException e) {
               BugSenseHandler.sendException(e);
               e.printStackTrace();
               return false;
           } catch (SSLException e) {       // Ignore logging this, not an error
               e.printStackTrace();
               return false;
           } catch (IOException e) {
               BugSenseHandler.sendException(e);
               e.printStackTrace();
               return false;
           }

           if (html == null) {
               return true;
           }

           // Parse the html into jobs (except the canncelled jobs)
           try {
               parser.execute(Interviews.INTERVIEWS_OUTLINE, html);
               parser.execute(Interviews.GROUPS_OUTLINE, html);
               parser.execute(Interviews.SPECIAL_OUTLINE, html);
           } catch (JbmnplsParsingException e) {
               BugSenseHandler.sendException(e);
               e.printStackTrace();
               return false;
           }

           // Check to see if this is first time checking interviews on device
           if (ids == null) {
               // First time getting interviews, so we need to add it to the database
               jobSource.addJobs(pulledJobs);
               pageSource.addPage(client.getUsername(), Interviews.PAGE_NAME, pulledInterviewsJobs, System.currentTimeMillis());
           } else {
               // Parse out which are the new interviews
               if (pulledJobs.isEmpty()) {
                   return true;
               }

               // Parse the new interviews; remove all jobs that are already existing
               int newCount = 0;
               ArrayList<Integer> comingUpJobIds = ids.get(Interviews.TABS.COMING_UP);
               ArrayList<Integer> finishedJobIds = ids.get(Interviews.TABS.FINISHED);
               if (comingUpJobIds != null) {
                   for (int i = 0; i < pulledJobs.size(); i++) {
                       if (!comingUpJobIds.contains(pulledJobs.get(i).getId())) {
                           newCount++;
                       }
                   }
               }
               if (finishedJobIds != null) {
                   for (int i = 0; i < pulledJobs.size(); i++) {
                       if (!finishedJobIds.contains(pulledJobs.get(i).getId())) {
                           newCount++;
                       }
                   }
               }

               // No new jobs
               if (newCount == 0) {
                   return true;
               }

               String message = newCount + " new interview"
                       + (newCount==1?"":"s");
               showNotification("Jobmine Plus", message);
           }
           return true;
       }

        @Override
        protected void onPostExecute(Boolean shouldScheduleAlarm) {
            pageSource.close();
            jobSource.close();
            if (shouldScheduleAlarm && nextTimeout != 0) {
                scheduleNextAlarm(nextTimeout);
            }
            super.onPostExecute(shouldScheduleAlarm);
        }

        @Override
        public void onRowParse(TableParserOutline outline, Object... jobData) {
            Job job;
            if (outline == Applications.ALL_OUTLINE) {
                job = Applications.parseRowTableOutline(outline, jobData);
                pulledAppsJobs.get(Applications.LISTS.ALL_JOBS).add(job);
            } else if (outline == Applications.ACTIVE_OUTLINES[0] || outline == Applications.ACTIVE_OUTLINES[1]
                    || outline == Applications.ACTIVE_OUTLINES[2]) {
                job = Applications.parseRowTableOutline(outline, jobData);
                pulledAppsJobs.get(Applications.LISTS.ACTIVE_JOBS).add(job);
            } else {
                job = Interviews.parseRowTableOutline(outline, jobData);
                if (job.pastNow()) {
                    pulledInterviewsJobs.get(Interviews.TABS.FINISHED).add(job);
                } else {
                    pulledInterviewsJobs.get(Interviews.TABS.COMING_UP).add(job);
                }
            }
            pulledJobs.add(job);
        }
    }
}
