package com.personalblog.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class R2ObjectStorageTest {
    private static final byte[] IMAGE = {1, 2, 3};

    @Test void reportsMissingConfigurationWithoutCallingR2() {
        S3Client client = mock(S3Client.class);
        R2ObjectStorage storage = new R2ObjectStorage(
            new MediaStorageProperties("", "", "", "", ""), client);

        MediaUploadException error = assertThrows(MediaUploadException.class,
            () -> storage.put("personal-blog/image.png", IMAGE, "image/png"));

        assertEquals("Image storage is not configured", error.getMessage());
        assertTrue(!error.isInvalidInput());
    }

    @Test void uploadsWithPublicCachingAndReturnsPublicUrl() {
        S3Client client = mock(S3Client.class);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());
        R2ObjectStorage storage = new R2ObjectStorage(properties("https://media.example.com/"), client);

        String url = storage.put("personal-blog/image.png", IMAGE, "image/png");

        assertEquals("https://media.example.com/personal-blog/image.png", url);
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture(), any(RequestBody.class));
        assertEquals("personal-blog-media", request.getValue().bucket());
        assertEquals("personal-blog/image.png", request.getValue().key());
        assertEquals("image/png", request.getValue().contentType());
        assertEquals("public, max-age=31536000, immutable", request.getValue().cacheControl());
    }

    @Test void rejectsNonHttpsPublicUrlBeforeReturningIt() {
        S3Client client = mock(S3Client.class);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());
        R2ObjectStorage storage = new R2ObjectStorage(properties("http://media.example.com"), client);

        MediaUploadException error = assertThrows(MediaUploadException.class,
            () -> storage.put("personal-blog/image.png", IMAGE, "image/png"));

        assertEquals("Image storage public URL is invalid", error.getMessage());
    }

    private MediaStorageProperties properties(String publicUrl) {
        return new MediaStorageProperties("account", "access", "secret", "personal-blog-media", publicUrl);
    }
}
