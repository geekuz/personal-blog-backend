package com.personalblog.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import com.personalblog.post.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class MediaStorageServiceTest {
    private final R2ObjectStorage storage = mock(R2ObjectStorage.class);
    private final MediaAssetRepository assets = mock(MediaAssetRepository.class);
    private final PostRepository posts = mock(PostRepository.class);
    private final MediaStorageService service = new MediaStorageService(storage, assets, posts);

    @Test void rejectsNonImageContentBeforeCallingStorage() {
        MediaUploadException error = assertThrows(MediaUploadException.class, () -> service.upload(
            new MockMultipartFile("file", "notes.txt", "text/plain", "not an image".getBytes())));
        assertTrue(error.isInvalidInput());
        assertEquals("File must be a JPEG, PNG, or GIF image", error.getMessage());
    }

    @Test void rejectsFilesLargerThanFiveMegabytesBeforeCallingStorage() {
        MediaTooLargeException error = assertThrows(MediaTooLargeException.class, () -> service.upload(
            new MockMultipartFile("file", "large.png", "image/png", new byte[(int) MediaStorageService.MAX_BYTES + 1])));
        assertEquals("Image must be 5 MB or smaller", error.getMessage());
    }

    @Test void uploadsValidatedImageToR2() throws Exception {
        byte[] bytes = png(2, 3);
        when(storage.put(any(String.class), eq(bytes), eq("image/png"))).thenReturn("https://media.example.com/cover.png");
        when(assets.saveAndFlush(any(MediaAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaUploadResponse response = service.upload(
            new MockMultipartFile("file", "cover.png", "image/png", bytes));

        assertEquals("https://media.example.com/cover.png", response.url());
        assertEquals(2, response.width());
        assertEquals(3, response.height());
        assertEquals("cover.png", response.originalFilename());
        assertEquals("image/png", response.contentType());
        assertEquals(bytes.length, response.sizeBytes());
        assertTrue(response.publicId().matches("personal-blog/[0-9a-f-]{36}\\.png"));
        verify(storage).put(eq(response.publicId()), eq(bytes), eq("image/png"));
    }

    @Test void deletesUnusedAssetFromR2AndCatalog() {
        UUID id = UUID.randomUUID();
        MediaAsset asset = asset(id);
        when(assets.findById(id)).thenReturn(Optional.of(asset));
        when(posts.existsByCoverImageUrl(asset.getPublicUrl())).thenReturn(false);

        service.delete(id);

        verify(storage).delete(asset.getObjectKey());
        verify(assets).delete(asset);
    }

    @Test void refusesToDeleteAssetUsedByAPost() {
        UUID id = UUID.randomUUID();
        MediaAsset asset = asset(id);
        when(assets.findById(id)).thenReturn(Optional.of(asset));
        when(posts.existsByCoverImageUrl(asset.getPublicUrl())).thenReturn(true);

        assertThrows(MediaAssetInUseException.class, () -> service.delete(id));

        verify(storage, never()).delete(any());
        verify(assets, never()).delete(any());
    }

    @Test void reportsMissingAssetDuringDelete() {
        UUID id = UUID.randomUUID();
        when(assets.findById(id)).thenReturn(Optional.empty());

        assertThrows(MediaAssetNotFoundException.class, () -> service.delete(id));
    }

    @Test void rejectsOversizedDimensionsBeforeCallingStorage() throws Exception {
        MediaUploadException error = assertThrows(MediaUploadException.class, () -> service.upload(
            new MockMultipartFile("file", "wide.png", "image/png", png(6001, 1))));
        assertTrue(error.isInvalidInput());
        assertEquals("Image dimensions must not exceed 6000 × 6000 pixels", error.getMessage());
    }

    private byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    private MediaAsset asset(UUID id) {
        return new MediaAsset(id, "personal-blog/image.png", "https://media.example.com/personal-blog/image.png",
            "image.png", "image/png", 128, 20, 10, Instant.parse("2026-09-28T12:00:00Z"));
    }
}
