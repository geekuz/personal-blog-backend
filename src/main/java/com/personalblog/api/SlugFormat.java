package com.personalblog.api;

public final class SlugFormat {
    public static final String PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$";
    public static final String MESSAGE = "must be a lowercase kebab-case slug";

    private SlugFormat() {}
}
