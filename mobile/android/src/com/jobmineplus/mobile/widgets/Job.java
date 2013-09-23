package com.jobmineplus.mobile.widgets;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import android.text.TextUtils;

import com.jobmineplus.mobile.exceptions.JbmnplsException;
import com.jobmineplus.mobile.exceptions.JbmnplsLoggedOutException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.table.SimpleHtmlParser;

public class Job {
    // ================
    //  Constructors
    // ================
    /**
     * As long as all the data is present in the fields (ie. no one hid the
     * columns) then you should be able to fill all of these fields. If user has
     * blocked columns please alert the user to unhide them on the website.
     */

    /**
     * Applications Constructor Since this is the applications, it will consider
     * that you have applied for this job.
     *
     * @param jId
     * @param jTitle
     * @param jEmployer
     * @param jTerm
     * @param jState
     *            - eg. Job.STATE.FILLED
     * @param jStatus
     *            - eg. Job.STATUS.SCREENED
     * @param jLastDate
     * @param jNumApps
     */
    public Job(int jId, String jTitle, String jEmployer, String jTerm,
            STATE jState, STATUS jStatus, Date jLastDate, int jNumApps) {
        setId(jId);
        title = jTitle;
        employer = jEmployer;
        term = jTerm;
        state = jState;
        status = jStatus;
        lastToApply = jLastDate;
        numApps = jNumApps;
    }

    /**
     * Short List Constructor Since this is the job short list, it will consider
     * that you have applied for this job.
     *
     * @param jId
     * @param jTitle
     * @param jEmployer
     * @param jLocation
     * @param jAppStatus
     * @param jLastDate
     * @param jNumApps
     */
    public Job(int jId, String jTitle, String jEmployer, String jLocation,
            APPLY_STATUS jAppStatus, Date jLastDate, int jNumApps) {
        setId(jId);
        title = jTitle;
        employer = jEmployer;
        location = jLocation;
        app_status = jAppStatus;
        lastToApply = jLastDate;
        numApps = jNumApps;
    }

    /**
     * Job Search Constructor
     *
     * @param jId
     * @param jTitle
     * @param jEmployer
     * @param jLocation
     * @param jOpenings
     * @param jLastDate
     * @param jNumApps
     */
    public Job(int jId, String jTitle, String jEmployer, String jLocation,
            int jOpenings, Date jLastDate, int jNumApps) {
        setId(jId);
        title = jTitle;
        employer = jEmployer;
        location = jLocation;
        openings = jOpenings;
        lastToApply = jLastDate;
        numApps = jNumApps;
    }

    /**
     * Interviews Constructor You must use this for interviews, we assume that
     * you have already applied and you are getting an interview for this job.
     *
     * @param jId
     * @param jEmployer
     * @param jTitle
     */
    public Job(int jId, String jEmployer, String jTitle) {
        setId(jId);
        title = jTitle;
        employer = jEmployer;
    }

   /**
    * Database Constructor This is used for putting all data back into a Job
    * object
    *
    * @param jId
    * @param jTitle
    * @param jEmployer
    * @param jTerm
    * @param jState
    * @param jStatus
    * @param jAppStatus
    * @param jLastToApply
    * @param jNumApps
    * @param jOpenings
    * @param jOpenToApply
    * @param jEmployerFull
    * @param jGradesRequired
    * @param jLocation
    * @param jDisciplines
    * @param jLevels
    * @param jHiringSupport
    * @param jWorkSupport
    * @param jDescription
    * @param jWarning
    * @param jInterviewStart
    * @param jInterviewEnd
    * @param jInterviewType
    * @param jRoom
    * @param jInstructions
    * @param jInterviewer
    */
    public Job(int jId, String jTitle, String jEmployer, String jTerm,
            String jState, String jStatus, String jAppStatus, long jLastToApply, int jNumApps,
            int jOpenings, long jOpenToApply, String jEmployerFull,
            int jGradesRequired, String jLocation, String jDisciplines,
            String jLevels, String jHiringSupport, String jWorkSupport,
            String jDescription, String jWarning, long jInterviewStart,
            long jInterviewEnd, String jInterviewType, String jRoom,
            String jInstructions, String jInterviewer) {
        setId(jId);
        title = jTitle;
        employer = jEmployer;
        term = jTerm;
        if (jState != null) {
            state = STATE.getStatefromString(jState);
        }
        if (jStatus != null) {
            status = STATUS.getStatusfromString(jStatus);
        }
        if (jAppStatus != null) {
            app_status = APPLY_STATUS.getApplicationStatusfromString(jAppStatus);
        }
        lastToApply = new Date(jLastToApply);
        numApps = jNumApps;
        openings = jOpenings;
        openToApply = new Date(jOpenToApply);
        employerFull = jEmployerFull;
        gradesRequired = jGradesRequired == 1;
        location = jLocation;
        disciplines = jDisciplines == null ? null : jDisciplines.split(",");
        levels = jLevels == null ? null :
            LEVEL.getLevelsFromStrArray(jLevels.split(","));
        hiringSupport = jHiringSupport;
        workSupport = jWorkSupport;
        description = jDescription;
        warning = jWarning;
        interviewStartTime = new Date(jInterviewStart);
        interviewEndTime = new Date(jInterviewEnd);
        interview_type = INTERVIEW_TYPE.getTypefromString(jInterviewType);
        room = jRoom;
        instructions = jInstructions;
        interviewer = jInterviewer;
    }

