package com.personalblog.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class MediaStorageServiceTest {
    private final R2ObjectStorage storage = mock(R2ObjectStorage.class);
    private final MediaStorageService service = new MediaStorageService(storage);

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

        MediaUploadResponse response = service.upload(
            new MockMultipartFile("file", "cover.png", "image/png", bytes));

        assertEquals("https://media.example.com/cover.png", response.url());
        assertEquals(2, response.width());
        assertEquals(3, response.height());
        assertTrue(response.publicId().matches("personal-blog/[0-9a-f-]{36}\\.png"));
        verify(storage).put(eq(response.publicId()), eq(bytes), eq("image/png"));
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
}
