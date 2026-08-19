package com.personalblog.post;

import com.personalblog.api.dto.PostDetailResponse;
import com.personalblog.api.dto.PostPageResponse;
import com.personalblog.api.dto.PostSummaryResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository posts;
    public PostService(PostRepository posts) { this.posts = posts; }

    public PostPageResponse list(int page, int size, String query, String tag) {
        String q = query == null ? "" : query.trim();
        String normalizedTag = normalizeOptional(tag);
        if (normalizedTag != null) normalizedTag = normalizedTag.toLowerCase();
        Page<Post> result = posts.findPublished(q, normalizedTag,
            PageRequest.of(page, size, Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))));
        List<PostSummaryResponse> items = result.getContent().stream().map(this::summary).toList();
        return new PostPageResponse(items, result.getNumber(), result.getSize(), result.getTotalElements(),
            result.getTotalPages(), result.hasNext());
    }

    public PostDetailResponse get(String slug) {
        Post p = posts.findBySlugAndStatus(slug.toLowerCase(), PostStatus.PUBLISHED)
            .orElseThrow(() -> new PostNotFoundException(slug));
        return new PostDetailResponse(p.getSlug(), p.getTitle(), p.getSummary(), p.getContent(), tags(p),
            p.getPublishedAt(), p.getUpdatedAt(), readingTime(p.getContent()));
    }

    private PostSummaryResponse summary(Post p) {
        return new PostSummaryResponse(p.getSlug(), p.getTitle(), p.getSummary(), tags(p), p.getPublishedAt(), readingTime(p.getContent()));
    }
    private List<String> tags(Post p) { return p.getTags().stream().map(t -> t.getSlug()).sorted().toList(); }
    private String normalizeOptional(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    // Markdown source is split on Unicode whitespace; rounding is half-up, with a minimum of one minute.
    static int readingTime(String content) {
        long words = content == null || content.isBlank() ? 0 : content.trim().split("\\s+").length;
        return Math.max(1, (int) Math.round(words / 200.0));
    }
}
