package com.personalblog.post;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(String slug) { super("Published post not found: " + slug); }
}
