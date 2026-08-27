package com.personalblog.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

class MediaStorageServiceTest {
    private final MediaStorageService service = new MediaStorageService(
        new MediaStorageProperties("", "", ""), RestClient.builder());

    @Test void rejectsNonImageContentBeforeCallingStorage() {
        MediaUploadException error = assertThrows(MediaUploadException.class, () -> service.upload(
            new MockMultipartFile("file", "notes.txt", "text/plain", "not an image".getBytes())));
        assertTrue(error.isInvalidInput());
        assertEquals("File must be a JPEG, PNG, or GIF image", error.getMessage());
    }

    @Test void acceptsRealImageButReportsMissingStorageConfiguration() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        MediaUploadException error = assertThrows(MediaUploadException.class, () -> service.upload(
            new MockMultipartFile("file", "cover.png", "image/png", bytes.toByteArray())));
        assertEquals("Image storage is not configured", error.getMessage());
        assertTrue(!error.isInvalidInput());
    }
}
