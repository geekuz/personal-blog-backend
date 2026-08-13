package com.personalblog.api.dto;

import java.util.List;

public final class TagResponses {
    private TagResponses() {}
    public record TagItem(String name, String slug, long postCount) {}
    public record TagList(List<TagItem> items) {}
}
