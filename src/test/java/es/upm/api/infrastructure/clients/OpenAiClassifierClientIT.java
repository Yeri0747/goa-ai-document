package es.upm.api.infrastructure.clients;

import es.upm.api.data.entities.DocumentCategory;
import es.upm.api.infrastructure.clients.OpenAiClassifierClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

@RestClientTest(value = OpenAiClassifierClient.class, properties = "openai.api-key=test-key")
class OpenAiClassifierClientIT {

    @Autowired
    private OpenAiClassifierClient openAiClassifierClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void testClassifyTextSuccess() {
        String mockResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "INVOICE"
                      }
                    }
                  ]
                }
                """;

        this.server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        DocumentCategory category = openAiClassifierClient.classifyText("billing content");

        assertThat(category).isEqualTo(DocumentCategory.INVOICE);
    }

    @Test
    void testClassifyTextUnrecognizedCategory() {
        String mockResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "UNKNOWN_CAT"
                      }
                    }
                  ]
                }
                """;

        this.server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        DocumentCategory category = openAiClassifierClient.classifyText("some content");

        assertThat(category).isEqualTo(DocumentCategory.OTHER);
    }

    @Test
    void testClassifyTextApiError() {
        this.server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withServerError());

        DocumentCategory category = openAiClassifierClient.classifyText("some content");

        assertThat(category).isEqualTo(DocumentCategory.OTHER);
    }

    @Test
    void testClassifyTextEmptyText() {
        DocumentCategory category = openAiClassifierClient.classifyText("");
        assertThat(category).isEqualTo(DocumentCategory.OTHER);
    }

    @Test
    void testSummarizeTextSuccess() {
        String mockResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "Este es un resumen de prueba impecable."
                      }
                    }
                  ]
                }
                """;

        this.server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String summary = openAiClassifierClient.summarizeText("Texto largo para resumir...");

        assertThat(summary).isEqualTo("Este es un resumen de prueba impecable.");
    }

    @Test
    void testSummarizeTextEmptyText() {
        String summary = openAiClassifierClient.summarizeText("");
        assertThat(summary).contains("falta API Key o texto");
    }

    @Test
    void testSummarizeTextApiFailure() {
        this.server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withServerError());

        String result = openAiClassifierClient.summarizeText("texto");
        assertThat(result).isEqualTo("Error al generar el resumen.");
    }

    @Test
    void testClassifyTextUnrecognizedResponse() {
        String mockResponse = """
                { "choices": [{ "message": { "content": "CATEGORIA_INVENTADA" } }] }
                """;

        this.server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        DocumentCategory category = openAiClassifierClient.classifyText("contenido");
        assertThat(category).isEqualTo(DocumentCategory.OTHER);
    }

}
