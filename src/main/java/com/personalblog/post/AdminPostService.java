package com.personalblog.post;

import com.personalblog.api.dto.AdminPostResponse;
import com.personalblog.api.dto.PostWriteRequest;
import com.personalblog.api.dto.TagInput;
import com.personalblog.tag.Tag;
import com.personalblog.tag.TagRepository;
import com.personalblog.newsletter.NewsletterDeliveryService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminPostService {
    private final PostRepository posts;
    private final TagRepository tags;
    private final NewsletterDeliveryService newsletter;

    public AdminPostService(PostRepository posts, TagRepository tags, NewsletterDeliveryService newsletter) {
        this.posts = posts;
        this.tags = tags;
        this.newsletter = newsletter;
    }

    public AdminPostResponse create(PostWriteRequest request) {
        String slug = request.slug().trim();
        if (posts.existsBySlug(slug)) throw new DuplicatePostSlugException(slug);
        Instant now = Instant.now();
        Post post = new Post(slug, request.title().trim(), request.summary().trim(), request.content(),
            optional(request.coverImageUrl()), optional(request.coverImageAlt()), request.status(),
            publishedAt(request, now), scheduledAt(request), now, resolveTags(request.tags()));
        Post saved = posts.save(post);
        if (saved.getStatus() == PostStatus.PUBLISHED) newsletter.enqueue(saved);
        return response(saved);
    }

    public AdminPostResponse update(String currentSlug, PostWriteRequest request) {
        Post post = find(currentSlug);
        boolean newlyPublished = post.getStatus() != PostStatus.PUBLISHED && request.status() == PostStatus.PUBLISHED;
        String nextSlug = request.slug().trim();
        if (!post.getSlug().equals(nextSlug) && posts.existsBySlug(nextSlug)) {
            throw new DuplicatePostSlugException(nextSlug);
        }
        Instant now = Instant.now();
        post.update(nextSlug, request.title().trim(), request.summary().trim(), request.content(),
            optional(request.coverImageUrl()), optional(request.coverImageAlt()), request.status(),
            publishedAt(request, now), scheduledAt(request), now, resolveTags(request.tags()));
        if (newlyPublished) newsletter.enqueue(post);
        return response(post);
    }

    @Transactional(readOnly = true)
    public AdminPostResponse get(String slug) { return response(find(slug)); }

    public void delete(String slug) { posts.delete(find(slug)); }

    private Post find(String slug) {
        return posts.findBySlug(slug.toLowerCase()).orElseThrow(() -> new PostNotFoundException(slug));
    }

    private Instant publishedAt(PostWriteRequest request, Instant now) {
        return request.status() == PostStatus.PUBLISHED
            ? (request.publishedAt() == null ? now : request.publishedAt())
            : null;
    }

    private Instant scheduledAt(PostWriteRequest request) {
        return request.status() == PostStatus.SCHEDULED ? request.scheduledAt() : null;
    }

    private Set<Tag> resolveTags(List<TagInput> inputs) {
        Set<String> seen = new LinkedHashSet<>();
        Set<Tag> result = new LinkedHashSet<>();
        for (TagInput input : inputs) {
            String slug = input.slug().trim();
            if (seen.add(slug)) {
                result.add(tags.findBySlug(slug).orElseGet(() -> tags.save(new Tag(input.name().trim(), slug))));
            }
        }
        return result;
    }

    private String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private AdminPostResponse response(Post post) {
        List<TagInput> responseTags = post.getTags().stream()
            .map(tag -> new TagInput(tag.getName(), tag.getSlug())).toList();
        return new AdminPostResponse(post.getId(), post.getSlug(), post.getTitle(), post.getSummary(),
            post.getContent(), post.getCoverImageUrl(), post.getCoverImageAlt(), post.getStatus(),
            post.getPublishedAt(), post.getScheduledAt(), post.getCreatedAt(), post.getUpdatedAt(), responseTags);
    }
}
