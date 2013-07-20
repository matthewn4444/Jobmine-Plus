package com.jobmineplus.mobile.widgets.table;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import com.jobmineplus.mobile.exceptions.HiddenColumnsException;
import com.jobmineplus.mobile.exceptions.InfiniteLoopException;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;
import com.jobmineplus.mobile.widgets.Job.APPLY_STATUS;
import com.jobmineplus.mobile.widgets.Job.INTERVIEW_TYPE;
import com.jobmineplus.mobile.widgets.Job.STATE;
import com.jobmineplus.mobile.widgets.Job.STATUS;
import com.jobmineplus.mobile.widgets.table.TableParserOutline.HEADER;

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
public class TableParser {
    private final int INFINITE_LOOP_LIMIT = 1000;
    private OnTableParseListener listener;
    private static final SimpleDateFormat DATE_FORMAT_SPACE = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT_DASH = new SimpleDateFormat("d-MMM-yyyy", Locale.getDefault());

    //===============
    //  Constructor
    //===============
    public TableParser() {
    }

    //==================
    //  Public Methods
    //==================
    /**
     * You must attach a listener if you want to execute each outline
     * @param listener
     */
    public void setOnTableRowParse(OnTableParseListener listener) {
        this.listener = listener;
    }

    /**
     * The activities call this inside parseWebpage to help them get the
     * column data from the table.
     * This takes the raw html from the HttpResponse and gets the table
     * html from it based on the id of the table.
     * This will parse the table by getting the <tr>....</tr> code from
     * the html and serve it to parseTable.
     * Passing an outline as a backup tries to parse the html with a second
     * outline if the first one fails
     * @param html
     */
    public void execute(TableParserOutline outline, String html) {
        TableParserOutline[] outlines = {outline};
        execute(outlines, html);
    }

    public void execute(TableParserOutline[] outlines, String html) {
        boolean verifySingleOutline = outlines.length == 1;
        SimpleHtmlParser parser = new SimpleHtmlParser(html);
        String text, tableID = outlines[0].getTableId();

        int i, index, start, end, columnNum = 0;
        index = parser.skipText(tableID);

        // Set the position to the table headers
        start = parser.skipText("<th");
        end = parser.skipText("<tr");
        parser.setPosition(index);

        // Now check to see if the outline(s) is/are valid for this table
        TableParserOutline passOutline = null;
        if (verifySingleOutline) {
            // See if each header matches
            HEADER[] headers = outlines[0].getHeaders();
            for (i = 0; i < headers.length; i++) {
                text = parser.getTextInNextElement("th").toLowerCase(Locale.getDefault()).replace("  ", " ");
                if (parser.getPosition() < end) {
                    if (!text.equals(headers[i].toString())) {
                        throw new HiddenColumnsException("Outline does not match html");
                    }
                } else {
                    throw new HiddenColumnsException("Outline has more columns than html");
                }
            }
            passOutline = outlines[0];
        } else {
            ArrayList<TableParserOutline> outlinesArr = new ArrayList<TableParserOutline>(
                    Arrays.asList(outlines));

            // Decide which outline fits the given html and use that one
            while (outlinesArr.size() > 1) {
                text = parser.getTextInNextElement("th").toLowerCase(Locale.getDefault()).replace("  ", " ");;
                if (parser.getPosition() < end) { // Not enough headers in html
                    for (i = outlinesArr.size() - 1; i >= 0; i--) {
                        HEADER[] headers = outlinesArr.get(i).getHeaders();
                        if (columnNum < headers.length) { // Not enough headers in outline
                            String curHeader = headers[columnNum].toString();
                            if (!curHeader.equals(text)) {
                                outlinesArr.remove(i);
                            }
                        } else {
                            outlinesArr.remove(i);
                        }
                    }
                    columnNum++;
                } else {
                    break;
                }
            }
            if (outlinesArr.size() != 1) {
                throw new HiddenColumnsException("Cannot find a suitable outline to parse table.");
            }
            passOutline = outlinesArr.get(0);
        }

        // Get the html for the table itself
        end = parser.skipText("</table>");
        html = html.substring(start, end);

        internalExecute(passOutline, html);
    }

