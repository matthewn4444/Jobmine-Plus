package com.jobmineplus.mobile.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

public class ListViewPlus extends ListView {

    private OnVisualRowChangeListener mListener;

    public interface OnVisualRowChangeListener {
        public void onVisuallyAddedRows(ListView listView);
        public void onVisuallyRemovedRows(ListView listView);
    }

    public ListViewPlus(Context context) {
        super(context);
    }

    public ListViewPlus(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListViewPlus(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnRowChangeListener(OnVisualRowChangeListener listener) {
        mListener = listener;
    }

    public View getLastChild() {
        return getChildCount() > 0 ? getChildAt(getChildCount() - 1) : null;
    }

    public View getFirstChild() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int beforeChildCount = getChildCount();

        super.onLayout(changed, l, t, r, b);

        if (mListener != null) {
            int diff = getChildCount() - beforeChildCount;
            if (diff < 0) {
                mListener.onVisuallyRemovedRows(this);
            } else if (diff > 0) {
                mListener.onVisuallyAddedRows(this);
            }
        }
    }
}
