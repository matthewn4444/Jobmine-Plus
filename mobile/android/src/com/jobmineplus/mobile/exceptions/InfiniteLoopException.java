package com.jobmineplus.mobile.exceptions;

public class InfiniteLoopException extends JbmnplsTableException {
    private static final long serialVersionUID = 4242497959391268456L;
    public InfiniteLoopException() {}
    public InfiniteLoopException(String message) {
        super(message);
    }
}
