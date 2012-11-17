package com.jobmineplus.mobile.widgets.table;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.jobmineplus.mobile.exceptions.HiddenColumnsException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.Job;
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
        int row = 0, position = 0;
        Object[] passedObj = new Object[columnInfo.length];
        while(position != -1 && row < INFINITE_LOOP_LIMIT) {
            int column = 0;

            // Check if there is another TD, if not we are done
            position = html.indexOf("<td", position);
            if (position == -1) { return; }

            // Skip TDs till we get to the id column
            column = columnInfo[0].getColumnNumber() - column;
            position = skipColumns(html, position, column);

            // Parse the job id of the table, if no id, then table is empty
            ParsingResult result = parseTextInTD(html, position);
            if (result.Text == "") { return; }
            position = result.Position;
            passedObj[0] = Integer.parseInt(result.Text);
            column++;

            // Parse each column info, start at 1 because index 0 is job id
            for (int i = 1; i < columnInfo.length; i++) {
                ColumnInfo entry = columnInfo[i];
                int entryCol = entry.getColumnNumber();

                // Skip TDs/columns till we get to a column we want
                position = skipColumns(html, position, entryCol - column);
                column += entryCol - column;

                // Get the text from this column
                result = parseTextInTD(html, position);
                position = result.Position;
                String text = result.Text;
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
            position = skipColumns(html, position, numOfColumns - column);

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

    // ========================
    //  Parsing Helper Methods
    // ========================

    /**
     * Pass in the HTML and the position of last search plus number of columns to skip. Uses
     * indexOf to find the <td> and skip them to move to the next column without parsing the
     * insides. Will throw if end of HTML. Throws exception when column does not exist.
     * @param html
     * @param position
     * @param numberOf
     * @return position
     */
    private int skipColumns(String html, int position, int numberOf) {
        for (int i = 0; i < numberOf; i++) {
            position = skipTag(html, "td", position);
            if (position == -1) {
                throw new JbmnplsParsingException("Cannot skip column when no columns left.");
            }
        }
        return position;
    }

    /**
     * Passes the HTML, the tag you are looking for and the position, will skip that tag
     * and forward the position in the HTML. Will throw if end of HTML.
     * @param html
     * @param tag
     * @param position
     * @return position
     */
    private int skipTag(String html, String tag, int position) {
        // Get the text inside the column
       String open = "<" + tag, closing = "</" + tag + ">";
       position = html.indexOf(open, position);
       if (position == -1) {
           throw new JbmnplsParsingException("Cannot skip tag because open " + tag + " doesnt exist.");
       }
       position = html.indexOf(closing, position);
       if (position == -1) {
           throw new JbmnplsParsingException("Cannot skip tag because closing " + tag + " doesnt exist.");
       }
       position += closing.length();
       return position;
    }

    /**
     * Gets the text inside a TD. Very customized for Jobmine web page tables.
     * Looks for the <td>, then <span> and if inside is an anchor tag <a>, then it will
     * find the text in that. Remove extra spaces and returns it.
     * Specify the HTML and its current position and it will return the position and text
     * it found. Will throw exceptions if end of HTML.
     * @param html
     * @param position
     * @return ParsingResult (which is a wrapper class of position and text)
     */
    private ParsingResult parseTextInTD(String html, int position) {
        ParsingResult result = htmlInTag(html, "td", position);
        if (result == null)  { throw new JbmnplsParsingException("Cannot find TD in html."); }
        String text = result.Text;
        position = result.Position;
        result = htmlInTag(text, "span", 0);

        // Column has text?
        if (result != null) {
            text = result.Text;
            if (text.startsWith("<a")) {
                result = htmlInTag(text, "a", 0);
                text = result.Text;
            }
            text = text.replaceAll("&nbsp;", "").trim();
        } else {
            text = "";
        }
        return new ParsingResult(text, position);
    }

    /**
     * Finds the text of the tag you are looking for in the HTML. Will update the
     * position to end of the element. If cannot find, it will return null.
     * @param html
     * @param tag
     * @param position
     * @return ParsingResult, if not found will return null
     */
    private ParsingResult htmlInTag(String html, String tag, int position) {
        // Get the text inside the column
        String open = "<" + tag, closing = "</" + tag + ">";
        int start = html.indexOf(open, position);
        if (start == -1) { return null; }
        start = html.indexOf(">", start);
        if (start == -1) { return null; }
        int end = html.indexOf(closing, start);
        if (end == -1) { return null; }
        String text = html.substring(++start, end);
        end += closing.length();
        return new ParsingResult(text, end);
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

    //=================
    //  Parsing Result
    //=================
    private class ParsingResult {
        public int Position;
        public String Text;

        public ParsingResult(String text, int position) {
            this.Position = position;
            this.Text = text;
        }
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