package com.personalblog.api.dto;

import com.personalblog.api.SlugFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TagInput(
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Size(max = 50)
    @Pattern(regexp = SlugFormat.PATTERN, message = SlugFormat.MESSAGE) String slug
) {}