    //==========================
    //  Interview Constructors
    //==========================
    /**
     * Main Interviews Constructor
     *  This is the constructor for the first table
     * @param jobId
     * @param employer
     * @param title
     * @param interviewStartTime
     * @param interviewEndTime
     * @param interview_type
     * @param room
     * @param instructions
     * @param interviewer
     * @param jobStatus
     */
    public Job(int jobId, String employer, String title, Date interviewStartTime, Date interviewEndTime, INTERVIEW_TYPE interview_type,
            String roomInfo, String instructions, String interviewer) throws JbmnplsParsingException{
        setId(jobId);
        if (interview_type == INTERVIEW_TYPE.GROUP || interview_type == INTERVIEW_TYPE.CANCELLED || interview_type == INTERVIEW_TYPE.SPECIAL) {
            throw new JbmnplsParsingException("You used the wrong constructor for this interviews interview_type!");
        }
        this.interview_type = interview_type;
        setEmployer(employer);
        setTitle(title);
        setInterviewStartTime(interviewStartTime);
        setInterviewEndTime(interviewEndTime);
        setRoomInfo(roomInfo);
        setInstructions(instructions);
        setInterviewer(interviewer);
    }

    /**
     * Group Interviews Constructor
     *  This is the constructor for the 2nd table
     * @param jobId
     * @param employer
     * @param title
     * @param interviewStartTime
     * @param interviewEndTime
     * @param roomInfo
     * @param instructions
     */
    public Job(int jobId, String employer, String title, Date interviewStartTime,
            Date interviewEndTime, String roomInfo, String instructions) {
        setId(jobId);
        setEmployer(employer);
        setTitle(title);
        setInterviewStartTime(interviewStartTime);
        setInterviewEndTime(interviewEndTime);
        this.interview_type = INTERVIEW_TYPE.GROUP;
        setRoomInfo(roomInfo);
        setInstructions(instructions);
    }

    /**
     * Special Interviews Constructor
     *  This is the constructor for the 3rd table
     * @param jobId
     * @param employer
     * @param title
     * @param instructions
     */
    public Job(int jobId, String employer, String title, String instructions) {
        setId(jobId);
        setEmployer(employer);
        setTitle(title);
        this.interview_type = INTERVIEW_TYPE.SPECIAL;
        setInstructions(instructions);
    }

    // ===========================
    //  Static properties used
    // ===========================

    static public enum STATE {
        CANCELLED("Cancelled", 9),
        AVAILABLE("Applications Available", 2),
        FILLED("Filled", 6),
        RESET("Reset", 1),
        POSTED("Posted", 1),
        SCREENED("Screened", 4),
        RANKING_COMPLETED("Ranking Completed", 8),
        SCHEDULED("Scheduled", 5),
        APPROVED("Approved", 2),
        COMPLETE("Complete", 2),
        PENDING("Pending", 4),
        UNFILLED("Unfilled", 8);

