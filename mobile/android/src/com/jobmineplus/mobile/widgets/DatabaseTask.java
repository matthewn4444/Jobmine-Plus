package com.jobmineplus.mobile.widgets;

import android.os.AsyncTask;

public class DatabaseTask<Result> extends AsyncTask<DatabaseTask.Action, Void, Result> {
    public static enum Action {PUT, GET};

    protected IDatabaseTask<Result> callbackObj;
    private Action pendingAction;

    public DatabaseTask(IDatabaseTask<Result> obj) {
        callbackObj = obj;
        pendingAction = null;
    }

    public void executePut() {
        execute(Action.PUT);
    }

    public void executeGet() {
        execute(Action.GET);
    }

    public boolean isFinished() {
        return getStatus() == Status.FINISHED;
    }

    @Override
    protected Result doInBackground(Action... params) {
        if (params[0] == Action.PUT) {
            pendingAction = Action.PUT;
            return callbackObj.doPutTask();
        } else {
            pendingAction = Action.GET;
            return callbackObj.doGetTask();
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        callbackObj.finishedTask(result, pendingAction);
        pendingAction = null;
    }

    public interface IDatabaseTask<Result>{
        Result doPutTask();
        Result doGetTask();
        void finishedTask(Result result, DatabaseTask.Action action);
    }
}