package com.jobmineplus.mobile.widgets.table;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.jobmineplus.mobile.exceptions.HiddenColumnsException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.Job;

/**
 * For this class, you need to have one for each table you are parsing
 * You should declare an object of this class final. To parse the job
 * data you must use ColumnInfo to specify each column's data of interest
 * by their type.
 * DO NOT include id as the first column as all tables have that 
-    * and it is redundant to declare it all the time. The id will be gained
 * from the constructor column
 * @author matthewn4444
 *
 */
public class TableParsingOutline {
    private String tableId;
    private int numOfColumns;
    private int jobIdColumn;
    private ColumnInfo[] columnInfo;
    private OnTableParseListener listener;
    
    //===============
    //    Constructor
    //===============
    /**
     * This should be used with final keyword to describe the table
     * @param tableId: the DOM id (eg. css #element_id)
     * @param numOfColumns: number of expected columns, will throw an exception if failed
     *                      when executed
     * @param jobIdColumn: this is the column number of where the job id is; usually column 0                      
     * @param columnInfo: multi-arguments of column info (one per each column that is of 
     *                    interest for job arguments)
     */
    public TableParsingOutline(String tableId, int numOfColumns, int jobIdColumn, ColumnInfo ...columnInfo) {
        this.tableId = tableId;
        this.columnInfo = columnInfo;
        this.jobIdColumn = jobIdColumn;
        this.numOfColumns = numOfColumns;
    }
    
    //==================
    //    Public Methods
    //==================
    /**
     * You must attach a listener if you want to execute each outline
     * @param listener
     */
    public void setOnTableRowParse (OnTableParseListener listener) {
        this.listener = listener;
    }
    
    public void execute(Document doc) {
        if (listener == null) {
            throw new JbmnplsParsingException("You did not attach a listener to the table parsing function.");
        }
        
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
        
        long a = new Date().getTime();
        for (int r = 1; r < rowLength; r++) {
            Element rowEl = rows.get(r);
            Elements tds = rowEl.getElementsByTag("td");
            
            // See if table is empty
            int id = getIntFromElement(tds.get(jobIdColumn));
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
                case ColumnInfo.INTERVIEW_TYPE:
                    value = Job.INTERVIEW_TYPE.getTypefromString(getTextFromElement(td));
                    break;
                default: 
                    throw new JbmnplsParsingException("Cannot parse column with invalid type.");
                }
                passedObj[c + 1] = value;
            }
            listener.onRowParse(this, passedObj);
        }
        long b = new Date().getTime();
        System.out.println((b -a)  + " msec");
    }

    // ==================================
    // Getting Items from Table Cell
    // ==================================
    protected Element parseTableById(Document doc, String id)
            throws JbmnplsParsingException {
        try {
            return doc.getElementById(id).select("tr:eq(1) table table")
                    .first();
        } catch (Exception e) {
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
        
    //=============
    //    Interface
    //=============
    public interface OnTableParseListener {
        /**
         * This is needed once you parse each row
         * Parameter 1 will always be the job id, everything else will follow just 
         * the same as the table columns
         * @param data: the parameters for a job shown from TableParsingOutline class
         */
        public void onRowParse(TableParsingOutline outline, Object ...jobData);
    }
}