package com.jobmineplus.mobile.widgets;

import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;

public final class JobSearchProperties {

    // Properties
    public Property<String> disciplines1;
    public Property<String> disciplines2;
    public Property<String> disciplines3;

    public Property<String> term;
    public Property<String> location;
    public Property<FILTER> filter;
    public Property<JOBTYPE> jobType;
    public Property<String> employer;
    public Property<String> title;

    public Property<Boolean> levelJunior;
    public Property<Boolean> levelIntermediate;
    public Property<Boolean> levelSenior;
    public Property<Boolean> levelBachelors;
    public Property<Boolean> levelMasters;
    public Property<Boolean> levelPhD;

    public static enum FILTER {
        APPROVED("Approved", "APPR"),
        APPLICATIONS_AVAILABLE("Apps Avail", "APPA"),
        CANCELLED("Cancelled", "CANC"),
        POSTED("Posted", "POST");

        public static FILTER fromString(String string) {
            if (string == null) {
                return null;
            }
            for (FILTER b : FILTER.values()) {
                if (string.equalsIgnoreCase(b.toString())) {
                    return b;
                }
            }
            throw new JbmnplsParsingException("Filter: Cannot match value '" + string + "'");
        }

        private FILTER(String string, String valueCode) {
            text = string;
            code = valueCode;
        }

        public String toString() {
            return text;
        }

        public String getCode() {
            return code;
        }

        private String text;
        private String code;
    }

    public static enum JOBTYPE {
        COOP("Co-op", 1),
        COOP_ARCH("Co-op ARCH", 2),
        COOP_CA("Co-op CA", 2),
        COOP_TEACH("Co-op TEACH", 2),
        COOP_PHARM("Co-op PHARM", 9),
        COOP_UAE("Co-op UAE", 10),
        ALUMNI("Alumni", 5),
        GRADUATING("Graduating", 6),
        OTHER("Other", 7),
        SUMMER("Summer", 8);

        public static JOBTYPE fromString(String string) {
            if (string == null) {
                return null;
            }
            for (JOBTYPE b : JOBTYPE.values()) {
                if (string.equalsIgnoreCase(b.toString())) {
                    return b;
                }
            }
            throw new JbmnplsParsingException("Filter: Cannot match value '" + string + "'");
        }

        private JOBTYPE(String string, int i) {
            text = string;
            index = i;
        }

        public String toString() {
            return text;
        }

        public int getIndex() {
            return index;
        }

        private String text;
        private int index;
    }

    public JobSearchProperties() {
        disciplines1 = new Property<String>();
        disciplines2 = new Property<String>();
        disciplines3 = new Property<String>();

        term = new Property<String>();
        location = new Property<String>();
        filter = new Property<FILTER>();
        jobType = new Property<JOBTYPE>();
        employer = new Property<String>();
        title = new Property<String>();

        levelJunior = new Property<Boolean>();
        levelIntermediate = new Property<Boolean>();
        levelSenior = new Property<Boolean>();
        levelBachelors = new Property<Boolean>();
        levelMasters = new Property<Boolean>();
        levelPhD = new Property<Boolean>();
    }

    public void rejectChanges() {
        disciplines1.rejectChange();
        disciplines2.rejectChange();
        disciplines3.rejectChange();

        term.rejectChange();
        location.rejectChange();
        filter.rejectChange();
        jobType.rejectChange();
        employer.rejectChange();
        title.rejectChange();

        levelJunior.rejectChange();
        levelIntermediate.rejectChange();
        levelSenior.rejectChange();
        levelBachelors.rejectChange();
        levelMasters.rejectChange();
        levelPhD.rejectChange();
    }

    public void acceptChanges() {
        disciplines1.acceptChange();
        disciplines2.acceptChange();
        disciplines3.acceptChange();

        term.acceptChange();
        location.acceptChange();
        filter.acceptChange();
        jobType.acceptChange();
        employer.acceptChange();
        title.acceptChange();

        levelJunior.acceptChange();
        levelIntermediate.acceptChange();
        levelSenior.acceptChange();
        levelBachelors.acceptChange();
        levelMasters.acceptChange();
        levelPhD.acceptChange();
    }
}