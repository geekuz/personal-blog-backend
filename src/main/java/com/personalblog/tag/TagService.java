package com.personalblog.tag;

import com.personalblog.api.dto.TagItem;
import com.personalblog.api.dto.TagListResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TagService {
    private final TagRepository tags;
    public TagService(TagRepository tags) { this.tags = tags; }
    public TagListResponse list() {
        return new TagListResponse(tags.findPublishedTagCounts().stream()
            .map(t -> new TagItem(t.getName(), t.getSlug(), t.getPostCount())).toList());
    }
}
