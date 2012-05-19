package com.jobmineplus.mobile.widgets;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Alert extends AlertDialog implements DialogInterface.OnClickListener{

    protected Builder builder;
    protected Context ctx;
    
    public Alert(Context ctx) {
        super(ctx);
        this.ctx = ctx;
        builder = new Builder(ctx);
        setButton("OK", this);
        builder.create();
    }
    
    public void onClick(DialogInterface dialog, int which) {
        dialog.cancel();
    }
    
    public void show (String message) {
        show(message, true);
    }
    
    public void show(String message, boolean cancelable) {
        setMessage(message);
        setCancelable(cancelable);
        show();
    }
}
