package com.jobmineplus.mobile.database.pages;

import java.util.ArrayList;

public final class PageResult {
    public ArrayList<Integer> ids;
    public long timestamp;

    public PageResult(ArrayList<Integer> list, long time) {
        this.ids = list;
        this.timestamp = time;
    }
}