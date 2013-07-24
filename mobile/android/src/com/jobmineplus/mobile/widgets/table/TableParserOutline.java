package com.jobmineplus.mobile.widgets.table;

import com.jobmineplus.mobile.widgets.Job.HEADER;

public class TableParserOutline {
    private final String tableId;
    private final HEADER[] headers;

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
