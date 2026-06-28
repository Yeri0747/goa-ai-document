package es.upm.api.infrastructure.clients;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextractExtractionExceptionTest {

    @Test
    void storesMessage() {
        TextractExtractionException exception = new TextractExtractionException("textract failed");

        assertThat(exception.getMessage()).isEqualTo("textract failed");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void storesCause() {
        RuntimeException cause = new RuntimeException("root");
        TextractExtractionException exception = new TextractExtractionException("wrapped", cause);

        assertThat(exception.getMessage()).isEqualTo("wrapped");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
