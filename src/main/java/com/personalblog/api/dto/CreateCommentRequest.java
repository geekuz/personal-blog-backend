package com.personalblog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
    @NotBlank(message = "must not be blank")
    @Size(max = 2000, message = "must be at most 2000 characters")
    String body
) {}
