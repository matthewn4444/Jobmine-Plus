package com.jobmineplus.mobile.widgets;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.jobmineplus.mobile.R;

import junit.framework.Assert;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

public abstract class JbmnplsAdapterBase extends ViewAdapterBase<Job> {
    private View[] currentElements;
    private View currentLayout;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public static enum HIGHLIGHTING{
        GREAT, NORMAL, BAD, WORSE
    }

    public JbmnplsAdapterBase(Activity a, int widgetResourceLayout,
            int[] viewResourceIdListInWidget, ArrayList<Job> list) {
        super(a, widgetResourceLayout, viewResourceIdListInWidget, list);
    }

    protected abstract HIGHLIGHTING setJobWidgetValues(Job item, View[] elements, View layout);

    /*
     * Helper functions to set the widget text
     */
    protected void setText(int index, String text) {
        setText(index, text, false);
    }
    protected void setText(int index, String text, boolean uppercase) {
        Assert.assertNotNull("Can only call this in setJobWidgetValues.", currentElements);
        TextView element = (TextView) currentElements[index];
        if (uppercase) {
            text = text.toUpperCase(Locale.getDefault());
        }
        element.setText(text);
    }

    protected void hide(int index) {
        Assert.assertNotNull("Can only call this in setJobWidgetValues.", currentElements);
        currentElements[index].setVisibility(View.GONE);
    }

    protected void setDate(int index, Date date) {
        setDate(index, date, null);
    }
    protected void setDate(int index, Date date, String prefix) {
        Assert.assertNotNull("Can only call this in setJobWidgetValues.", currentElements);
        String dateStr = DATE_FORMAT.format(date);
        if (prefix != null && prefix != "") {
            ((TextView)currentElements[index]).setText(prefix.trim() + " " + dateStr);
        } else {
            ((TextView)currentElements[index]).setText(dateStr);
        }
    }

    protected void setText(int index1, int index2, String text) {
        setText(index1, index2, text, false);
    }
    protected void setText(int index1, int index2, String text, boolean uppercase) {
        Assert.assertNotNull("Can only call this in setJobWidgetValues.", currentElements);
        TextView element1 = (TextView) currentElements[index1];
        TextView element2 = (TextView) currentElements[index2];
        if (uppercase) {
            text = text.toUpperCase(Locale.getDefault());
        }

        // Split the text into 2 elements, if there is no 2nd word, then hide it
        String[] textSplit = text.split(" ");
        element1.setText(textSplit[0]);
        if (textSplit.length > 1) {
            element2.setText(textSplit[1]);
        } else {
            hide(index2);
        }
    }

    private void setBackgroundFromResource(int resourceId) {
        int color = getActivity().getResources().getColor(resourceId);
        currentLayout.setBackgroundColor(color);
    }

    protected <T> boolean isOneOf(T value, T... list) {
        for (T item: list) {
            if (value.equals(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void setWidgetValues(Job item, View[] elements, View layout) {
        if (item != null) {
            currentLayout = layout;
            currentElements = elements;
            HIGHLIGHTING highlight = setJobWidgetValues(item, currentElements, layout);
            switch(highlight) {
            case GREAT:
                setBackgroundFromResource(R.color.highlight_green);
                break;
            case BAD:
                setBackgroundFromResource(R.color.highlight_red);
                break;
            case WORSE:
                setBackgroundFromResource(R.color.highlight_grey);
                break;
            default:
                setBackgroundFromResource(android.R.color.transparent);
                break;
            }
        }
    }
}
