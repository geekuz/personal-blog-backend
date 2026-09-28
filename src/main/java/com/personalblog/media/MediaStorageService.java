package com.personalblog.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaStorageService {
    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final int MAX_DIMENSION = 6000;
    private static final Set<String> ALLOWED_FORMATS = Set.of("JPEG", "PNG", "GIF");
    private final R2ObjectStorage storage;

    public MediaStorageService(R2ObjectStorage storage) {
        this.storage = storage;
    }

    public MediaUploadResponse upload(MultipartFile file) {
        ValidatedImage image = validateFile(file);
        String objectKey = "personal-blog/" + UUID.randomUUID() + "." + image.extension();
        String url = storage.put(objectKey, image.bytes(), image.contentType());
        return new MediaUploadResponse(url, objectKey, image.width(), image.height());
    }

    private ValidatedImage validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new MediaUploadException("Choose an image to upload", true);
        if (file.getSize() > MAX_BYTES) throw new MediaUploadException("Image must be 5 MB or smaller", true);

        byte[] bytes = read(file);
        if (bytes.length > MAX_BYTES) throw new MediaUploadException("Image must be 5 MB or smaller", true);

        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) throw invalidImage();
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw invalidImage();
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                String format = reader.getFormatName().toUpperCase(Locale.ROOT);
                if (!ALLOWED_FORMATS.contains(format)) throw invalidImage();
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_DIMENSION || height > MAX_DIMENSION)
                    throw new MediaUploadException("Image dimensions must not exceed 6000 × 6000 pixels", true);
                return new ValidatedImage(bytes, extension(format), contentType(format), width, height);
            } finally {
                reader.dispose();
            }
        } catch (MediaUploadException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new MediaUploadException("Image could not be read", true);
        }
    }

    private byte[] read(MultipartFile file) {
        try { return file.getBytes(); }
        catch (IOException ex) { throw new MediaUploadException("Image could not be read", true); }
    }

    private MediaUploadException invalidImage() {
        return new MediaUploadException("File must be a JPEG, PNG, or GIF image", true);
    }

    private String extension(String format) { return "JPEG".equals(format) ? "jpg" : format.toLowerCase(Locale.ROOT); }

    private String contentType(String format) { return "JPEG".equals(format) ? "image/jpeg" : "image/" + format.toLowerCase(Locale.ROOT); }

    private record ValidatedImage(byte[] bytes, String extension, String contentType, int width, int height) {}
}