        public static STATE getStatefromString(String text)
                throws JbmnplsParsingException {
            if (text == null) {
                return null;
            }
            for (STATE b : STATE.values()) {
                if (text.equalsIgnoreCase(b.toString())) {
                    return b;
                }
            }
            throw new JbmnplsParsingException("State: Cannot match value '"
                    + text + "'");
        }

        public static STATE getDefault() {
            return POSTED;
        }

        @Override
        public String toString() {
            return state;
        }

        public int getPriority() {
            return priority;
        }

        private STATE(String s, int p) {
            state = s;
            priority = p; // The higher the number, the more priority it has
        }

        private String state;
        private int priority;
    }

    static public enum STATUS {
        APPLIED("Applied", 3),
        CANCELLED("Cancelled", 9),
        NOT_POSTED("Not Posted", 2),
        SIGN_OFF("Sign Off", 3),
        NO_RESUME("No Resumes Available", 2),
        NOT_SELECTED("Not Selected", 10),
        NOT_RANKED("Not Ranked", 7),
        EMPLOYED("Employed", 11),
        SELECTED("Selected", 10),
        SCHEDULED("Scheduled", 10),
        ALTERNATE("Alternate", 6),
        BLANK("", 0);

        public static STATUS getStatusfromString(String text)
                throws JbmnplsParsingException {
            if (text == null) {
                return null;
            }
            for (STATUS b : STATUS.values()) {
                if (text.equalsIgnoreCase(b.toString())) {
                    return b;
                }
            }
            throw new JbmnplsParsingException("Status: Cannot match value '"
                    + text + "'");
        }

        public static STATUS getDefault() {
            return BLANK;
        }

        @Override
        public String toString() {
            return status;
        }

        public int getPriority() {
            return priority;
        }

        private STATUS(String s, int p) {
            status = s;
            priority = p; // The higher the number, the more priority it has
        }

        private String status;
        private int priority;
    }

    static public enum APPLY_STATUS {
        APPLY("Apply", 3),
        ALREADY_APPLIED("Already Applied", 3),
        CANNOT_APPLY("Not Authorized to Apply", 8),
        NO_RESUME("No Resumes Available", 2),
        NO_APPS("No Apps Available", 2),
        NOT_POSTED("Not Posted", 2);

        public static APPLY_STATUS getApplicationStatusfromString(String text)
                throws JbmnplsParsingException {
            if (text == null) {
                return null;
            }
            for (APPLY_STATUS b : APPLY_STATUS.values()) {
                if (text.equalsIgnoreCase(b.toString())) {
                    return b;
                }
            }
            throw new JbmnplsParsingException("Application Status: Cannot match value '"
                    + text + "'");
        }

        public static APPLY_STATUS getDefault() {
            return CANNOT_APPLY;
        }

        @Override
        public String toString() {
            return application_status;
        }

        public int getPriority() {
            return priority;
        }

        private APPLY_STATUS(String s, int p) {
            application_status = s;
            priority = p; // The higher the number, the more priority it has
        }

        private String application_status;
        private int priority;
    }

    static public enum LEVEL {
        JUNIOR("Junior"), INTERMEDIATE("Intermediate"), SENIOR("Senior"), BACHELORS(
                "Bachelors"), MASTERS("Masters"), PHD("Ph.D.");

        public static LEVEL getLevelfromString(String text)
                throws JbmnplsParsingException {
            if (text != null) {
                if (text.equals("")) {
                    return null;
                }
                for (LEVEL b : LEVEL.values()) {
                    if (text.equalsIgnoreCase(b.toString())) {
                        return b;
                    }
                }
            }
            throw new JbmnplsParsingException("Level: Cannot match value '"
                    + text + "'");
        }

        public static LEVEL[] getLevelsFromStrArray(String[] arr) {
            LEVEL[] levels = new LEVEL[arr.length];
            int count = 0;
            for (String str : arr) {
                levels[count++] = getLevelfromString(str);
            }
            return levels;
        }

        @Override
        public String toString() {
            return state;
        }

        private LEVEL(String s) {
            state = s;
        }

        private String state;
    }

