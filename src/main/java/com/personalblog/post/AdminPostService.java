package com.personalblog.post;

import com.personalblog.api.dto.AdminPostDtos.AdminPostResponse;
import com.personalblog.api.dto.AdminPostDtos.PostWriteRequest;
import com.personalblog.api.dto.AdminPostDtos.TagInput;
import com.personalblog.tag.Tag;
import com.personalblog.tag.TagRepository;
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

    public AdminPostService(PostRepository posts, TagRepository tags) {
        this.posts = posts;
        this.tags = tags;
    }

    public AdminPostResponse create(PostWriteRequest request) {
        String slug = request.slug().trim();
        if (posts.existsBySlug(slug)) throw new DuplicatePostSlugException(slug);
        Instant now = Instant.now();
        Post post = new Post(slug, request.title().trim(), request.summary().trim(), request.content(),
            request.status(), publishedAt(request, now), now, resolveTags(request.tags()));
        return response(posts.save(post));
    }

    public AdminPostResponse update(String currentSlug, PostWriteRequest request) {
        Post post = find(currentSlug);
        String nextSlug = request.slug().trim();
        if (!post.getSlug().equals(nextSlug) && posts.existsBySlug(nextSlug)) {
            throw new DuplicatePostSlugException(nextSlug);
        }
        Instant now = Instant.now();
        post.update(nextSlug, request.title().trim(), request.summary().trim(), request.content(),
            request.status(), publishedAt(request, now), now, resolveTags(request.tags()));
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

    private AdminPostResponse response(Post post) {
        List<TagInput> responseTags = post.getTags().stream()
            .map(tag -> new TagInput(tag.getName(), tag.getSlug())).toList();
        return new AdminPostResponse(post.getId(), post.getSlug(), post.getTitle(), post.getSummary(),
            post.getContent(), post.getStatus(), post.getPublishedAt(), post.getCreatedAt(),
            post.getUpdatedAt(), responseTags);
    }
}
