package com.jobmineplus.mobile.widgets;

/**
 * Property class holds data of current and old data
 * This is mainly for seeing if properties change and users
 * should mark accept or reject changes when new data is set
 * @author Matthew
 *
 * @param <T> data type
 */
public class Property<T> {
    private boolean hasChanged = false;
    private T data;
    private T oldData;

    public Property() {
    }

    public Property(T value) {
        set(value);
        acceptChange();
    }

    /**
     * When new data is set, data has changed
     * @return
     */
    public boolean hasChanged() {
        return hasChanged;
    }

    /**
     * Accepting the change gets rid of the old data
     * and change flag is gone
     */
    public void acceptChange() {
        hasChanged = false;
        oldData = null;
    }

    /**
     *  Say that we have accepted the new data
     *  but want the status to have changed
     */
    public void updateChange() {
        oldData = data;
    }

    /**
     * Rejecting the change will revert back to the
     * old data
     */
    public void rejectChange() {
        hasChanged = false;
        data = oldData;
        oldData = null;
    }

    /**
     * Setting new data
     * If the data is the same, change flag does not run
     * @param value
     */
    public void set(T value) {
        if (value != data && !value.equals(data)) {
            oldData = data;
            data = value;
            hasChanged = true;
        }
    }

    public T get() {
        return data;
    }

    public T getOldData() {
        return oldData;
    }
}