    static public enum INTERVIEW_TYPE{
        IN_PERSON   ("In Person"),
        VIDEO       ("Video"),
        PHONE       ("Phone"),
        GROUP       ("Group"),
        WEBCAM      ("Webcam"),
        SPECIAL     ("Special"),
        CANCELLED   ("Cancelled");
        public static INTERVIEW_TYPE getTypefromString(String text) throws JbmnplsParsingException {
            if (text == null) {
                return null;
            }
            for (INTERVIEW_TYPE interview_type : INTERVIEW_TYPE.values()) {
                String a = interview_type.toString().toLowerCase(Locale.getDefault());
                text = text.toLowerCase(Locale.getDefault());
                if (text.contains(a)) {
                    return interview_type;
                }
            }
            throw new JbmnplsParsingException("State: Cannot match value '" + text + "'");
        }
        @Override
        public String toString() {
            return interview_type;
        }
        private INTERVIEW_TYPE(String s) {
            interview_type = s;
        }
        private String interview_type;
    }

    static public enum HEADER {
        // Common
        JOB_ID("job id", "Job ID"),
        JOB_TITLE("job title", "Job Title"),
        EMPLOYER("employer", "Employer"),
        JOB_STATUS("job status", "Job Status"),

        // Applications
        UNIT("unit", "Unit"),
        TERM("term", "Term"),
        APP_STATUS("app. status", "Application Status"),
        VIEW_DETAILS("view details", "View Details"),
        LAST_DAY_TO_APPLY("last day to apply", "Last Day to Apply"),
        NUM_APPS("# apps", "Number of Apps"),
        VIEW_PACKAGE("view package", "View Package"),

        // Interviews
        EMPLOYER_NAME("employer name", "Employer"),
        DATE("date", "Date"),
        INTER_TYPE("type", "Type"),
        SELECT_TIME("select/view time", "Interview Select Time"),
        START_TIME("start time", "Start Time"),
        END_TIME("end time", "Finished Time"),
        LENGTH("length", "Length"),
        ROOM("room", "Room Info"),
        INSTRUCTIONS("instructions", "Instructions"),
        INTERVIEWER("interviewer", "Interviewer"),

        // Shortlist
        JOB_IDENTIFIER("job identifier", "Job ID"),
        UNIT_NAME_1("unit name 1", "Unit"),
        LOCATION("location", "Job Location"),
        APPLY("apply", "Application Status"),
        LAST_DATE_TO_APPLY("last date to apply", "Last Date to Apply"),

        // Job Search
        OPENINGS("openings", "Openings"),
        SHORTLIST("shortlist", "Short List"),

        BLANK("", "");

        @Override
        public String toString() {
            return header;
        }

        public String readable() {
            return readable;
        }

        private HEADER(String headerName, String readableName) {
            header = headerName;
            readable = readableName;
        }

        private String header;
        private String readable;
    }

    static public final String DESCR_URL_PREFIX = "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_JOBDTLS?UW_CO_JOB_ID=";
    private final short NUM_DIGITS_ID = 8;
    private final String REQUIRED_TEXT = "Required";
    private final String TAG = "span";
    private final int NUM_OF_TAGS = 32;

    // ===========================
    // Long list of properties
    // ===========================

    // Definitely write once; cannot change
    protected int id;
    protected String title;
    protected String employer;
    protected String term;

    // These can change
    protected STATE state = STATE.getDefault();
    protected STATUS status = STATUS.getDefault();
    protected APPLY_STATUS app_status = APPLY_STATUS.getDefault();
    protected Date lastToApply;
    protected int numApps;
    protected int openings;

    // Gained from job description
    protected Date openToApply;
    protected String employerFull;
    protected boolean gradesRequired = true;
    protected String location;
    protected String[] disciplines;
    protected LEVEL[] levels;
    protected String hiringSupport;
    protected String workSupport;
    protected String description;
    protected String warning;

    // Interview Data
    private Date interviewStartTime;
    private Date interviewEndTime;
    private INTERVIEW_TYPE interview_type;
    private String room;
    private String instructions;
    private String interviewer;

    // Cannot set this
    protected String url = null;

    // Other booleans
    protected boolean hasRead = false;

    // ==============
    // Is Methods
    // ==============
    public boolean areGradesRequired() {
        return gradesRequired;
    }

    public boolean hasRead() {
        return hasRead;
    }

    public boolean hasApplied() {
        return status != STATUS.BLANK && app_status != APPLY_STATUS.CANNOT_APPLY;
    }

    public boolean canApply() {
        if (openToApply == null) {
            return false;
        }
        Date now = new Date();
        return now.after(openToApply) && now.before(lastToApply);
    }

