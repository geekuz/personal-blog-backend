package com.personalblog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewsletterTokenRequest(
    @NotBlank @Size(max = 200) String token
) {}
