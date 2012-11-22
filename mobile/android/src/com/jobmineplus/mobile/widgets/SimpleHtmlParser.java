package com.jobmineplus.mobile.widgets;

import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;

public class SimpleHtmlParser {
    private int position;
    private String html;

    public SimpleHtmlParser(String html) {
        this.position = 0;
        this.html = html;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int newPosition) {
        if (html.length() <= newPosition) {
            position = -1;
        } else {
            position = newPosition;
        }
    }

    public boolean isEndOfContent() {
        return position == -1;
    }

    /**
     * Pass in the HTML and the position of last search plus number of columns to skip. Uses
     * indexOf to find the <td> and skip them to move to the next column without parsing the
     * insides. Will throw if end of HTML. Throws exception when column does not exist.
     * @param numberOf
     */
    public void skipColumns(int numberOf) {
        for (int i = 0; i < numberOf; i++) {
            skipTag("td");
            if (position == -1) {
                throw new JbmnplsParsingException("Cannot skip column when no columns left.");
            }
        }
    }

    /**
     * Passes the HTML, the tag you are looking for and the position, will skip that tag
     * and forward the position in the HTML. Will throw if end of HTML.
     * @param tag
     */
    public void skipTag(String tag) {
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
    }

    /**
     * Gets the text inside a TD. Very customized for Jobmine web page tables.
     * Looks for the <td>, then <span> and if inside is an anchor tag <a>, then it will
     * find the text in that. Remove extra spaces and returns it.
     * Specify the HTML and its current position and it will return the position and text
     * it found. Will throw exceptions if end of HTML.
     * @return String
     */
    public String getHTMLInNextTD() {
        ParsingResult result = htmlInTag(html, "td", position);
        if (result == null) {
            throw new JbmnplsParsingException("Cannot find TD in html.");
        }
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
        return text;
    }

    /**
     * Finds the text of the tag you are looking for in the HTML. Will update the
     * position to end of the element. If cannot find, it will return null.
     * @param html
     * @param tag
     * @param position
     * @return ParsingResult, if not found will return null
     */
    private ParsingResult htmlInTag(String text, String tag, int pos) {
        // Get the text inside the column
        String open = "<" + tag, closing = "</" + tag + ">";
        int start = text.indexOf(open, pos);
        if (start == -1) { return null; }
        start = text.indexOf(">", start);
        if (start == -1) { return null; }
        int end = text.indexOf(closing, start);
        if (end == -1) { return null; }
        text = text.substring(++start, end);
        end += closing.length();
        return new ParsingResult(text, end);
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

    protected void log(Object... txt) {
        String returnStr = "";
        int i = 1;
        int size = txt.length;
        if (size != 0) {
            returnStr = txt[0] == null ? "null" : txt[0].toString();
            for (; i < size; i++) {
                returnStr += ", "
                        + (txt[i] == null ? "null" : txt[i].toString());
            }
        }
        System.out.println(returnStr);
    }
}
