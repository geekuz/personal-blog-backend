package com.personalblog.post;

import com.personalblog.api.dto.PostDetailResponse;
import com.personalblog.api.dto.PostLinkResponse;
import com.personalblog.api.dto.RelatedPostResponse;
import com.personalblog.api.dto.PostPageResponse;
import com.personalblog.api.dto.PostSummaryResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {
    static final int RELATED_LIMIT = 3;
    static final int MAX_SEARCH_CANDIDATES = 500;
    private static final PageRequest SINGLE = PageRequest.of(0, 1);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
    private final PostRepository posts;
    public PostService(PostRepository posts) { this.posts = posts; }

    public PostPageResponse list(int page, int size, String query, String tag) {
        String normalizedTag = normalizeOptional(tag);
        if (normalizedTag != null) normalizedTag = normalizedTag.toLowerCase();
        SearchTerms terms = SearchTerms.parse(query);
        if (!terms.isEmpty()) return search(terms, normalizedTag, page, size);
        Page<Post> result = posts.findPublished(normalizedTag, PageRequest.of(page, size, NEWEST_FIRST));
        List<PostSummaryResponse> items = result.getContent().stream().map(p -> summary(p, null)).toList();
        return new PostPageResponse(items, result.getNumber(), result.getSize(), result.getTotalElements(),
            result.getTotalPages(), result.hasNext());
    }

    // Matching happens in the database; ranking and snippets in memory. A personal blog's matches fit easily,
    // and the candidate cap bounds the work if a very common term is searched.
    private PostPageResponse search(SearchTerms terms, String tag, int page, int size) {
        List<Post> candidates = posts.findAll(PostSearchSpecifications.publishedMatchingAll(terms, tag),
            PageRequest.of(0, MAX_SEARCH_CANDIDATES, NEWEST_FIRST)).getContent();
        // Score each post once; the stable sort keeps equal scores in the candidates' newest-first order.
        List<Post> ranked = candidates.stream()
            .map(p -> new Scored(p, SearchScorer.score(p, terms.terms())))
            .sorted(Comparator.comparingInt(Scored::score).reversed())
            .map(Scored::post)
            .toList();
        int total = ranked.size();
        int from = (int) Math.min((long) page * size, total);
        int to = Math.min(from + size, total);
        List<PostSummaryResponse> items = ranked.subList(from, to).stream()
            .map(p -> summary(p, SnippetExtractor.snippet(p.getContent(), terms.terms())))
            .toList();
        int totalPages = (total + size - 1) / size;
        return new PostPageResponse(items, page, size, total, totalPages, to < total);
    }

    public PostDetailResponse get(String slug) {
        Post p = posts.findBySlugAndStatus(slug.toLowerCase(), PostStatus.PUBLISHED)
            .orElseThrow(() -> new PostNotFoundException(slug));
        PostLinkResponse previous = first(posts.findOlderPublished(p.getPublishedAt(), p.getId(), SINGLE));
        PostLinkResponse next = first(posts.findNewerPublished(p.getPublishedAt(), p.getId(), SINGLE));
        List<RelatedPostResponse> related = posts.findRelatedPublished(p.getId(), PageRequest.of(0, RELATED_LIMIT));
        return new PostDetailResponse(p.getSlug(), p.getTitle(), p.getSummary(), p.getContent(),
            p.getCoverImageUrl(), p.getCoverImageAlt(), tags(p), p.getPublishedAt(), p.getUpdatedAt(),
            readingTime(p.getContent()), previous, next, related);
    }

    private record Scored(Post post, int score) {}

    private static <T> T first(List<T> items) { return items.isEmpty() ? null : items.getFirst(); }

    private PostSummaryResponse summary(Post p, String snippet) {
        return new PostSummaryResponse(p.getSlug(), p.getTitle(), p.getSummary(), p.getCoverImageUrl(),
            p.getCoverImageAlt(), tags(p), p.getPublishedAt(), readingTime(p.getContent()), snippet);
    }
    private List<String> tags(Post p) { return p.getTags().stream().map(t -> t.getSlug()).sorted().toList(); }
    private String normalizeOptional(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    // Markdown source is split on Unicode whitespace; rounding is half-up, with a minimum of one minute.
    static int readingTime(String content) {
        long words = content == null || content.isBlank() ? 0 : content.trim().split("\\s+").length;
        return Math.max(1, (int) Math.round(words / 200.0));
    }
}
