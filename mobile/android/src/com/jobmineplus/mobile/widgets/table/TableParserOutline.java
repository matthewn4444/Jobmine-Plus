package com.jobmineplus.mobile.widgets.table;

public class TableParserOutline {
    private final String tableId;
    private final int numOfColumns;
    private final ColumnInfo[] columnInfo;

    // ===============
    // Constructor
    // ===============
    /**
     * This should be used with final keyword to describe the table
     * @param tableId: the DOM id (eg. css #element_id)
     * @param numOfColumns: number of expected columns, will throw an exception if failed
     *                      when executed
     * @param columnInfo: multi-arguments of column info (one per each column that is of
     *                    interest for job arguments)
     */
    public TableParserOutline(String tableId, int numOfColumns, ColumnInfo... columnInfo) {
        this.tableId = tableId;
        this.columnInfo = columnInfo;
        this.numOfColumns = numOfColumns;

        // ColumnInfo.ID must be the first one on the list
        assert columnInfo[0].getType() != ColumnInfo.ID : "TabeParser Outline is set wrong, the ColumnInfo.ID is not set first.";
    }

    public String getTableId() {
        return tableId;
    }

    public int columnLength() {
        return numOfColumns;
    }

    public ColumnInfo[] getColumnInfo() {
        return columnInfo;
    }
}
