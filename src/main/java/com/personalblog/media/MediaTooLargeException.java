package com.personalblog.media;

public class MediaTooLargeException extends RuntimeException {
    public MediaTooLargeException() {
        super("Image must be 5 MB or smaller");
    }
}
