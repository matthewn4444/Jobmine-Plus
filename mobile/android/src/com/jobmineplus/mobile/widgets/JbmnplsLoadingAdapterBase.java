package com.jobmineplus.mobile.widgets;

import java.util.ArrayList;
import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;

public abstract class JbmnplsLoadingAdapterBase extends JbmnplsAdapterBase {

    private boolean showLoading = false;
    private final ArrayList<ViewGroup> listRowItems;

    public JbmnplsLoadingAdapterBase(Activity a, int widgetResourceLayout,
            int[] viewResourceIdListInWidget, ArrayList<Job> list) {
        super(a, widgetResourceLayout, viewResourceIdListInWidget, list);
        listRowItems = new ArrayList<ViewGroup>();
    }

    public void showLoadingAtEnd(boolean flag) {
        showLoading = flag;

        if (!flag) {
            // Do not show it anymore, get rid of the null value
            for (int i = entries.size() - 1; i >= 0; i--) {
                if (entries.get(i) == null) {
                    entries.remove(i);
                    break;
                }
            }

            // Make sure all the rows do not have any loading animation on them
            for (int i = 0; i < listRowItems.size(); i++) {
                ViewGroup item = listRowItems.get(i);
                for (int j = 0; j < item.getChildCount() - 1; j++) {
                    item.getChildAt(j).setVisibility(View.VISIBLE);
                }
                item.getChildAt(item.getChildCount() - 1).setVisibility(View.GONE);
            }
            notifyDataSetChanged();
        }
    }

    public boolean canShowLoading() {
        return showLoading;
    }

    @Override
    public void notifyDataSetChanged() {
        if (canShowLoading()) {
            // Before updating, remove the null entry which is the loading item
            for (int i = entries.size() - 1; i >= 0; i--) {
                if (entries.get(i) == null) {
                    entries.remove(i);
                    break;
                }
            }

            // After updating the data set, add the loading symbol to the bottom
            if (!entries.isEmpty()) {
                entries.add(null);
            }
        }
        super.notifyDataSetChanged();
    }

    @Override
    protected void onCreateListItem(int position, View item, ViewGroup parent) {
        super.onCreateListItem(position, item, parent);

        if (canShowLoading()) {
            ViewGroup group = (ViewGroup) item;

            // Layouts
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

            LinearLayout layout = new LinearLayout(getActivity());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setLayoutParams(layoutParams);
            layout.setPadding(5, 5, 5, 5);
            layout.setOnClickListener(null);

            ProgressBar bar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyle);
            bar.setIndeterminate(true);
            progressParams.gravity = Gravity.CENTER_HORIZONTAL;
            layout.addView(bar);
            bar.setLayoutParams(progressParams);
            group.addView(layout);

            listRowItems.add(group);
        }
    }

    @Override
    protected void setWidgetValues(int position, Job item, View[] elements,
            View layout) {
        super.setWidgetValues(position, item, elements, layout);

        if (canShowLoading()) {
            // Set the last item on the list as the loading symbol
            ViewGroup lay = (ViewGroup) layout;
            View lastChild = lay.getChildAt(lay.getChildCount() - 1);
            if (item == null && position == entries.size() - 1) {
                for (int i = 0; i < lay.getChildCount() - 1; i++) {
                    lay.getChildAt(i).setVisibility(View.GONE);
                }
                lastChild.setVisibility(View.VISIBLE);
            } else {
                for (int i = 0; i < lay.getChildCount() - 1; i++) {
                    lay.getChildAt(i).setVisibility(View.VISIBLE);
                }
                lastChild.setVisibility(View.GONE);
            }
        }
    }
}
