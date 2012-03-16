package com.jobmineplus.mobile.widgets;

import android.app.Activity;
import android.app.ProgressDialog;

public abstract class ProgressDialogAsyncTaskBase<TParams, TProgress, TResult> 
                            extends JbmnplsAsyncTaskBase<TParams, TProgress, TResult>{
    private ProgressDialog progressDial;
    private String message;
    
    public ProgressDialogAsyncTaskBase(Activity activity, String dialogueMessage) {
        super(activity);
        message = dialogueMessage;
    }
    
    @Override
    public void attach(Activity activity) {
        super.attach(activity);
        if (isRunning()) {
            showProgress();
        }
    };
    
    @Override
    public void detach() {
        dismissProgress();
        super.detach();
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        showProgress();
    };
    
    @Override
    protected void onPostExecute(TResult result) {
        super.onPostExecute(result);
        dismissProgress();
    };
    
    @Override 
    protected void onCancelled() {
        super.onCancelled();
        dismissProgress();
    };
    
    public void showProgress() {
        progressDial = ProgressDialog.show(getActivity(), "", message, true);
    }
    
    public void dismissProgress() {
        if (progressDial != null && progressDial.isShowing()) {
            progressDial.dismiss();
            progressDial = null;
        }
    }

}
