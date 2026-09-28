package com.personalblog.media;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Component
@EnableConfigurationProperties(MediaStorageProperties.class)
public class R2ObjectStorage {
    private final MediaStorageProperties properties;
    private final S3Client client;

    @Autowired
    public R2ObjectStorage(MediaStorageProperties properties) {
        this(properties, properties.configured() ? buildClient(properties) : null);
    }

    R2ObjectStorage(MediaStorageProperties properties, S3Client client) {
        this.properties = properties;
        this.client = client;
    }

    public String put(String objectKey, byte[] bytes, String contentType) {
        if (!properties.configured() || client == null)
            throw new MediaUploadException("Image storage is not configured", false);
        String url = publicUrl(objectKey);
        try {
            client.putObject(PutObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(objectKey)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build(), RequestBody.fromBytes(bytes));
            return url;
        } catch (MediaUploadException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new MediaUploadException("Image upload failed", ex);
        }
    }

    public void delete(String objectKey) {
        if (!properties.configured() || client == null)
            throw new MediaUploadException("Image storage is not configured", false);
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(objectKey)
                .build());
        } catch (RuntimeException ex) {
            throw new MediaUploadException("Image deletion failed", ex);
        }
    }

    private String publicUrl(String objectKey) {
        String base = properties.publicBaseUrl().strip();
        URI uri;
        try { uri = URI.create(base); }
        catch (IllegalArgumentException ex) { throw new MediaUploadException("Image storage public URL is invalid", false); }
        if (!uri.isAbsolute() || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null)
            throw new MediaUploadException("Image storage public URL is invalid", false);
        return (base.endsWith("/") ? base : base + "/") + objectKey;
    }

    private static S3Client buildClient(MediaStorageProperties properties) {
        URI endpoint = URI.create("https://" + properties.accountId() + ".r2.cloudflarestorage.com");
        return S3Client.builder()
            .endpointOverride(endpoint)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())))
            .region(Region.of("auto"))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build())
            .httpClientBuilder(UrlConnectionHttpClient.builder()
                .connectionTimeout(Duration.ofSeconds(5))
                .socketTimeout(Duration.ofSeconds(30)))
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                .apiCallAttemptTimeout(Duration.ofSeconds(35))
                .apiCallTimeout(Duration.ofSeconds(40))
                .build())
            .build();
    }

    @PreDestroy
    void close() {
        if (client != null) client.close();
    }
}
