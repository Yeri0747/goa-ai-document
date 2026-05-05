package es.upm.api.services;

import es.upm.api.data.entities.DocumentCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

@RestClientTest(value = OpenAiClassifierService.class, properties = "openai.api-key=test-key")
class OpenAiClassifierServiceIT {

    @Autowired
    private OpenAiClassifierService openAiClassifierService;

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

        DocumentCategory category = openAiClassifierService.classifyText("billing content");

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

        DocumentCategory category = openAiClassifierService.classifyText("some content");

        assertThat(category).isEqualTo(DocumentCategory.OTHER);
    }

    @Test
    void testClassifyTextApiError() {
        this.server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withServerError());

        DocumentCategory category = openAiClassifierService.classifyText("some content");

        assertThat(category).isEqualTo(DocumentCategory.OTHER);
    }

    @Test
    void testClassifyTextEmptyText() {
        DocumentCategory category = openAiClassifierService.classifyText("");
        assertThat(category).isEqualTo(DocumentCategory.OTHER);
    }
}
