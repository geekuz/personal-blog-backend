package com.personalblog.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.personalblog.post.PostRepository;

@Service
public class MediaStorageService {
    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final int MAX_DIMENSION = 6000;
    private static final Set<String> ALLOWED_FORMATS = Set.of("JPEG", "PNG", "GIF");
    private final R2ObjectStorage storage;
    private final MediaAssetRepository assets;
    private final PostRepository posts;

    public MediaStorageService(R2ObjectStorage storage, MediaAssetRepository assets, PostRepository posts) {
        this.storage = storage;
        this.assets = assets;
        this.posts = posts;
    }

    @Transactional
    public MediaUploadResponse upload(MultipartFile file) {
        ValidatedImage image = validateFile(file);
        String objectKey = "personal-blog/" + UUID.randomUUID() + "." + image.extension();
        String url = storage.put(objectKey, image.bytes(), image.contentType());
        MediaAsset asset = new MediaAsset(UUID.randomUUID(), objectKey, url, originalFilename(file),
            image.contentType(), image.bytes().length, image.width(), image.height(), Instant.now());
        try {
            return response(assets.saveAndFlush(asset));
        } catch (RuntimeException ex) {
            try { storage.delete(objectKey); }
            catch (RuntimeException cleanup) { ex.addSuppressed(cleanup); }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<MediaUploadResponse> recent() {
        return assets.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100)).stream()
            .map(this::response).toList();
    }

    @Transactional
    public void delete(UUID id) {
        MediaAsset asset = assets.findById(id).orElseThrow(MediaAssetNotFoundException::new);
        if (posts.existsByCoverImageUrl(asset.getPublicUrl())) throw new MediaAssetInUseException();
        storage.delete(asset.getObjectKey());
        assets.delete(asset);
    }

    private ValidatedImage validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new MediaUploadException("Choose an image to upload", true);
        if (file.getSize() > MAX_BYTES) throw new MediaTooLargeException();

        byte[] bytes = read(file);
        if (bytes.length > MAX_BYTES) throw new MediaTooLargeException();

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

    private String originalFilename(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) return "image";
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isBlank()) return "image";
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private MediaUploadResponse response(MediaAsset asset) {
        return new MediaUploadResponse(asset.getId(), asset.getPublicUrl(), asset.getObjectKey(),
            asset.getOriginalFilename(), asset.getContentType(), asset.getSizeBytes(), asset.getWidth(),
            asset.getHeight(), asset.getCreatedAt());
    }

    private String extension(String format) { return "JPEG".equals(format) ? "jpg" : format.toLowerCase(Locale.ROOT); }

    private String contentType(String format) { return "JPEG".equals(format) ? "image/jpeg" : "image/" + format.toLowerCase(Locale.ROOT); }

    private record ValidatedImage(byte[] bytes, String extension, String contentType, int width, int height) {}
}
