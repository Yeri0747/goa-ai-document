package es.upm.api.infrastructure.clients;

public class TextractExtractionException extends RuntimeException {

    public TextractExtractionException(String message) {
        super(message);
    }

    public TextractExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
