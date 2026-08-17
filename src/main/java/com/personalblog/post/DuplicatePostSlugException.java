package com.personalblog.post;

public class DuplicatePostSlugException extends RuntimeException {
    public DuplicatePostSlugException(String slug) { super("Post slug already exists: " + slug); }
}