    private void internalExecute(TableParserOutline outline, String html) {
        if (listener == null) {
            throw new JbmnplsParsingException("You did not attach a listener to the table parsing function.");
        }

        // Parse the table now
        try {
            parseTable(outline, html);
        } catch (NumberFormatException e) {
            throw new JbmnplsParsingException(e.getMessage());
        }
    }

    /**
     * This parses the table based on the table HTML. This requires the HTML to be
     * <tr>....</tr>. The execute method in this class calls it correctly. Any errors
     * in parsing will throw JbmnplsParsingException. This is much faster than using
     * a 3rd party HTML parser because this is 4 times faster. When complete it will
     * return mid-code.
     * @param html
     */
    private void parseTable(TableParserOutline outline, String html) {
        SimpleHtmlParser parser = new SimpleHtmlParser(html);
        HEADER[] headers = outline.getHeaders();

        int row = 0;
        Object[] passedObj = new Object[outline.columnLength()];
        while(!parser.isEndOfContent() && row < INFINITE_LOOP_LIMIT) {
            // Check if there is another TD, if not we are done
            int position = html.indexOf("<tr", parser.getPosition());
            if (position == -1) { return; }
            parser.setPosition(position);

            // Parse the job id of the table, if no id, then table is empty
            String text = parser.getTextInNextTD();
            if (text == "") { return; }
            try {
                passedObj[0] = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                throw new HiddenColumnsException("Cannot get id from table.");
            }

            for (int i = 1; i < outline.columnLength(); i++) {
                text = parser.getTextInNextTD();

                // Convert the value to the column type and type
                Object value = null;
                switch(headers[i]) {
                    // Strings
                    case JOB_TITLE:
                    case EMPLOYER:
                    case EMPLOYER_NAME:
                    case UNIT:
                    case UNIT_NAME_1:
                    case TERM:
                    case ROOM:
                    case INSTRUCTIONS:
                    case INTERVIEWER:
                    case LOCATION:
                    case START_TIME:        // Handled by interviews
                    case END_TIME:          // Handled by interviews
                        value = text;
                        break;

                    // Integers
                    case NUM_APPS:
                    case LENGTH:
                        value = text == "" ? 0 : Integer.parseInt(text);
                        break;

                    case JOB_ID:
                    case JOB_IDENTIFIER:
                        if (text == "") {   // No data in table/row
                            if (row != 0) {
                                throw new JbmnplsParsingException("Cannot parse id because it is empty on row= " + row);
                            }
                            return;
                        }
                        value = text == "" ? 0 : Integer.parseInt(text);
                        break;

                    // Dates
                    case LAST_DAY_TO_APPLY:
                    case LAST_DATE_TO_APPLY:
                    case DATE:
                        if (text.equals("")) {
                            value = new Date(0);
                        } else {
                            try {
                                if (text.contains("-")) {
                                    value = DATE_FORMAT_DASH.parse(text);
                                } else {
                                    value = DATE_FORMAT_SPACE.parse(text);
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                                value = new Date(0);
                            }
                        }
                        break;

                    // Interview Type
                    case TYPE:
                        value = INTERVIEW_TYPE.getTypefromString(text);
                        break;

                    // Application Status
                    case APPLY:
                        value = APPLY_STATUS.getApplicationStatusfromString(text);
                        break;

                    // Job Status
                    case APP_STATUS:
                        value = STATUS.getStatusfromString(text);
                        break;

                    // Job State
                    case JOB_STATUS:
                        value = STATE.getStatefromString(text);
                        break;

                    // Ignore
                    case VIEW_DETAILS:
                    case VIEW_PACKAGE:
                    case SELECT_TIME:
                    case BLANK:
                        break;

                    default:
                      throw new JbmnplsParsingException(
                              "Cannot parse column with invalid type. Row= " + row + ", type= " + headers[i]);
                }
                passedObj[i] = value;
            }
            // Now we pass the values back to the activities to make jobs
            listener.onRowParse(outline, passedObj);
            row++;
        }
        // Chances are, these parts will never run
        if (row >= INFINITE_LOOP_LIMIT) {
            throw new InfiniteLoopException("We ran an infinite loop looking for column data.");
        }
        throw new JbmnplsParsingException("Went to end of table but found no information.");
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
        public void onRowParse(TableParserOutline outline, Object... jobData);
    }
}