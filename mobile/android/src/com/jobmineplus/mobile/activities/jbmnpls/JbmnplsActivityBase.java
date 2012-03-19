package com.jobmineplus.mobile.activities.jbmnpls;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jobmineplus.mobile.JbmnplsApplication;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.activities.HomeActivity;
import com.jobmineplus.mobile.activities.LoginActivity;
import com.jobmineplus.mobile.exceptions.HiddenColumnsException;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.widgets.Alert;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.ProgressDialogAsyncTaskBase;

public abstract class JbmnplsActivityBase extends FragmentActivity {

    //=================
    //  Declarations
    //=================
    protected static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy");
    
    private final int MAX_LOGIN_ATTEMPTS = 3;
    
    private String dataUrl = null;         //Use JbmnPlsHttpService.GET_LINKS.<url>

    private JbmnplsHttpService service;
    protected JbmnplsApplication app;
    protected GetHtmlTask task = null;
    protected final String LOADING_MESSAGE = "Fetching data...";
    private Alert alert;
    
    //====================
    //  Abstract Methods
    //====================
    
    /**
     * Running setUp() you need to specify the "layout" and
     * "dataUrl".
     * For example:
     *     protected void setUp() {
     *         setContentView(layout);
     *        String url = JbmnplsHttpService.GET_LINKS.APPLICATIONS;
     *        return url;
     *     }
     * @param savedInstanceState
     * @return dataUrl (String) that gets the data that will be parsed
     */
    protected abstract String setUp(Bundle savedInstanceState);
    
    /**
     * This allows the user to define all their UI object variables here.
     * is necessary but does not always need it. Please specify layout in
     * setUp();
     */
    protected abstract void defineUI(Bundle savedInstanceState);
    
    /**
     * Here you are given the document of the dataUrl page
     * specified in setUp(). Also render the layout with the 
     * data here.
     * 
     * @param doc
     */
    protected abstract void parseWebpage(Document doc);
    
    /**
     * Calling this when the parseWebpage is complete.
     * This is used when you need to update any visual element
     * that you cannot do in parseWebpage
     * 
     * @param doc
     */
    protected abstract void onRequestComplete();
    