    public boolean isOld() {
        return new Date().after(lastToApply);
    }

    public boolean hasDescriptionData() {
        return description != null && description != "";
    }

    // This is for interview data
    public boolean hasPassed() {
        if (interviewEndTime == null) {
            return true;
        }
        return new Date().after(interviewEndTime);
    }

    // =============
    // Setters
    // =============
    public void setState(STATE jState) {
        state = jState;
    }

    public void setStatus(STATUS jStatus) {
        status = jStatus;
    }

    public void setLastDateToApply(Date date) {
        lastToApply = date;
    }

    public void setOpeningDateToApply(Date date) {
        openToApply = date;
    }

    public void setNumberOfApplications(int num) {
        numApps = num;
    }

    public void setNumberOfOpenings(int num) {
        openings = num;
    }

    public void setGradesRequired(Boolean flag) {
        gradesRequired = flag;
    }

    public void setLocation(String loc) {
        location = loc;
    }

    public void setLevels(LEVEL[] levelsArr) {
        levels = levelsArr;
    }

    public void setDisciplines(String[] disc) {
        disciplines = disc;
    }

    public void setHiringSupport(String name) {
        hiringSupport = name;
    }

    public void setWorkSupport(String name) {
        workSupport = name;
    }

    public void setDescription(String text) {
        description = text;
    }

    public void setDescriptionWarning(String text) {
        warning = text;
    }

    public void setDescriptionData(
            String fullEmployerName,
            String title,
            String location,
            LEVEL[] levelsArr,
            Date openingDate,
            Date lastDate,
            boolean areGradesRequired,
            int numOpenings,
            String[] disciplines,
            String workSupportName,
            String hiringSupportName,
            String descriptionWarning,
            String description) {
        setEmployerFullName(fullEmployerName);
        setTitle(title);
        setLocation(location);
        setLevels(levelsArr);
        setOpeningDateToApply(openingDate);
        setLastDateToApply(lastDate);
        setGradesRequired(areGradesRequired);
        setNumberOfOpenings(numOpenings);
        setDisciplines(disciplines);
        setWorkSupport(workSupportName);
        setHiringSupport(hiringSupportName);
        setDescriptionWarning(descriptionWarning);
        setDescription(description);
    }

    // Protected
    protected void setTitle(String jTitle) {
        title = jTitle;
    }

    protected void setEmployer(String jEmployer) {
        employer = jEmployer;
    }

    protected void setTerm(String jTerm) {
        term = jTerm;
    }

    protected void setEmployerFullName(String name) {
        employerFull = name;
    }

    protected void setReadAlready(boolean flag) {
        hasRead = flag;
    }

    protected void setId(int jId) throws IllegalArgumentException {
        if (jId <= 0) {
            throw new IllegalArgumentException(
                    "You cannot set an id that is negative or equal to 0.");
        }
        id = jId;
        String str_id = String.valueOf(jId);
        while (str_id.length() < NUM_DIGITS_ID) {
            str_id = "0" + str_id;
        }
        url = Job.DESCR_URL_PREFIX + str_id;
    }

    public void setInterviewStartTime(Date start) {
        interviewStartTime = start;
    }

    public void setInterviewEndTime(Date end) {
        interviewEndTime = end;
    }

