package com.personalblog.api.dto;

import com.personalblog.post.PostStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminPostDtos {
    private AdminPostDtos() {}

    public record TagInput(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "must be a lowercase kebab-case slug") String slug) {}

    public record PostWriteRequest(
        @NotBlank @Size(max = 160)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "must be a lowercase kebab-case slug") String slug,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 500) String summary,
        @NotBlank String content,
        @NotNull PostStatus status,
        Instant publishedAt,
        @NotNull @Size(max = 10) List<@Valid TagInput> tags) {}

    public record AdminPostResponse(UUID id, String slug, String title, String summary, String content,
                                    PostStatus status, Instant publishedAt, Instant createdAt,
                                    Instant updatedAt, List<TagInput> tags) {}
}
