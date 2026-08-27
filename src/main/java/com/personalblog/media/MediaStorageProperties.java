package com.personalblog.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog.media.cloudinary")
public record MediaStorageProperties(String cloudName, String apiKey, String apiSecret) {
    public boolean configured() {
        return hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
