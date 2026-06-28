package es.upm.api.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionsTest {

    @Test
    void badRequestExceptionUsesDetailAsMessage() {
        BadRequestException exception = new BadRequestException("invalid pdf");
        assertThat(exception.getMessage()).isEqualTo("invalid pdf");
    }

    @Test
    void notFoundExceptionPrefixesDescription() {
        NotFoundException exception = new NotFoundException("Document not found");
        assertThat(exception.getMessage()).contains("Not Found Exception");
        assertThat(exception.getMessage()).contains("Document not found");
    }

    @Test
    void conflictExceptionPrefixesDescription() {
        ConflictException exception = new ConflictException("duplicate");
        assertThat(exception.getMessage()).contains("Conflict Exception");
        assertThat(exception.getMessage()).contains("duplicate");
    }

    @Test
    void forbiddenExceptionPrefixesDescription() {
        ForbiddenException exception = new ForbiddenException("denied");
        assertThat(exception.getMessage()).contains("Forbidden Exception");
        assertThat(exception.getMessage()).contains("denied");
    }

    @Test
    void badGatewayExceptionPrefixesDescription() {
        BadGatewayException exception = new BadGatewayException("upstream");
        assertThat(exception.getMessage()).contains("Bad Gateway Exception");
        assertThat(exception.getMessage()).contains("upstream");
    }
}
