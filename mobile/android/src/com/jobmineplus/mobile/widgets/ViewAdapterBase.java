package com.jobmineplus.mobile.widgets;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;


public abstract class ViewAdapterBase<TItem> extends ArrayAdapter<TItem>{
    private Activity activity;
    private ArrayList<TItem> entries;
    private int widgetLayout;
    private int[] resources;

    public ViewAdapterBase(Activity a, int listViewResourceId, int widgetResourceLayout, int[] viewResourceIdListInWidget, ArrayList<TItem> list) {
        super(a, listViewResourceId, list);
        entries = list;
        activity = a;
        resources = viewResourceIdListInWidget;
        widgetLayout = widgetResourceLayout;
    }

    protected abstract void setWidgetValues(TItem item, View[] elements);

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View viewObj = convertView;
        View[] elements = null;
        if (viewObj == null) {
            LayoutInflater inflator = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            viewObj = inflator.inflate(widgetLayout, null);
            int size = resources.length;
            elements = new View[size];
            for (int i = 0; i < size; i++) {
                elements[i] = viewObj.findViewById(resources[i]);
            }
            viewObj.setTag(elements);
        } else {
            elements = (View[]) viewObj.getTag();
        }
        final TItem item = entries.get(position);
        for (View v: elements) {
            v.setVisibility(View.VISIBLE);
        }
        setWidgetValues(item, elements);
        return viewObj;
    }
}
