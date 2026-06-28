package es.upm.api.resources.httperrors;

import es.upm.api.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMessageTest {

    @Test
    void buildsFromException() {
        ErrorMessage message = new ErrorMessage(new BadRequestException("invalid"), 400);

        assertThat(message.getError()).isEqualTo("BadRequestException");
        assertThat(message.getMessage()).isEqualTo("invalid");
        assertThat(message.getCode()).isEqualTo(400);
        assertThat(message.toString())
                .contains("BadRequestException")
                .contains("invalid")
                .contains("400");
    }
}
