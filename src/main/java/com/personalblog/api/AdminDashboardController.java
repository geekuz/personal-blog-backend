package com.personalblog.api;

import com.personalblog.api.dto.AdminDashboardResponse;
import com.personalblog.api.dto.AdminPostResponse;
import com.personalblog.api.dto.PostWriteRequest;
import com.personalblog.post.AdminDashboardService;
import com.personalblog.post.AdminPostService;
import com.personalblog.media.MediaStorageService;
import com.personalblog.media.MediaUploadResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/api/v1/dashboard", produces = "application/json;charset=UTF-8")
@Validated
public class AdminDashboardController {
    private final AdminDashboardService dashboard;
    private final AdminPostService posts;
    private final MediaStorageService media;
    public AdminDashboardController(AdminDashboardService dashboard, AdminPostService posts, MediaStorageService media) {
        this.dashboard = dashboard; this.posts = posts; this.media = media;
    }
    @GetMapping public AdminDashboardResponse get() { return dashboard.dashboard(); }
    @PostMapping(value = "/posts", consumes = "application/json")
    public ResponseEntity<AdminPostResponse> create(@Valid @RequestBody PostWriteRequest request) {
        AdminPostResponse created = posts.create(request);
        return ResponseEntity.created(URI.create("/api/v1/dashboard/posts/" + created.slug())).body(created);
    }
    @GetMapping("/posts/{slug}")
    public AdminPostResponse getPost(@PathVariable @Pattern(regexp = SlugFormat.PATTERN) String slug) { return posts.get(slug); }
    @PutMapping(value = "/posts/{slug}", consumes = "application/json")
    public AdminPostResponse update(@PathVariable @Pattern(regexp = SlugFormat.PATTERN) String slug,
            @Valid @RequestBody PostWriteRequest request) { return posts.update(slug, request); }
    @DeleteMapping("/posts/{slug}")
    public ResponseEntity<Void> delete(@PathVariable @Pattern(regexp = SlugFormat.PATTERN) String slug) {
        posts.delete(slug); return ResponseEntity.noContent().build();
    }
    @PostMapping(value = "/media", consumes = "multipart/form-data")
    public MediaUploadResponse upload(@RequestPart("file") MultipartFile file) { return media.upload(file); }
}
