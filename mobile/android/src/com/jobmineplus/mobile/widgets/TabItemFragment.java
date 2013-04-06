package com.jobmineplus.mobile.widgets;

import junit.framework.Assert;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public abstract class TabItemFragment extends Fragment {
    private int[] viewIds;
    private int resourceId;

    public abstract void setValues(View[] views);

    protected void init(int resourceView, int[] resourceElementIds) {
        resourceId = resourceView;
        viewIds = resourceElementIds;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Assert.assertNotSame("Did not provide a resource id, please use init",
                resourceId, null);
        return inflater.inflate(resourceId, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Assert.assertNotSame("Did not provide a resource ids for elements, please use init",
                viewIds, null);
        View v = getView();
        View[] views = new View[viewIds.length];
        int i = 0;
        for (int id : viewIds) {
            views[i++] = v.findViewById(id);
        }
        setValues(views);
        super.onActivityCreated(savedInstanceState);
    }
}
