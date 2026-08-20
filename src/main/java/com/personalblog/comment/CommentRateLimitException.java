package com.personalblog.comment;

public class CommentRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public CommentRateLimitException(long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
