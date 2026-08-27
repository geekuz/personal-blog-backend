package com.personalblog.media;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
@EnableConfigurationProperties(MediaStorageProperties.class)
public class MediaStorageService {
    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final int MAX_DIMENSION = 6000;
    private final MediaStorageProperties properties;
    private final RestClient restClient;

    public MediaStorageService(MediaStorageProperties properties, RestClient.Builder restClient) {
        this.properties = properties;
        this.restClient = restClient.build();
    }

    public MediaUploadResponse upload(MultipartFile file) {
        validateFile(file);
        if (!properties.configured()) throw new MediaUploadException("Image storage is not configured", false);
        long timestamp = Instant.now().getEpochSecond();
        String folder = "personal-blog";
        String signature = sha1("folder=" + folder + "&timestamp=" + timestamp + properties.apiSecret());
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", resource(file)).filename(safeFilename(file));
        body.part("api_key", properties.apiKey());
        body.part("timestamp", Long.toString(timestamp));
        body.part("folder", folder);
        body.part("signature", signature);
        try {
            Map<?, ?> response = restClient.post()
                .uri("https://api.cloudinary.com/v1_1/{cloud}/image/upload", properties.cloudName())
                .contentType(MediaType.MULTIPART_FORM_DATA).body(body.build()).retrieve().body(Map.class);
            if (response == null) throw new MediaUploadException("Image storage returned an empty response", false);
            return new MediaUploadResponse(value(response, "secure_url"), value(response, "public_id"),
                number(response, "width"), number(response, "height"));
        } catch (MediaUploadException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MediaUploadException("Image upload failed", ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new MediaUploadException("Choose an image to upload", true);
        if (file.getSize() > MAX_BYTES) throw new MediaUploadException("Image must be 5 MB or smaller", true);
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) throw new MediaUploadException("File must be a JPEG, PNG, or GIF image", true);
            if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION)
                throw new MediaUploadException("Image dimensions must not exceed 6000 × 6000 pixels", true);
        } catch (IOException ex) {
            throw new MediaUploadException("Image could not be read", true);
        }
    }

    private ByteArrayResource resource(MultipartFile file) {
        try { return new ByteArrayResource(file.getBytes()); }
        catch (IOException ex) { throw new MediaUploadException("Image could not be read", true); }
    }

    private String safeFilename(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name == null || name.isBlank() ? "image" : name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String sha1(String input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-1 is unavailable", ex); }
    }

    private String value(Map<?, ?> response, String key) {
        Object value = response.get(key);
        if (!(value instanceof String text) || text.isBlank()) throw new MediaUploadException("Image storage response was incomplete", false);
        return text;
    }

    private int number(Map<?, ?> response, String key) {
        Object value = response.get(key);
        if (!(value instanceof Number number)) throw new MediaUploadException("Image storage response was incomplete", false);
        return number.intValue();
    }
}
