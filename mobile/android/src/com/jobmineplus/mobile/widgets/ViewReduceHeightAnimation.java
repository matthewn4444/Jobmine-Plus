package com.jobmineplus.mobile.widgets;

import android.view.View;

import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator.AnimatorListener;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

public class ViewReduceHeightAnimation implements AnimatorListener, AnimatorUpdateListener {

    // This is the default animation duration
    public static final int DEFAULT_DURATION = 200;

    // State variables
    private boolean isRunning;
    private long duration;

    // Every start of the animation will reset this
    private ValueAnimator animator;
    private View row;
    private int initialHeight;

    // Additional event listeners
    private AnimatorListener listener;

    public ViewReduceHeightAnimation() {
        this(DEFAULT_DURATION);
    }

    public ViewReduceHeightAnimation(long durationMilliseconds) {
        setDuration(durationMilliseconds);
        duration = durationMilliseconds;
        isRunning = false;
    }

    public void start(View v) {
        // Cancel the currently playing animation to run the next one
        if (animator != null) {
            if (isRunning) {
                animator.cancel();
            }
            animator.addUpdateListener(null);
            animator.addListener(null);
        }

        row = v;
        initialHeight = row.getHeight();

        // Init the new animator
        animator = ValueAnimator.ofInt(initialHeight, 1);
        animator.setDuration(duration);
        animator.addUpdateListener(this);
        animator.addListener(this);
        animator.start();
        isRunning = true;
    }

    public void cancel() {
        if (animator != null) {
            animator.cancel();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setDuration(long durationMS) {
        if (animator != null) {
            animator.setDuration(durationMS);
        }
        duration = durationMS;
    }

    public long getDuration() {
        return duration;
    }

    protected View getView() {
        return row;
    }

    public void addListener(AnimatorListener l) {
        listener = l;
    }

    @Override
    public void onAnimationStart(Animator animator) {
        if (listener != null) {
            listener.onAnimationStart(animator);
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animator) {
        int newHeight = (Integer)animator.getAnimatedValue();
        if (newHeight > 0) {
            row.getLayoutParams().height = newHeight;
            row.requestLayout();
        }
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        row.getLayoutParams().height = initialHeight;
        row.requestLayout();
        if (listener != null) {
            listener.onAnimationEnd(animator);
        }
        row = null;
        isRunning = false;
    }

    @Override
    public void onAnimationCancel(Animator animator) {
        if (listener != null) {
            listener.onAnimationCancel(animator);
        }
    }

    @Override
    public void onAnimationRepeat(Animator animator) {
        if (listener != null) {
            listener.onAnimationRepeat(animator);
        }
    }
}
