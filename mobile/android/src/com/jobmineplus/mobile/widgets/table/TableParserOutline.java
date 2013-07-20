package com.jobmineplus.mobile.widgets.table;

import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;

public class TableParserOutline {
    private final String tableId;
    private final HEADER[] headers;

    static public enum HEADER {
        // Common
        JOB_ID("job id"),
        JOB_TITLE("job title"),
        EMPLOYER("employer"),
        JOB_STATUS("job status"),

        // Applications
        UNIT("unit"),
        TERM("term"),
        APP_STATUS("app. status"),
        VIEW_DETAILS("view details"),
        LAST_DAY_TO_APPLY("last day to apply"),
        NUM_APPS("# apps"),
        VIEW_PACKAGE("view package"),

        // Interviews
        EMPLOYER_NAME("employer name"),
        DATE("date"),
        TYPE("type"),
        SELECT_TIME("select/view time"),
        START_TIME("start time"),
        END_TIME("end time"),
        LENGTH("length"),
        ROOM("room"),
        INSTRUCTIONS("instructions"),
        INTERVIEWER("interviewer"),

        // Shortlist
        JOB_IDENTIFIER("job identifier"),
        UNIT_NAME_1("unit name 1"),
        LOCATION("location"),
        APPLY("apply"),
        LAST_DATE_TO_APPLY("last date to apply"),

        BLANK("");

        public static HEADER getApplicationStatusfromString(String text)
                throws JbmnplsParsingException {
            if (text == null) {
                return null;
            }
            for (HEADER b : HEADER.values()) {
                if (text.equalsIgnoreCase(b.toString())) {
                    return b;
                }
            }
            throw new JbmnplsParsingException("Application Status: Cannot match value '" + text + "'");
        }

        @Override
        public String toString() {
            return header;
        }

        private HEADER(String s) {
            header = s;
        }

        private String header;
    }

    // ===============
    // Constructor
    // ===============
    /**
     * This should be used with final keyword to describe the table
     * @param tableId: the DOM id (eg. css #element_id)
     * @param tableHeaders: multi-arguments of column info (one per each column that is of
     *                    interest for job arguments)
     */
    public TableParserOutline(String tableId, HEADER... tableHeaders) {
        this.tableId = tableId;
        this.headers = tableHeaders;
    }

    public String getTableId() {
        return tableId;
    }

    public int columnLength() {
        return headers.length;
    }

    public HEADER[] getHeaders() {
        return headers;
    }
}
