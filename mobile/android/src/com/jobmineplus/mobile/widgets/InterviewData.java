package com.jobmineplus.mobile.widgets;

import java.util.Date;

import com.jobmineplus.mobile.exceptions.JbmnplsException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;

public class InterviewData {
    //================
    //  Constructors
    //================
    /**
     * Main Interviews Constructor
     *  This is the constructor for the first table
     * @param jobId
     * @param employer
     * @param title
     * @param startTime
     * @param endTime
     * @param type
     * @param room
     * @param instructions
     * @param interviewer
     * @param jobStatus
     */
    public InterviewData(int jobId, String employer, String title, Date startTime, Date endTime, TYPE type, 
            String roomInfo, String instructions, String interviewer) throws JbmnplsParsingException{
        setJobId(jobId);
        if (type == TYPE.GROUP || type == TYPE.CANCELLED || type == TYPE.SPECIAL) {
            throw new JbmnplsParsingException("You used the wrong constructor for this interviews type!");
        }
        this.type = type;
        setEmployer(employer);
        setTitle(title);
        setStartTime(startTime);
        setEndTime(endTime);
        setRoomInfo(roomInfo);
        setInstructions(instructions);
        setInterview(interviewer);
    }
    
    /**
     * Group Interviews Constructor
     *  This is the constructor for the 2nd table
     * @param jobId
     * @param employer
     * @param title
     * @param startTime
     * @param endTime
     * @param roomInfo
     * @param instructions
     */
    public InterviewData(int jobId, String employer, String title, Date startTime, 
            Date endTime, String roomInfo, String instructions) {
        setJobId(jobId);
        setEmployer(employer);
        setTitle(title);
        setStartTime(startTime);
        setEndTime(endTime);
        this.type = TYPE.GROUP;
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
    public InterviewData(int jobId, String employer, String title, String instructions) {
        setJobId(jobId);
        setEmployer(employer);
        setTitle(title);
        this.type = TYPE.SPECIAL;
        setInstructions(instructions);
    }
    
    /**
     * Cancelled Interviews Constructor
     *  This is the constructor for the 4th and last table
     * @param jobId
     * @param employer
     * @param title
     */
    public InterviewData(int jobId, String employer, String title) {
        setJobId(jobId);
        setEmployer(employer);
        setTitle(title);
        this.type = TYPE.CANCELLED;
    }
    
    //=======================
    //  Types of Interviews
    //=======================
    static public enum TYPE{
        IN_PERSON   ("In Person"),
        VIDEO       ("Video"),
        PHONE       ("Phone"),
        GROUP       ("Group"),
        SPECIAL     ("Special"),
        CANCELLED   ("Cancelled");
        public static TYPE getTypefromString(String text) throws JbmnplsParsingException {
            if (text != null) {
                for (TYPE type : TYPE.values()) {
                    String a = type.toString().toLowerCase();
                    text = text.toLowerCase(); 
                    if (text.contains(a)) {
                        return type;
                    }
                }
            }
            throw new JbmnplsParsingException("State: Cannot match value '" + text + "'");
        }
        @Override
        public String toString() {
            return type;
        }
        private TYPE(String s) {
            type = s;
        }
        private String type;
    }
    
    //========================
    //  Variable Declaration
    //========================
    private int jobId;
    private String employer;
    private String title;
    private Date startTime;
    private Date endTime;
    private TYPE type;
    private String room;
    private String instructions;
    private String interviewer;
    
    //==============
    //  Is Methods
    //==============
    public boolean hasPassed() {
        if (endTime == null) {
            return true;
        }
        return new Date().after(endTime);
    }
    
    //==================
    //  Getter Methods
    //==================
    public int getJobId() {
        return jobId;
    }
    
    public String getEmployer() {
        return employer;
    }
    
    public String getTitle() {
        return title;
    }
    
    public Date getStartTime() {
        return startTime;
    }
    
    public Date getEndTime() {
        return endTime;
    }
    
    public int getLengthInMinutes() {
        int length = (int)((endTime.getTime() - startTime.getTime()) / 1000) / 60;
        if (length <= 0) {
            throw new JbmnplsException("Either start or end time is not set, and therefore " +
            		"cannot get 0 or negative length in time.");
        }
        return length;
    }
    
    public TYPE getType() {
        return type;
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
    
    //==================
    //  Setter Methods
    //==================
    public void setJobId(int id) throws JbmnplsException {
        if (id <= 0) {
            throw new JbmnplsException("Cannot set a job id to interview data as 0.");
        }
        jobId = id;
    }
    
    public void setEmployer(String employer) {
        this.employer = employer;
    }
    
    public void setTitle(String jobTitle) {
        title = jobTitle;
    }
    
    public void setStartTime(Date start) {
        startTime = start;
    }
    
    public void setEndTime(Date end) {
        endTime = end;
    }
    
    public void setRoomInfo(String roomInfo) {
        room = roomInfo;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    
    public void setInterview(String name) {
        interviewer = name;
    }
    
    //=================
    //  Other Methods
    //=================
    
    public void merge(InterviewData data) {
        if (jobId != data.getJobId()) {
            return;
        }
        String employer = data.getEmployer();
        String title = data.getTitle();
        Date startTime = data.getStartTime();
        Date endTime = data.getEndTime();
        TYPE type = data.getType();
        String room = data.getRoomInfo();
        String instructions = data.getInstructions();
        String interviewer = data.getInterviewer();
        
        if (employer != null) {
            this.employer = employer;
        }
        if (title != null) {
            this.title = title;
        }
        if (startTime != null) {
            this.startTime = startTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }
        if (type != null) {
            this.type = type;
        }
        if (room != null) {
            this.room = room;
        }
        if (instructions != null) {
            this.instructions =instructions;
        }
        if (interviewer != null) {
            this.interviewer = interviewer;
        }
    }
}