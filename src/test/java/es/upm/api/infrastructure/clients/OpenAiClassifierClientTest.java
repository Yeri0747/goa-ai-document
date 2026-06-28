package es.upm.api.infrastructure.clients;

import es.upm.api.data.entities.DocumentCategory;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiClassifierClientTest {

    @Test
    void classifyTextWithoutApiKeyReturnsOther() {
        OpenAiClassifierClient client = new OpenAiClassifierClient(
                RestClient.builder(), "", "https://api.openai.com/v1/chat/completions", "gpt-3.5-turbo");

        assertThat(client.classifyText("invoice text")).isEqualTo(DocumentCategory.OTHER);
    }

    @Test
    void classifyTextWithBlankInputReturnsOther() {
        OpenAiClassifierClient client = new OpenAiClassifierClient(
                RestClient.builder(), "test-key", "https://api.openai.com/v1/chat/completions", "gpt-3.5-turbo");

        assertThat(client.classifyText("")).isEqualTo(DocumentCategory.OTHER);
        assertThat(client.classifyText(null)).isEqualTo(DocumentCategory.OTHER);
    }

    @Test
    void summarizeTextWithoutApiKeyReturnsFallbackMessage() {
        OpenAiClassifierClient client = new OpenAiClassifierClient(
                RestClient.builder(), "", "https://api.openai.com/v1/chat/completions", "gpt-3.5-turbo");

        assertThat(client.summarizeText("long text")).contains("falta API Key o texto");
    }

    @Test
    void summarizeTextWithBlankInputReturnsFallbackMessage() {
        OpenAiClassifierClient client = new OpenAiClassifierClient(
                RestClient.builder(), "test-key", "https://api.openai.com/v1/chat/completions", "gpt-3.5-turbo");

        assertThat(client.summarizeText("")).contains("falta API Key o texto");
        assertThat(client.summarizeText(null)).contains("falta API Key o texto");
    }
}
