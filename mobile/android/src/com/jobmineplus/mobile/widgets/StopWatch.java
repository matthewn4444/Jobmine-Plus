package com.jobmineplus.mobile.widgets;

public class StopWatch {
    private long startTime;
    private long lastTime;
    private long stopTime;

    private boolean isRunning;

    public StopWatch() {
        stopTime = startTime = lastTime = -1;
        isRunning = false;
    }

    public StopWatch(boolean startNow) {
        stopTime = startTime = lastTime = -1;
        isRunning = false;
        if (startNow) {
            start();
        }
    }

    public void start() {
        if (!isRunning) {
            restart();
        }
    }

    public void stop() {
        if (isRunning) {
            isRunning = false;
            stopTime = time();
        }
    }

    public void reset() {
        long now = time();
        if (lastTime == -1) {
            lastTime = now;
        }
        if (stopTime == -1) {
            stopTime = now;
        }
        startTime = now;
    }

    public void restart() {
        reset();
        isRunning = true;
    }

    public long lap() {
        lastTime = time();
        return elapsed();
    }

    public long last() {
        return time() - lastTime;
    }

    public long elapsed() {
        if (isRunning) {
            return time() - startTime;
        }
        return stopTime - startTime;
    }

    public void printLast() {
        System.out.println(last() + " ms since last lap");
    }

    public void printElapsed(String format) {
        System.out.println(String.format(format, elapsed()));
    }

    public void printElapsed() {
        System.out.println(elapsed() + " ms has passed");
    }

    private long time() {
        return (System.nanoTime() / 1000000);
    }
}
