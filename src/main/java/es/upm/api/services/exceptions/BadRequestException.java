package es.upm.api.services.exceptions;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String detail) {
        super(detail);
    }
}
