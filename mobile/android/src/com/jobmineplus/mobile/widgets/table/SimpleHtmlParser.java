package com.jobmineplus.mobile.widgets.table;

import android.util.Pair;

import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;

public class SimpleHtmlParser {
    private int position;
    private String html;

    public SimpleHtmlParser(String html) {
        this.position = 0;
        this.html = html;
    }

    public SimpleHtmlParser(String html, int pos) {
        this.position = pos;
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
     * Used to find the text inside the element (goes further down the children till reaches
     * text). This is not smart enough to pick up trailing text after embedded elements or
     * the same element in the element (such as a <div> inside another <div>). Since Jobmine
     * does not do this, we do not need to waste time doing that parsing.
     * @param tag
     * @return text
     */
    public String getTextInNextElement(String tag) {
        return getTextInNextElement(html, tag, position);
    }

    /**
     * Recursively crawls an element's children and receives its text. Not smart enough for
     * major parsing but simple for Jobmine tables.
     * @param text
     * @param tag
     * @param pos
     * @return text in the child node
     */
    private String getTextInNextElement(String text, String tag, int pos) {
        Pair<Integer, String> result = htmlInTag(text, tag, pos);
        if (result == null) {
            throw new JbmnplsParsingException("Cannot find " + tag + " in html.");
        }
        int holdPosition = result.first;
        text = result.second;

        if (text.charAt(0) == '<') {
            int greaterThan = text.indexOf('>', 1);
            int space = text.indexOf(' ', 1);
            if (greaterThan == space) {
                throw new JbmnplsParsingException("Cannot find " + tag + " in html.");
            }
            greaterThan = greaterThan == -1 ? text.length() : greaterThan;
            space = space == -1 ? text.length() : space;
            tag = text.substring(1, Math.min(greaterThan, space));
            text = getTextInNextElement(text, tag, 0);
            position = holdPosition;
            return text;
        }

        text = text.replaceAll("&nbsp;", "").trim();
        position = holdPosition;
        return text;
    }

    /**
     * Gets the text inside a TD. Very customized for Jobmine web page tables.
     * Looks for the <td>, then <span> and if inside is an anchor tag <a>, then it will
     * find the text in that. Remove extra spaces and returns it.
     * Specify the HTML and its current position and it will return the position and text
     * it found. Will throw exceptions if end of HTML.
     * @return String
     */
    public String getTextInNextTD() {
        return getTextInNextElement("td");
    }

    /**
     * Finds the text of the tag you are looking for in the HTML. Will update the
     * position to end of the element. If cannot find, it will return null.
     * @param html
     * @param tag
     * @param position
     * @return Pair (of positon and text), if not found will return null
     */
    private Pair<Integer, String> htmlInTag(String text, String tag, int pos) {
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
        return new Pair<Integer, String>(end, text);
    }
}
