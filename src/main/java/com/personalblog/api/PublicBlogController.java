package com.personalblog.api;

import com.personalblog.api.dto.HealthResponse;
import com.personalblog.api.dto.PostDetailResponse;
import com.personalblog.api.dto.PostPageResponse;
import com.personalblog.api.dto.TagListResponse;
import com.personalblog.post.PostService;
import com.personalblog.tag.TagService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json;charset=UTF-8")
@Validated
public class PublicBlogController {
    private final PostService posts;
    private final TagService tags;
    public PublicBlogController(PostService posts, TagService tags) { this.posts = posts; this.tags = tags; }

    @GetMapping("/posts")
    public PostPageResponse posts(@RequestParam(defaultValue = "0") @Min(0) int page,
                                  @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(required = false) @Pattern(regexp = SlugFormat.PATTERN, message = SlugFormat.MESSAGE) String tag) {
        return posts.list(page, size, q, tag);
    }

    @GetMapping("/posts/{slug}")
    public PostDetailResponse post(@PathVariable @Pattern(regexp = SlugFormat.PATTERN, message = SlugFormat.MESSAGE) String slug) {
        return posts.get(slug);
    }

    @GetMapping("/tags")
    public TagListResponse tags() {
        return tags.list();
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP");
    }
}
