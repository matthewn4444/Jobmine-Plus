package com.jobmineplus.mobile.widgets.table;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.jobmineplus.mobile.exceptions.HiddenColumnsException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.Job;
import com.jobmineplus.mobile.widgets.SimpleHtmlParser;
import com.jobmineplus.mobile.widgets.StopWatch;

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
    private final int INFINITE_LOOP_LIMIT = 300;

    private final String tableId;
    private final int numOfColumns;
    private final ColumnInfo[] columnInfo;
    private OnTableParseListener listener;

    //===============
    //  Constructor
    //===============
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

        // ColumnInfo.ID must be the first one on the list
        assert columnInfo[0].getType() != ColumnInfo.ID :
            "TabeParsingOutline is set wrong, the ColumnInfo.ID is not set first.";
    }

    //==================
    //  Public Methods
    //==================
    /**
     * You must attach a listener if you want to execute each outline
     * @param listener
     */
    public void setOnTableRowParse (OnTableParseListener listener) {
        this.listener = listener;
    }

    /**
     * The activities call this inside parseWebpage to help them get the
     * column data from the table.
     * This takes the raw html from the HttpResponse and gets the table
     * html from it based on the id of the table.
     * This will parse the table by getting the <tr>....</tr> code from
     * the html and serve it to parseTable.
     * @param html
     */
    public void execute(String html) {
        if (listener == null) {
            throw new JbmnplsParsingException(
                    "You did not attach a listener to the table parsing function.");
        }

        StopWatch sw = new StopWatch(true);

        // Grab the table data, variations of quotes
        int start = html.indexOf("id='" + tableId + "'");
        if (start == -1) {
            start = html.indexOf("id=\"" + tableId + "\"");
            if (start == -1) { throw new JbmnplsParsingException("Cannot find id in html."); }
        }
        int thStart = html.indexOf("<th", start);
        if (start == -1) { throw new JbmnplsParsingException("There is no table on this webpage."); }
        int thEnd = html.indexOf("<tr", thStart);
        String headers = html.substring(thStart, thEnd);

        // Check to see if we have the correct number of headers
        if (countText(headers, "</th>") !=  numOfColumns) {
            throw new HiddenColumnsException();
        }

        int end = html.indexOf("</table>", thEnd);
        if (end == -1) { throw new JbmnplsParsingException("Cannot find end of table in html."); }
        html =  html.substring(thEnd, end);

        // Parse the table now
        parseTable(html);

        sw.printElapsed();
    }

    /**
     * This parses the table based on the table HTML. This requires the HTML to be
     * <tr>....</tr>. The execute method in this class calls it correctly. Any errors
     * in parsing will throw JbmnplsParsingException. This is much faster than using
     * a 3rd party HTML parser because this is 4 times faster. When complete it will
     * return mid-code.
     * @param html
     */
    private void parseTable(String html) {
        SimpleHtmlParser parser = new SimpleHtmlParser(html);
        int row = 0;
        Object[] passedObj = new Object[columnInfo.length];
        while(!parser.isEndOfContent() && row < INFINITE_LOOP_LIMIT) {
            int column = 0;

            // Check if there is another TD, if not we are done
            int position = html.indexOf("<td", parser.getPosition());
            if (position == -1) { return; }
            parser.setPosition(position);

            // Skip TDs till we get to the id column
            column = columnInfo[0].getColumnNumber() - column;
            parser.skipColumns(column);

            // Parse the job id of the table, if no id, then table is empty
            String text = parser.getTextInNextTD();
            if (text == "") { return; }
            passedObj[0] = Integer.parseInt(text);
            column++;

            // Parse each column info, start at 1 because index 0 is job id
            for (int i = 1; i < columnInfo.length; i++) {
                ColumnInfo entry = columnInfo[i];
                int entryCol = entry.getColumnNumber();

                // Skip TDs/columns till we get to a column we want
                parser.skipColumns(entryCol - column);
                column += entryCol - column;

                // Get the text from this column
                text = parser.getTextInNextTD();
                column++;

                // Convert the value to the column type
                Object value = null;
                switch (entry.getType()) {
                  case ColumnInfo.TEXT:
                      value = text;
                      break;
                  case ColumnInfo.DATE:
                      try {
                          value = new SimpleDateFormat(entry.getDateFormat(),
                              Locale.ENGLISH).parse(text);
                      } catch(ParseException e) {
                          value = new Date();
                      }
                      break;
                  case ColumnInfo.DOUBLE:
                      value = text == "" ? 0 : Double.parseDouble(text);
                      break;
                  case ColumnInfo.NUMERIC:
                      value = text == "" ? 0 : Integer.parseInt(text);
                      break;
                  case ColumnInfo.STATE:
                      value = Job.STATE.getStatefromString(text);
                      break;
                  case ColumnInfo.STATUS:
                      value = Job.STATUS.getStatusfromString(text);
                      break;
                  case ColumnInfo.INTERVIEW_TYPE:
                      value = Job.INTERVIEW_TYPE.getTypefromString(text);
                      break;
                  case ColumnInfo.ID:
                      throw new JbmnplsParsingException("Cannot have duplicated job id columns.");
                  default:
                      throw new JbmnplsParsingException(
                              "Cannot parse column with invalid type. Row=" + row);
                  }
                  passedObj[i] = value;
            }

            // Skip the html till we get to the next row
            parser.skipColumns(numOfColumns - column);

            // Now we pass the values back to the activities to make jobs
            listener.onRowParse(this, passedObj);
            row++;
        }

        // Chances are, these parts will never run
        if (row >= INFINITE_LOOP_LIMIT) {
            throw new JbmnplsParsingException("We ran an infinite loop looking for column data.");
        }
        throw new JbmnplsParsingException("Went to end of table but found no information.");
    }

    /**
     * Specify the text and what you want to find and it will count that and return it.
     * Does not count inclusively.
     * @param text
     * @param findStr
     * @return number of text found
     */
    private int countText(String text, String findStr) {
        int i = 0, counter = 0;
        while(i != -1) {
            if ((i = text.indexOf(findStr, i + findStr.length())) != -1) {
                counter++;
            }
        }
        return counter;
    }

    //=============
    //  Interface
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