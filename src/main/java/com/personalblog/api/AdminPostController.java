package com.personalblog.api;

import com.personalblog.api.dto.AdminPostResponse;
import com.personalblog.api.dto.PostWriteRequest;
import com.personalblog.post.AdminPostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/admin/posts", produces = "application/json;charset=UTF-8")
@Validated
public class AdminPostController {
    private final AdminPostService posts;

    public AdminPostController(AdminPostService posts) { this.posts = posts; }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<AdminPostResponse> create(@Valid @RequestBody PostWriteRequest request) {
        AdminPostResponse created = posts.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/posts/" + created.slug())).body(created);
    }

    @GetMapping("/{slug}")
    public AdminPostResponse get(@PathVariable @Pattern(regexp = SlugFormat.PATTERN) String slug) {
        return posts.get(slug);
    }

    @PutMapping(value = "/{slug}", consumes = "application/json")
    public AdminPostResponse update(@PathVariable @Pattern(regexp = SlugFormat.PATTERN) String slug,
                                    @Valid @RequestBody PostWriteRequest request) {
        return posts.update(slug, request);
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable @Pattern(regexp = SlugFormat.PATTERN) String slug) {
        posts.delete(slug);
        return ResponseEntity.noContent().build();
    }
}
