package es.upm.api.resources.httperrors;

import es.upm.api.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

    @Mock
    private Environment environment;

    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler(environment);
    }

    @Test
    void unauthorizedRequestDoesNotReturnBody() {
        handler.unauthorizedRequest(new AccessDeniedException("denied"));
    }

    @Test
    void noResourceFoundRequestReturnsNotFoundMessage() {
        ErrorMessage result = handler.noResourceFoundRequest(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "missing"));

        assertThat(result.getCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getError()).isEqualTo("NotFoundException");
        assertThat(result.getMessage()).contains("Path no encontrado");
    }

    @Test
    void notFoundRequestMapsNotFoundException() {
        ErrorMessage result = handler.notFoundRequest(new NotFoundException("Document not found"));

        assertThat(result.getCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getError()).isEqualTo("NotFoundException");
        assertThat(result.getMessage()).contains("Document not found");
    }

    @Test
    void badRequestMapsBadRequestException() {
        ErrorMessage result = handler.badRequest(new BadRequestException("invalid pdf"));

        assertThat(result.getCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getError()).isEqualTo("BadRequestException");
        assertThat(result.getMessage()).isEqualTo("invalid pdf");
    }

    @Test
    void badRequestMapsDuplicateKeyException() {
        ErrorMessage result = handler.badRequest(new DuplicateKeyException("duplicate"));

        assertThat(result.getCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getError()).isEqualTo("DuplicateKeyException");
    }

    @Test
    void conflictMapsConflictException() {
        ErrorMessage result = handler.conflict(new ConflictException("already exists"));

        assertThat(result.getCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getError()).isEqualTo("ConflictException");
        assertThat(result.getMessage()).contains("already exists");
    }

    @Test
    void forbiddenMapsForbiddenException() {
        ErrorMessage result = handler.forbidden(new ForbiddenException("no access"));

        assertThat(result.getCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(result.getError()).isEqualTo("ForbiddenException");
        assertThat(result.getMessage()).contains("no access");
    }

    @Test
    void badGatewayMapsBadGatewayException() {
        ErrorMessage result = handler.badGateway(new BadGatewayException("upstream failed"));

        assertThat(result.getCode()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(result.getError()).isEqualTo("BadGatewayException");
        assertThat(result.getMessage()).contains("upstream failed");
    }

    @Test
    void exceptionMapsGenericExceptionInTestProfile() {
        given(environment.acceptsProfiles(Profiles.of("dev", "test"))).willReturn(true);

        ErrorMessage result = handler.exception(new RuntimeException("unexpected"));

        assertThat(result.getCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getError()).isEqualTo("RuntimeException");
        assertThat(result.getMessage()).isEqualTo("unexpected");
    }

    @Test
    void exceptionMapsGenericExceptionOutsideDevTestProfiles() {
        given(environment.acceptsProfiles(Profiles.of("dev", "test"))).willReturn(false);

        ErrorMessage result = handler.exception(new RuntimeException("unexpected"));

        assertThat(result.getCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getMessage()).isEqualTo("unexpected");
    }
}
