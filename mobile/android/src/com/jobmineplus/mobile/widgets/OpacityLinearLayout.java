package com.jobmineplus.mobile.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

public class OpacityLinearLayout extends LinearLayout {
    protected static final int ALPHA_MAX = 255;
    protected static final int ALPHA_PRESSED = 120;
    private int alpha = ALPHA_MAX;

    private Paint paint = new Paint();

    public OpacityLinearLayout(Context context) {
        super(context);
        init();
    }

    public OpacityLinearLayout(Context context, AttributeSet attrSet) {
        super(context, attrSet);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        // Add the pressed state
        final OpacityLinearLayout self = this;
        setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    self.setCustomAlpha(ALPHA_PRESSED);
                    LayoutParams params = (LayoutParams) self.getLayoutParams();
                    params.topMargin = params.topMargin + 2;
                    params.bottomMargin = params.bottomMargin - 2;
                    self.setLayoutParams(params);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP) {
                    self.setCustomAlpha(ALPHA_MAX);
                    LayoutParams params = (LayoutParams) self.getLayoutParams();
                    params.topMargin = params.topMargin - 2;
                    params.bottomMargin = params.bottomMargin + 2;
                    self.setLayoutParams(params);
                }
                return false;
            }
        });
    }

    public void setCustomAlpha(int alpha) {
        if (this.alpha != alpha) {
            this.alpha = alpha;
            invalidate();
        }
    }

    public int getCustomAlpha() {
        return alpha;
    }

    private int getRelativeLeft(View myView) {
        if (myView.getParent() == myView.getRootView() || myView.getParent() == this)
            return myView.getLeft();
        else
            return myView.getLeft() + getRelativeLeft((View) myView.getParent());
    }

    private int getRelativeTop(View myView) {
        if (myView.getParent() == myView.getRootView() || myView.getParent() == this)
            return myView.getTop();
        else
            return myView.getTop() + getRelativeTop((View) myView.getParent());
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        for(int index = 0; index < getChildCount(); index++ ) {
            View child  = getChildAt(index);
            child.setVisibility(View.INVISIBLE);
            child.setDrawingCacheEnabled(true);
            Bitmap bitmap = child.getDrawingCache(true);
            if (bitmap != null) {
                bitmap = Bitmap.createBitmap(bitmap);
                child.setDrawingCacheEnabled(false);

                int x = getRelativeLeft(child);
                int y = getRelativeTop(child);

                paint.setAlpha(alpha);
                canvas.drawBitmap(bitmap, x, y, paint);
            }
        }
    }
}
