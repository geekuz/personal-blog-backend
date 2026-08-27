package com.personalblog.media;

public class MediaUploadException extends RuntimeException {
    private final boolean invalidInput;

    public MediaUploadException(String message, boolean invalidInput) {
        super(message);
        this.invalidInput = invalidInput;
    }

    public MediaUploadException(String message, Throwable cause) {
        super(message, cause);
        this.invalidInput = false;
    }

    public boolean isInvalidInput() { return invalidInput; }
}
