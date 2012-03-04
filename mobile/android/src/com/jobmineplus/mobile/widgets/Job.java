package com.jobmineplus.mobile.widgets;

import java.io.IOException;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.services.JbmnplsHttpService;


public class Job {
	//===========================
	//	Static properties used
	//===========================
	
	static public enum STATE{
		CANCELLED		("Cancelled", 9),
		AVAILABLE		("Applications Avaliable", 2),
		FILLED			("Filled", 6),
		POSTED			("Posted", 1),
		SCREENED		("Screened", 4),
		RANKING_COMPLETE("Ranking Complete", 8),
		SCHEDULED		("Scheduled", 5),
		APPROVED		("Approved", 2),
		COMPLETE		("Complete", 2);
		
		public static STATE getStatefromString(String text) throws JbmnplsParsingException {
			if (text != null) {
				for (STATE b : STATE.values()) {
					if (text.equalsIgnoreCase(b.toString())) {
						return b;
					}
				}
			}
			throw new JbmnplsParsingException("State: Cannot match value '" + text + "'");
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
			priority = p;	//The higher the number, the more priority it has
		}
		private String state;
		private int priority;
	}

	static public enum STATUS{
		APPLIED		("Applied", 3),
		CANNOT_APPLY("Not Authorized to Apply", 8),
		NOT_SELECTED("Not Selected", 10),
		EMPLOYED	("Employed", 11),
		SELECTED	("Selected", 10),
		ALTERNATE	("Alternate", 5),
		BLANK		("", 0);
		
		public static STATUS getStatusfromString(String text) throws JbmnplsParsingException {
			if (text != null) {
				for (STATUS b : STATUS.values()) {
					if (text.equalsIgnoreCase(b.toString())) {
						return b;
					}
				}
			}
			throw new JbmnplsParsingException("Status: Cannot match value '" + text + "'");
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
			priority = p;	//The higher the number, the more priority it has
		}
		private String status;
		private int priority;
	}
	
	static public enum LEVEL{
		JUNIOR("Junior"),
		INTERMEDIATE("Intermediate"),
		SENIOR("Senior"),
		BACHELOR("Bachelor"),
		MASTERS("Masters"),
		PHD("Ph.D.");
		
		@Override
		public String toString() {
			return state;
		}
		private LEVEL(String s) {
			state = s;
		}
		private String state;
	}
	
	static public final String DESCR_URL_PREFIX = "https://jobmine.ccol.uwaterloo.ca/psc/SS/EMPLOYEE/WORK/c/UW_CO_STUDENTS.UW_CO_JOBDTLS?UW_CO_JOB_ID=";
	
	protected JbmnplsHttpService service;
	
	//===========================
	//	Long list of properties
	//===========================

	// Definitely write once; cannot change
	protected String id;
	protected String title;
	protected String employer;
	protected String term;
	
	// These can change
	protected STATE state = STATE.getDefault();
	protected STATUS status = STATUS.getDefault();
	protected Date lastToApply;
	protected int numApps;
	protected int openings;
	
	// Gained from job description
	protected Date openToApply;
	protected boolean gradesRequired = true;
	protected String location;
	protected String[] disciplines;
	protected LEVEL[] levels;
	protected String hiringSupport;
	protected String workSupport;
	protected String description;

	//Cannot set this
	protected String url = null;
	
	// Other booleans
	protected boolean hasRead = false;
	
	//================
	//	Constructors
	//================
	/**
	 * Applications Constructor
	 * 	As long as all the data is present in the fields (ie. no one
	 * 	hid the columns) then you should be able to fill all of these
	 * 	fields. If user has blocked columns please alert the user to
	 * 	unhide them on the website.
	 * 	Since this is the applications, it will consider that you
	 * 	have applied for this job.
	 * @param jId
	 * @param jTitle
	 * @param jEmployer
	 * @param jTerm
	 * @param jState - eg. Job.STATE.FILLED
	 * @param jStatus - eg. Job.STATUS.SCREENED
	 * @param jLastDate
	 * @param jNumApps
	 */
	public Job(String jId, String jTitle, String jEmployer, String jTerm, 
			STATE jState, STATUS jStatus, Date jLastDate, int jNumApps){
		setId(jId);
		title = jTitle;
		employer = jEmployer;
		term = jTerm;
		state = jState;
		status = jStatus;
		lastToApply = jLastDate;
		numApps = jNumApps;
		service = JbmnplsHttpService.getInstance();
	}
	
	/**
	 * Short List Constructor
	 * 	As long as all the data is present in the fields (ie. no one
	 * 	hid the columns) then you should be able to fill all of these
	 * 	fields. If user has blocked columns please alert the user to
	 * 	unhide them on the website.
	 * 	Since this is the job short list, it will consider that you
	 * 	have applied for this job.
	 * @param jId
	 * @param jTitle
	 * @param jEmployer
	 * @param jLocation
	 * @param jStatus - eg. Job.STATUS.SCREENED
	 * @param jLastDate
	 * @param jNumApps
	 */
	public Job(String jId, String jTitle, String jEmployer, String jLocation,
			STATUS jStatus, Date jLastDate, int jNumApps){
		setId(jId);
		title = jTitle;
		employer = jEmployer;
		location = jLocation;
		status = jStatus;
		lastToApply = jLastDate;
		numApps = jNumApps;
		service = JbmnplsHttpService.getInstance();
	}
	
	/**
	 * Job Search Constructor
	 * 	As long as all the data is present in the fields (ie. no one
	 * 	hid the columns) then you should be able to fill all of these
	 * 	fields. If user has blocked columns please alert the user to
	 * 	unhide them on the website.
	 * @param jId
	 * @param jTitle
	 * @param jEmployer
	 * @param jLocation
	 * @param jOpenings
	 * @param jLastDate
	 * @param jNumApps
	 */
	public Job(String jId, String jTitle, String jEmployer, String jLocation,
			int jOpenings, Date jLastDate, int jNumApps){
		setId(jId);
		title = jTitle;
		employer = jEmployer;
		location = jLocation;
		openings = jOpenings;
		lastToApply = jLastDate;
		numApps = jNumApps;
		service = JbmnplsHttpService.getInstance();
	}
	
	//==============
	//	Is Methods
	//==============
	public boolean areGradesRequired() {
		return gradesRequired;
	}
	
	public boolean hasRead() {
		return hasRead;
	}
	
	public boolean hasApplied() {
		return status != STATUS.BLANK && status != STATUS.CANNOT_APPLY;
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
	
	//=============
	//	Setters
	//=============
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
	
	//	Protected
	protected void setTitle(String jTitle) {
		title = jTitle;
	}
	
	protected void setEmployer(String jEmployer) {
		employer = jEmployer;
	}
	
	protected void setTerm(String jTerm) {
		term = jTerm;
	}
	
	protected void setId(String jId) throws IllegalArgumentException{
		if (Integer.parseInt(jId) <= 0) {
			throw new IllegalArgumentException("You cannot set an id that is negative or equal to 0.");
		}
		id = jId;
		url = Job.DESCR_URL_PREFIX + id;
	}
	
	//=============
	//	Getters
	//=============
	public String getId() {
		return id;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getEmployer() {
		return employer;
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
	
	public LEVEL[] getLevels() {
		return levels;
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
		return status.getPriority() > state.getPriority() ? status.toString() : state.toString();
	}
	
	//===========
	//	Methods
	//===========
	public boolean grabDescriptionData() {
		Document doc;
		try {
			HttpResponse response = service.get(url);
			doc = Jsoup.parse(service.getHtmlFromHttpResponse(response));
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		//TODO do the parsing here
		return true;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
