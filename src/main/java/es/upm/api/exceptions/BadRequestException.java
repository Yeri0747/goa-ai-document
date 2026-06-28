package es.upm.api.exceptions;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String detail) {
        super(detail);
    }
}
