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
            tag = findCurrentTag(text, 0);
            text = getTextInNextElement(text, tag, 0);
            position = holdPosition;
            return text;
        }

        text = text.replaceAll("&nbsp;", "").trim();
        position = holdPosition;
        return text;
    }


    /**
     * This finds the attribute's value inside the current element
     * @param attribute to find
     * @return the value inside the attribute or null if attribute doesnt exist
     */
    public String getAttributeInCurrentElement(String attribute) {
        int lessThan = html.lastIndexOf("<", position);
        if (lessThan == -1) {
            throw new JbmnplsParsingException("Cannot find attribute in current element");
        }
        position = lessThan;

        // See if we are in the closing tag, go to the last opening tag of the same tag
        if (html.charAt(position + 1) == '/') {
            String tag = findCurrentTag(html, position);
            position = html.lastIndexOf("<" + tag, position);
            if (position == -1) {
                throw new JbmnplsParsingException("Cannot find attribute in current element");
            }
        }

        // Find either the end of the tag or the attribute
        Pair<Integer, String> result = indexOfFirstOccurance(html, position, attribute + "=", ">");
        if (result == null) {
            throw new JbmnplsParsingException("Cannot find attribute in current element.");
        }
        if (result.second == ">") {
            return null;
        }
        int attrStart = result.first + result.second.length();

        // See what type of quotes it is using and find the other quote that surrounds the value
        Character quoteChar = html.charAt(attrStart);
        if (quoteChar != '"' && quoteChar != '\'') {
            throw new JbmnplsParsingException("Cannot find attribute in current element. (Cannot parse attribute)");
        }
        attrStart++;
        int attrEnd = html.indexOf(quoteChar, attrStart);
        if (attrEnd == -1) {
            throw new JbmnplsParsingException("Cannot find attribute in current element. (Cannot parse attribute)");
        }
        return html.substring(attrStart, attrEnd);
    }

    /**
     * Finds the text inside the current html tag
     * Like getTextInNextElement, it will recusively look for the text
     * inside the tag. The current tag is where the current position inside
     * the html the parser is using.
     * @return text inside the current element
     */
    public String getTextInCurrentElement() {
        int lessThan = html.lastIndexOf("<", position);
        if (lessThan == -1) {
            throw new JbmnplsParsingException("Cannot find text in current element");
        }
        position = lessThan;
        String tag = findCurrentTag(html, position);
        return getTextInNextElement(tag);
    }

    /**
     * Gets the tag string where the position is inside the text.
     * If you call this within text (not html tag), it will look for the parent tag
     * If you call this within a tag definition, it will find the name of that tag
     * This is not smart for complex html, you must use this with valid html
     * syntax.
     * @param text
     * @param pos
     * @return tag
     */
    private String findCurrentTag(String text, int pos) {
        int lessThan = text.lastIndexOf("<", pos);
        if (lessThan == -1 || text.length() <= lessThan + 1) {
            throw new JbmnplsParsingException("Cannot find last tag in html.");
        }
        // If captured the ending tag then skip the slash but find the tag name
        if (text.charAt(lessThan+1) == '/') {
            lessThan++;
        }
        Pair<Integer, String> result = indexOfFirstOccurance(text, lessThan, " ", ">");
        if (result == null) {
            throw new JbmnplsParsingException("Cannot find last tag in html.");
        }
        return text.substring(lessThan + 1, result.first);
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
     * Moves the position after searched text and will return the next position.
     * If you have multiple text to skip, then add them as arguments, order does matter
     * as it will go from text to next text.
     * Throws error when text is not found.
     * @param text (can be multiple)
     * @return position
     */
    public int skipText(String... textArr) {
        int index, i;
        String text;
        for (i = 0; i < textArr.length; i++) {
            text = textArr[i];
            index = html.indexOf(text, position);
            if (index == -1) {
                throw new JbmnplsParsingException("Cannot find " + text + " in html.");
            }
            position = index + text.length();
        }
        return position;
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

    /**
     * This internal function will find the first occurance of one of the specfied strings
     * passed in.
     * For example, if you pass         indexOfFirstOccurance("foo", "bar", "thing");
     * it will look in the text for each and return the position and string that appears first
     *
     * For a sentence like    "I am Matthew and I like foo and bar with thing"
     * The first occurance would be "foo" at index 21
     * @param text This is the string to search
     * @param indexFrom Like indexOf, this index is where searching starts from
     * @param strings A list of words to search
     * @return a pair of the position and found text, null if cannot find any
     */
    private Pair<Integer, String> indexOfFirstOccurance(String text, int indexFrom, String... strings) {
        int[] positions = new int[strings.length];
        int smallest = text.length();
        int smallestIndex = -1;
        for (int i = 0; i < strings.length; i++) {
            positions[i] = text.indexOf(strings[i], indexFrom);

            // Record the index if this came first
            if (positions[i] < smallest) {
                smallestIndex = i;
                smallest = positions[i];
            }
        }

        // Could not find any of the strings in the text
        if (positions[smallestIndex] == -1) {
            return null;
        }
        return new Pair<Integer, String>(positions[smallestIndex], strings[smallestIndex]);
    }

}
