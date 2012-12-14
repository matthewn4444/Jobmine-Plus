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

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
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
        task.execute("");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class GetInterviewsTask extends AsyncTask<String, Void, Integer>
        implements TableParser.OnTableParseListener {
        private final TableParser parser = new TableParser();
        private ArrayList<Job> pulledJobs;

        public GetInterviewsTask() {
            parser.setOnTableRowParse(this);
        }

        @Override
        protected Integer doInBackground(String... params) {
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
                // TODO Auto-generated catch block
                e.printStackTrace();
                return 0;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return 0;
            }

            // Parse the html into jobs
            try {
                parser.execute(Interviews.INTERVIEWS_OUTLINE, html);
                parser.execute(Interviews.GROUPS_OUTLINE, html);
                parser.execute(Interviews.SPECIAL_OUTLINE, html);
                parser.execute(Interviews.CANCELLED_OUTLINE, html);
            } catch (JbmnplsParsingException e) {
                e.printStackTrace();
                return 0;
            }
            long timestamp = System.currentTimeMillis();

            // Parse out which are the new interviews
            if (ids != null) {
                // Both empty
                if (pulledJobs.isEmpty() && ids.isEmpty()) {
                    return 0;
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
                    return 0;
                }

                // TODO throw notification
            }
            pageSource.addPage(Interviews.PAGE_NAME, pulledJobs, timestamp);
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            pageSource.close();
            super.onPostExecute(result);
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
