package com.personalblog.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.personalblog.media.MediaTooLargeException;
import com.personalblog.media.MediaAssetInUseException;
import com.personalblog.media.MediaAssetNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest(
        "POST", "/api/v1/dashboard/media");

    @Test void mapsApplicationImageLimitToPayloadTooLarge() {
        assertTooLarge(handler.mediaTooLarge(new MediaTooLargeException(), request));
    }

    @Test void mapsMultipartParserLimitToPayloadTooLarge() {
        assertTooLarge(handler.mediaTooLarge(new MaxUploadSizeExceededException(6L * 1024 * 1024), request));
    }

    @Test void mapsMissingMediaToNotFound() {
        ResponseEntity<ApiError> response = handler.mediaNotFound(new MediaAssetNotFoundException(), request);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("MEDIA_NOT_FOUND", response.getBody().code());
    }

    @Test void mapsReferencedMediaToConflict() {
        ResponseEntity<ApiError> response = handler.mediaInUse(new MediaAssetInUseException(), request);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("MEDIA_IN_USE", response.getBody().code());
    }

    private void assertTooLarge(ResponseEntity<ApiError> response) {
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals("IMAGE_TOO_LARGE", response.getBody().code());
        assertEquals("Image must be 5 MB or smaller", response.getBody().message());
        assertEquals("/api/v1/dashboard/media", response.getBody().path());
    }
}
