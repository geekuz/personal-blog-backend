package com.personalblog.api.dto;

import com.personalblog.api.SlugFormat;
import com.personalblog.post.PostStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record PostWriteRequest(
    @NotBlank @Size(max = 160)
    @Pattern(regexp = SlugFormat.PATTERN, message = SlugFormat.MESSAGE) String slug,
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 500) String summary,
    @NotBlank String content,
    @NotNull PostStatus status,
    Instant publishedAt,
    @NotNull @Size(max = 10) List<@Valid TagInput> tags
) {}
