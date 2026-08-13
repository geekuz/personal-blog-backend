package com.personalblog.api;

import com.personalblog.api.dto.*;
import com.personalblog.api.dto.TagResponses.TagList;
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
                                  @RequestParam(required = false) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "must be a lowercase kebab-case slug") String tag) {
        return posts.list(page, size, q, tag);
    }

    @GetMapping("/posts/{slug}")
    public PostDetailResponse post(@PathVariable @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "must be a lowercase kebab-case slug") String slug) {
        return posts.get(slug);
    }

    @GetMapping("/tags") public TagList tags() { return tags.list(); }
    @GetMapping("/health") public Health health() { return new Health("UP"); }
    public record Health(String status) {}
}