    public void setRoomInfo(String roomInfo) {
        room = roomInfo;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void setInterviewer(String name) {
        interviewer = name;
    }

    // =============
    // Getters
    // =============
    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getEmployer() {
        return employer != null ? employer : employerFull;
    }

    public String getTerm() {
        return term;
    }

    public STATE getState() {
        return state;
    }

    public STATUS getStatus() {
        return status;
    }

    public APPLY_STATUS getApplicationStatus() {
        return app_status;
    }

    public Date getLastDateToApply() {
        return lastToApply;
    }

    public Date getOpenDateToApply() {
        return openToApply;
    }

    public int getNumberOfApplications() {
        return numApps;
    }

    public int getNumberOfOpenings() {
        return openings;
    }

    public String getLocation() {
        return location;
    }

    public String[] getDisciplines() {
        return disciplines;
    }

    public String getDisciplinesAsString() {
        return getDisciplinesAsString(",");
    }

    public String getDisciplinesAsString(CharSequence delimiter) {
        return arrayJoin(disciplines, delimiter);
    }

    public LEVEL[] getLevels() {
        return levels;
    }

    public String getLevelsAsString() {
        return getLevelsAsString(",");
    }

    public String getLevelsAsString(CharSequence delimiter) {
        return arrayJoin(levels, delimiter);
    }

    public String getHiringSupportName() {
        return hiringSupport;
    }

    public String getWorkSupportName() {
        return workSupport;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayStatus() {
        return status.getPriority() > state.getPriority() ? status.toString()
                : state.toString();
    }

    public String getDescriptionWarning() {
        return warning;
    }

    public String getEmployerFullName() {
        return employerFull;
    }

    public Date getInterviewStartTime() {
        return interviewStartTime;
    }

    public Date getInterviewEndTime() {
        return interviewEndTime;
    }

    public int getInterviewLengthInMinutes() {
        int length = (int)((interviewEndTime.getTime() - interviewStartTime.getTime()) / 1000) / 60;
        if (length <= 0) {
            throw new JbmnplsException("Either start or end time is not set, and therefore " +
                    "cannot get 0 or negative length in time.");
        }
        return length;
    }

    public INTERVIEW_TYPE getInterviewType() {
        return interview_type;
    }

    public String getRoomInfo() {
        return room;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getInterviewer() {
        return interviewer;
    }

    // ===========
    //  Methods
    // ===========
    public boolean pastNow() {
        if (interviewStartTime == null && interviewEndTime == null) {
            return false;
        }
        Calendar now = Calendar.getInstance();
        Date endTime = interviewEndTime != null ? interviewEndTime : interviewStartTime;
        return now.after(endTime);
    }

    public String grabDescriptionData(JbmnplsHttpClient client) throws IOException {
        // Get the html
        String html;
        try {
            html = client.getJobmineHtml(url);
        } catch (JbmnplsLoggedOutException e) {
            e.printStackTrace();
            return null;
        }
        if (html == null) {
            return null;
        }

        StopWatch sw = new StopWatch(true);

        // Starts us at the correct table and then we get the text
        int start = html.indexOf("id='ACE_width'");
        if (start == -1) {
            start = html.indexOf("id=\"ACE_width\"");
            if (start == -1) { throw new JbmnplsParsingException("Cannot find id in html."); }
        }

        // Easiest way to parse job details is to go through spans
        SimpleHtmlParser parser = new SimpleHtmlParser(html, start);
        String text, disc = "";
        for (int i = 0; i < NUM_OF_TAGS; i++) {
            switch (i) {
            case 3:
                text = parser.getTextInNextElement(TAG);
                setOpeningDateToApply(parseDate(text));
                break;
            case 4:
                text = parser.getTextInNextElement(TAG);
                setLastDateToApply(parseDate(text));
                break;
            case 10:
                text = parser.getTextInNextElement(TAG);
                setEmployerFullName(text);
                break;
            case 12:
                text = parser.getTextInNextElement(TAG);
                setTitle(text);
                break;
            case 14:
                text = parser.getTextInNextElement(TAG);
                setGradesRequired(text.equals(REQUIRED_TEXT));
                break;
            case 16:
                text = parser.getTextInNextElement(TAG);
                setLocation(text);
                break;
            case 18:
                text = parser.getTextInNextElement(TAG);
                setNumberOfOpenings(text.length() == 0 ? 0 : Integer.parseInt(text));
                break;
            case 20:
                disc = parser.getTextInNextElement(TAG);
                break;
            case 21:
                String temp = parser.getTextInNextElement(TAG);
                if (temp.length() != 0) {
                    disc += "," + temp;
                }
                setDisciplines(disc.split(","));
                break;
            case 23:
                String[] thing = parser.getTextInNextElement(TAG).split(",");
                LEVEL[] l = new LEVEL[thing.length];
                for (int j = 0; j < thing.length; j++) {
                    String tempStr = thing[j].trim();
                    if (tempStr.length() != 0) {
                        l[j] = LEVEL.getLevelfromString(tempStr);
                    }
                }
                setLevels(l);
                break;
            case 27:
                text = parser.getTextInNextElement(TAG);
                setWorkSupport(text);
                break;
            case 26:
                text = parser.getTextInNextElement(TAG);
                setHiringSupport(text);
                break;
            case 29:
                text = parser.getTextInNextElement(TAG);
                setDescriptionWarning(text);
                break;
            case 31:
                text = parser.getTextInNextElement(TAG);
                setDescription(text.replace("<br />", "\n"));
                break;
            default:
                parser.skipTag(TAG);
                break;
            }
        }
        hasRead = true;
        sw.printElapsed("%s ms has passed to parse html");
        return html;
    }

    protected Date parseDate(String dateString) {
        if (TextUtils.isEmpty(dateString)) {
            return new Date();
        }
        try {
            return new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
                    .parse(dateString);
        } catch (ParseException error) {
            error.printStackTrace();
            return new Date();
        }
    }

    protected String arrayJoin(Object[] array, CharSequence delimiter) {
        if (array == null) {
            return null;
        }
        String returnStr = "";
        int i = 1;
        int size = array.length;
        if (size > 0) {
            if (array[0] == null) {
                return "";
            }
            returnStr = array[0].toString();
            for (; i < size; i++) {
                returnStr += delimiter.toString() + array[i].toString().trim();
            }
        }
        return returnStr;
    }

    // ===================
    //  HeaderComparator
    // ===================
    public static final class HeaderComparator implements Comparator<Job> {
        public static enum DIRECTION {
            ASCEND, DESCEND
        };

        private HEADER header;
        private DIRECTION direction;

        public HeaderComparator() {
            direction = DIRECTION.ASCEND;
        }

        public HeaderComparator(HEADER headerName) {
            header = headerName;
            direction = DIRECTION.ASCEND;
        }

        public HeaderComparator(HEADER headerName, DIRECTION sortDirection) {
            header = headerName;
            direction = sortDirection;
        }

        public void setHeader(HEADER headerName) {
            header = headerName;
        }

        public void setDirection(DIRECTION sortDirection) {
            direction = sortDirection;
        }

        @Override
        public int compare(Job j1, Job j2) {
            int result = 0;
            switch(header) {
                // Strings
                case JOB_TITLE:
                    result =  j1.getTitle().compareTo(j2.getTitle());
                    break;
                case EMPLOYER:
                case EMPLOYER_NAME:
                    result =  j1.getEmployer().compareTo(j2.getEmployer());
                    break;
                case TERM:
                    result =  j1.getTerm().compareTo(j2.getTerm());
                    break;
                case ROOM:
                    result =  j1.getRoomInfo().compareTo(j2.getRoomInfo());
                    break;
                case INSTRUCTIONS:
                    result =  j1.getInstructions().compareTo(j2.getInstructions());
                    break;
                case INTERVIEWER:
                    result =  j1.getInterviewer().compareTo(j2.getInterviewer());
                    break;
                case LOCATION:
                    result =  j1.getLocation().compareTo(j2.getLocation());
                    break;

                // Integers
                case NUM_APPS:
                    result =  j1.getNumberOfApplications() - j2.getNumberOfApplications();
                    break;
                case LENGTH:
                    result =  j1.getInterviewLengthInMinutes() - j2.getInterviewLengthInMinutes();
                    break;

                // Dates
                case LAST_DAY_TO_APPLY:
                case LAST_DATE_TO_APPLY:
                case DATE:
                    result =  j1.getLastDateToApply().compareTo(j2.getLastDateToApply());
                    break;

                case START_TIME:
                    result =  j1.getInterviewStartTime().compareTo(j2.getInterviewStartTime());
                    break;
                case END_TIME:
                    result =  j1.getInterviewEndTime().compareTo(j2.getInterviewEndTime());
                    break;

                // Other
                case INTER_TYPE:
                    result =  j1.getInterviewType().toString().compareTo(j2.getInterviewType().toString());
                    break;
                case APPLY:
                    result =  j1.getApplicationStatus().toString().compareTo(j2.getApplicationStatus().toString());
                    break;
                case APP_STATUS:
                case JOB_STATUS:
                    result =  j1.getDisplayStatus().toString().compareTo(j2.getDisplayStatus().toString());
                    break;
                default:
                    throw new RuntimeException("Cannot sort the headers of " + header);
            }
            return direction == DIRECTION.ASCEND ? result : -result;
        }
    }
}