    //====================
    //  Override Methods
    //====================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (JbmnplsApplication) getApplication();
        service = JbmnplsHttpService.getInstance();
        dataUrl = setUp(savedInstanceState);
        alert = new Alert(this);
        defineUI(savedInstanceState);
        requestData();
    }
    
    //========================
    //  Login/Logout Methods
    //========================
    protected boolean verifyLogin() {
        if (!service.isLoggedIn()) {
           for (int i = 0; i < MAX_LOGIN_ATTEMPTS; i++) {
               int result = service.login();
               if (result == JbmnplsHttpService.LOGIN) {
                   return true;
               } else if (result == JbmnplsHttpService.LOGGED_OFFLINE) {
                   return false;
               }
           }
           return false;
        }
        return true;
    }
    
    //======================
    //  Activity Movements
    //======================
    
    protected void goToLoginActivity(String reasonMsg) {
        startActivityWithMessage(LoginActivity.class, reasonMsg);
    }
    
    protected void goToHomeActivity(String reasonMsg) {
        startActivityWithMessage(HomeActivity.class, reasonMsg);
    }
    
    protected void goToDescription(int jobId) {
        BasicNameValuePair pass = new BasicNameValuePair("jobId", Integer.toString(jobId));
        startActivity(Description.class, pass);
    }
    
    protected void startActivityWithMessage(Class<?> cls, String reasonMsg) {
        Intent in = new Intent(this, cls);
        in.putExtra("reason", reasonMsg);
        startActivity(in);
        finish();
    }
    
    protected void startActivity(Class<?> goToClass) {
        NameValuePair[] empty = null;
        startActivity(goToClass, empty);
    }
    
    protected void startActivity(Class<?> goToClass, NameValuePair ...args) {
        Intent in = new Intent(this, goToClass);
        if (args != null) {
            for (NameValuePair arg: args) {
                in.putExtra(arg.getName(), arg.getValue());
            }
        }
        startActivity(in);
    }
    
    //=======================
    //  Dom Text Extraction
    //=======================
    /**
     * Information for each column
     * @author matthewn4444
     *
     */
    protected class ColumnInfo {
        final public static int TEXT      = 0;
        final public static int DATE      = 1;
        final public static int NUMERIC   = 2;
        final public static int DOUBLE    = 3;
        final public static int URL       = 4;
        final public static int STATE     = 5;
        final public static int STATUS    = 6;
        
        private int columnNumber;
        private int type;
        private String dateFormat;
        /**
         * This constructor is not for dates, For types please use
         * static object ColumnInfo.<Type> such as ColumnInfo.TEXT
         * @param columnNumber: the column number
         * @param type: ColumnInfo.<Type>
         */
        public ColumnInfo (int columnNumber, int type) {
            this.columnNumber = columnNumber;
            if (type < 0 || type > STATUS) {
                throw new JbmnplsParsingException("Setting the column type is invald.");
            }
            this.type = type;
        }
        /**
         * This constructor is only used for columns that has a date
         * Please you the static object ColumnInfo.DATE for 
         * @param columnNumber: the column number
         * @param type: ColumnInfo.DATE
         * @param dateFormat: a String that shows the format of the date in column
         */
        public ColumnInfo (int columnNumber, int type, String dateFormat) {
            this.columnNumber = columnNumber;
            if (type < 0 || type > 4) {
                throw new JbmnplsParsingException("Setting the column type is invald.");
            }
            this.type = type;
            if (type == DATE && dateFormat == null) {
                throw new JbmnplsParsingException("Date is invalid without specifying the dateformat.");
            }
            this.dateFormat = dateFormat;
        }
        public int getColumnNumber() {
            return columnNumber;
        }
        public int getType () {
            return type;
        }
        public String getDateFormat() {
            return dateFormat;
        }
    }
    
    /**
     * For this class, you need to have one for each table you are parsing
     * You should declare an object of this class final. To parse the job
     * data you must
     * DO NOT include id as the first column as all tables have that 
     * and it is redundent to declare it all the time.
     * @author matthewn4444
     *
     */
    protected class TableParsingOutline {
        private String tableId;
        private int numOfColumns;
        private ColumnInfo[] columnInfo;
        
        /**
         * This should be used with final keyword to describe the table
         * @param tableId: the DOM id (eg. css #element_id)
         * @param numOfColumns: number of expected columns, will throw an exception if failed
         *                      when executed
         * @param columnInfo: multi-arguments of column info (one per each column that is of 
         *                    interest for job arguments)
         */
        public TableParsingOutline(String tableId, int numOfColumns, ColumnInfo ...columnInfo) {
            this.tableId = tableId;
            this.columnInfo = columnInfo;
            this.numOfColumns = numOfColumns;
        }
        public void execute(Document doc) {
            int rowLength;
            Object[] passedObj = new Object[columnInfo.length + 1];
            Element header, table = parseTableById(doc, tableId);
            Elements rows;
            if (table == null) {
                throw new JbmnplsParsingException("Cannot parse '" + tableId + "'.");
            }
            rows = table.getElementsByTag("tr");
            header = rows.get(0);
            
            if (header.getElementsByTag("th").size() != numOfColumns) {
                throw new HiddenColumnsException();
            }
            rowLength = rows.size();
            for (int r = 1; r < rowLength; r++) {
                Element rowEl = rows.get(r);
                Elements tds = rowEl.getElementsByTag("td");
                
                // See if table is empty
                int id = getIntFromElement(tds.get(0));
                if (id == 0) {
                    break;
                }
                passedObj[0] = id;
                
                for (int c = 0; c < columnInfo.length; c++) {
                    ColumnInfo each = columnInfo[c];
                    Object value = null;
                    Element td = tds.get( each.getColumnNumber() );
                    switch (each.getType()) {
                    case ColumnInfo.TEXT:
                        value = getTextFromElement(td);
                        break;
                    case ColumnInfo.DATE:
                        value = getDateFromElement(td, each.getDateFormat());
                        break;
                    case ColumnInfo.DOUBLE:
                        value = getDoubleFromElement(td);
                        break;
                    case ColumnInfo.NUMERIC:
                        value = getIntFromElement(td);
                        break;
                    case ColumnInfo.URL:
                        value = getUrlFromElement(td);
                        break;
                    case ColumnInfo.STATE:
                        value = Job.STATE.getStatefromString(getTextFromElement(td));
                        break;
                    case ColumnInfo.STATUS:
                        value = Job.STATUS.getStatusfromString(getTextFromElement(td));
                        break;
                    default: 
                        throw new JbmnplsParsingException("Cannot parse column with invalid type.");
                    }
                    passedObj[c + 1] = value;
                }
                onRowParse(this, passedObj);
            }
        }
    }
    
    /**
     * This is needed once you parse each row
     * Parameter 1 will always be the job id, everything else will follow just 
     * the same as the table columns
     * @param data: the parameters for a job shown from TableParsingOutline class
     */
    protected void onRowParse(TableParsingOutline outline, Object ...jobData){}
    
    protected Element parseTableById(Document doc, String id) throws JbmnplsParsingException {
        try{
            return doc.getElementById(id).select("tr:eq(1) table table").first();
        } catch(Exception e) {
            throw new JbmnplsParsingException("Problem parsing table.");
        }
    }
    
    protected String getTextFromElement(Element e) {
        return e.text().replaceAll("\\s+", " ").trim();
    }
    
    protected Date getDateFromElement(Element e, String format) {
        String text = getTextFromElement(e);
        try {
            return new SimpleDateFormat(format, Locale.ENGLISH).parse(text);
        } catch (ParseException error) {
            error.printStackTrace();
            return new Date();
        }
    }
    
    protected int getIntFromElement(Element e) {
        String text = getTextFromElement(e);
        if (text.length() == 0) {
            return 0;
        }
        return Integer.parseInt(text);
    }
    
    protected double getDoubleFromElement(Element e) {
        String text = getTextFromElement(e);
        return Double.parseDouble(text);
    }
    
    protected String getUrlFromElement(Element e) {
        Element anchor = e.select("a").first();
        if (anchor == null) {
            return null;
        }
        return anchor.attr("href");
    }
    
    //=================
    //  Miscellaneous
    //=================
    
    protected boolean isLoading() {
        return task != null && task.isRunning();
    }
    

    protected void log(Object txt) {
        System.out.println(txt);
    }
    
    protected void showMessage(String message) {
        alert.show(message);
    }
    
    //====================================
    //  Data Request Classes and Methods
    //====================================
    
    protected void requestData() throws RuntimeException {
        task = new GetHtmlTask(this, LOADING_MESSAGE);
        if (dataUrl == null) {
            throw new RuntimeException("Class that extended JbmnPlsActivityBase without specifying a dataurl.");
        }
        task.execute(dataUrl);
    }
    
    /**
     * You can override this function if you need to fetch
     * something else besides the default url.
     * Return null if it failed and this class will throw 
     * a dialog saying it failed otherwise return the html
     * @param url
     * @return null if failed or String that is the html
     */
    protected String onRequestData(String[] args) throws JbmnplsLoggedOutException {
        String url = args[0];
        return service.getJobmineHtml(url);
    }
    
    private class GetHtmlTask extends ProgressDialogAsyncTaskBase<String, Void, Integer>{
        
        static final int NO_PROBLEM = 0;
        static final int FORCED_LOGGEDOUT = 1;
        static final int GO_HOME_NO_REASON = 2;
        static final int HIDDEN_COLUMNS_ERROR = 3;
        static final int PARSING_ERROR = 4;
        
        public GetHtmlTask(Activity activity, String dialogueMessage) {
            super(activity, dialogueMessage);
        }

        @Override
        protected Integer doInBackground(String... params) {
            if (!verifyLogin()) {
                return FORCED_LOGGEDOUT;
            }
            JbmnplsActivityBase activity = (JbmnplsActivityBase) getActivity();
            Document doc = null;
            try {
                String html = activity.onRequestData(params);
                if (html == null) {
                    return PARSING_ERROR;
                }
                doc = Jsoup.parse(html);
                activity.parseWebpage(doc);
                return NO_PROBLEM;
            } catch (HiddenColumnsException e) {
                e.printStackTrace();
                return HIDDEN_COLUMNS_ERROR;
            } catch (JbmnplsParsingException e) {
                e.printStackTrace();
                return PARSING_ERROR;
            } catch (JbmnplsLoggedOutException e) {
                e.printStackTrace();
                return FORCED_LOGGEDOUT;
            }
        }
        
        @Override
        protected void onPostExecute(Integer reasonForFailure) {
            super.onPostExecute(reasonForFailure);
            if (reasonForFailure == NO_PROBLEM) {
                onRequestComplete();
            } else {
                switch (reasonForFailure) {
                case FORCED_LOGGEDOUT:
                    goToLoginActivity(getString(R.string.jobmine_offline_message));
                    break;
                case PARSING_ERROR:
                    goToHomeActivity(getString(R.string.parsing_error_message));
                    break;
                case HIDDEN_COLUMNS_ERROR:
                    goToHomeActivity(getString(R.string.hidden_column_message));
                    break;
                case GO_HOME_NO_REASON:
                    goToHomeActivity("");
                    break;
                }
                finish();
            }
        }
    }
    
    //==================
    //  JobListAdapter
    //==================
    
    protected class JobListAdapter extends ArrayAdapter<Integer>{
        private ArrayList<Integer> entries;
        private Activity activity;
        
        public JobListAdapter(Activity a, int textViewResourceId, ArrayList<Integer> listOfIds) {
            super(a, textViewResourceId, listOfIds);
            entries = listOfIds;
            activity = a;
        }
        
        private class JobListElements {
            public TextView job_title;
            public TextView job_employer;
            public TextView job_status;
            public TextView last_date;
            public TextView numApps;
            public TextView location;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View item = convertView;
            JobListElements elements;
            if (item == null) {
                LayoutInflater inflator = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                item = inflator.inflate(R.layout.job_widget, null);
                elements = new JobListElements();
                elements.job_title =        (TextView) item.findViewById(R.id.job_title);
                elements.job_employer =     (TextView) item.findViewById(R.id.job_employer);
                elements.location =         (TextView) item.findViewById(R.id.location);
                elements.job_status =       (TextView) item.findViewById(R.id.job_status);
                elements.last_date =        (TextView) item.findViewById(R.id.job_last_day);
                elements.numApps =          (TextView) item.findViewById(R.id.job_apps);
                item.setTag(elements);
            } else {
                elements = (JobListElements) item.getTag();
            }
            
            final int id = entries.get(position);
            final Job entry = app.getJob(id);
            if (entry != null) {
                elements.job_title.setText(entry.getTitle());
                elements.job_employer.setText(entry.getEmployer());
                elements.job_status.setText(entry.getDisplayStatus());
                elements.last_date.setText(DISPLAY_DATE_FORMAT.format(entry.getLastDateToApply()));
                elements.numApps.setText(Integer.toString(entry.getNumberOfApplications()));
                
                String location = entry.getLocation();
                if (location != null && location != "") {
                    elements.location.setText(location);
                } else {
                    elements.location.setVisibility(View.GONE);
                }
            }
            return item;
        }
    }
}    

    
    
    
    
    
    
