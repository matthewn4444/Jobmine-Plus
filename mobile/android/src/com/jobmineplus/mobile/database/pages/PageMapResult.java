package com.jobmineplus.mobile.database.pages;

import java.util.ArrayList;
import java.util.HashMap;

public final class PageMapResult {
    public HashMap<String, ArrayList<Integer>> idMap;
    public long timestamp;

    public PageMapResult(HashMap<String, ArrayList<Integer>> list, long time) {
        this.idMap = list;
        this.timestamp = time;
    }
}
