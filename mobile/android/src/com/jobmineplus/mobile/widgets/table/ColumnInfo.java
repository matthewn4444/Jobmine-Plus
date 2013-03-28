package com.jobmineplus.mobile.widgets.table;

import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;

//====================
//    ColumnInfo Class
//====================
/**
 * Information for each column
 * @author matthewn4444
 */
public class ColumnInfo {
    final public static int ID              = 0;
    final public static int TEXT            = 1;
    final public static int DATE            = 2;
    final public static int NUMERIC         = 3;
    final public static int DOUBLE          = 4;
    final public static int STATE           = 5;
    final public static int STATUS          = 6;
    final public static int APPLY_STATUS    = 7;
    final public static int INTERVIEW_TYPE  = 8;

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
        if (type < 0 || type > INTERVIEW_TYPE) {
            throw new JbmnplsParsingException("Setting the column type is invald.");
        }
        if (type == DATE) {
            throw new JbmnplsParsingException("You have used the wrong constructor to set the date.");
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
        if (type < 0 || type > STATE) {
            throw new JbmnplsParsingException("Setting the column type is invald.");
        }
        if (type == DATE && dateFormat == null) {
            throw new JbmnplsParsingException("Date is invalid without specifying the dateformat.");
        }
        this.type = type;
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

