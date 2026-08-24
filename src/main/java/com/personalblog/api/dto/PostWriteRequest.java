package com.personalblog.api.dto;

import com.personalblog.api.SlugFormat;
import com.personalblog.post.PostStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.time.Instant;
import java.util.List;

public record PostWriteRequest(
    @NotBlank @Size(max = 160)
    @Pattern(regexp = SlugFormat.PATTERN, message = SlugFormat.MESSAGE) String slug,
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 500) String summary,
    @NotBlank String content,
    @Size(max = 2048) @Pattern(regexp = "^$|^https?://\\S+$", message = "must be an HTTP or HTTPS URL") String coverImageUrl,
    @Size(max = 300) String coverImageAlt,
    @NotNull PostStatus status,
    Instant publishedAt,
    @NotNull @Size(max = 10) List<@Valid TagInput> tags
) {
    @AssertTrue(message = "cover image URL and alt text must be provided together")
    public boolean isCoverImageValid() {
        return hasText(coverImageUrl) == hasText(coverImageAlt);
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
