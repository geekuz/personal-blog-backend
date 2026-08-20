package com.personalblog.api;

import com.personalblog.api.dto.CommentListResponse;
import com.personalblog.api.dto.CommentResponse;
import com.personalblog.api.dto.CreateCommentRequest;
import com.personalblog.comment.CommentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json;charset=UTF-8")
@Validated
public class CommentController {
    private final CommentService comments;

    public CommentController(CommentService comments) { this.comments = comments; }

    @GetMapping("/posts/{slug}/comments")
    public CommentListResponse list(@PathVariable @jakarta.validation.constraints.Pattern(
        regexp = SlugFormat.PATTERN, message = SlugFormat.MESSAGE) String slug, Authentication authentication) {
        String email = authentication == null ? null : authentication.getName();
        return comments.list(slug, email);
    }

    @PostMapping(value = "/posts/{slug}/comments", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable @jakarta.validation.constraints.Pattern(
                                      regexp = SlugFormat.PATTERN, message = SlugFormat.MESSAGE) String slug,
                                  @Valid @RequestBody CreateCommentRequest request,
                                  Authentication authentication) {
        return comments.create(slug, authentication.getName(), request.body());
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID commentId, Authentication authentication) {
        comments.delete(commentId, authentication.getName());
    }
}
