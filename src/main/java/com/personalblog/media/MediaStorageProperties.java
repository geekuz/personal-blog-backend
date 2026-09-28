package com.personalblog.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog.media.r2")
public record MediaStorageProperties(
    String accountId,
    String accessKeyId,
    String secretAccessKey,
    String bucketName,
    String publicBaseUrl
) {
    public boolean configured() {
        return hasText(accountId) && hasText(accessKeyId) && hasText(secretAccessKey)
            && hasText(bucketName) && hasText(publicBaseUrl);
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
