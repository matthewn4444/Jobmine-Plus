package com.jobmineplus.mobile.widgets;

import android.view.View;

import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator.AnimatorListener;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

public class HeightAnimation implements AnimatorListener, AnimatorUpdateListener {

    // This is the default animation duration
    public static final int DEFAULT_DURATION = 200;
    public static final int DEFAULT_DELAY = 0;

    // State variables
    private boolean isRunning;
    private long duration;
    private long delay;

    // Every start of the animation will reset this
    private ValueAnimator animator;
    private View row;
    private int totalHeight;

    // Additional event listeners
    private AnimatorListener listener;

    public HeightAnimation() {
        this(DEFAULT_DURATION, DEFAULT_DELAY);
    }

    public HeightAnimation(long durationMilliseconds, long delayMilliseconds) {
        setDuration(durationMilliseconds);
        setDelay(delayMilliseconds);
        isRunning = false;
    }

    public void start(View v, boolean grow) {
        // Cancel the currently playing animation to run the next one
        if (animator != null) {
            if (isRunning) {
                animator.cancel();
            }
            animator.addUpdateListener(null);
            animator.addListener(null);
        }

        row = v;
        totalHeight = row.getHeight();

        // Init the new animator
        animator = ValueAnimator.ofInt(1, row.getHeight());
        animator.setDuration(duration);
        animator.setStartDelay(delay);
        animator.addUpdateListener(this);
        animator.addListener(this);
        if (grow) {
            animator.start();
        } else {
            animator.reverse();
        }
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

    public void setDelay(long delayMS) {
        delay = delayMS;
    }

    public long getDelay() {
        return delay;
    }

    protected View getView() {
        return row;
    }

    public void addListener(AnimatorListener l) {
        listener = l;
    }

    private void setHeightOfView(int newHeight) {
        row.getLayoutParams().height = newHeight;
        row.requestLayout();
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
        if (newHeight > 0 && newHeight <= totalHeight) {
            setHeightOfView(newHeight);
        }
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        setHeightOfView(totalHeight);
        if (listener != null) {
            listener.onAnimationEnd(animator);
        }
        row = null;
        totalHeight = 0;
        isRunning = false;
    }

    @Override
    public void onAnimationCancel(Animator animator) {
        if (listener != null) {
            listener.onAnimationCancel(animator);
        }
        row = null;
        totalHeight = 0;
        isRunning = false;
    }

    @Override
    public void onAnimationRepeat(Animator animator) {
        if (listener != null) {
            listener.onAnimationRepeat(animator);
        }
    }
}
