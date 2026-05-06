package es.upm.api.services;

import es.upm.api.data.entities.DocumentCategory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiClassifierService {

    private static final Logger log = LogManager.getLogger(OpenAiClassifierService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public OpenAiClassifierService(
            RestClient.Builder restClientBuilder,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.api-url:https://api.openai.com/v1/chat/completions}") String apiUrl,
            @Value("${openai.model:gpt-3.5-turbo}") String model) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.restClient = restClientBuilder.build();
    }

    public DocumentCategory classifyText(String text) {
        if (apiKey == null || apiKey.isBlank() || text == null || text.isBlank()) {
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("OpenAI API Key is missing. Falling back to OTHER category.");
            }
            return DocumentCategory.OTHER;
        }

        String systemPrompt = """
            You are a highly accurate legal and administrative document classifier.
            Classify the following document into ONE of these categories:
            INVOICE, CONTRACT, JUDGMENT, LEGAL_BRIEF, IDENTIFICATION, OTHER.
            Rules:
            - Choose ONLY one category.
            - Respond with ONLY the category name in uppercase.
            - Do not add explanations or extra text.
            """;

        String content = this.callOpenAi(systemPrompt, text, 0.0);

        if (content != null) {
            try {
                return DocumentCategory.valueOf(content.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unrecognized category from OpenAI: {}", content);
            }
        }

        return DocumentCategory.OTHER;
    }

    public String summarizeText(String text) {
        if (apiKey == null || apiKey.isBlank() || text == null || text.isBlank()) {
            return "No se pudo generar el resumen (falta API Key o texto).";
        }

        String systemPrompt = """
            Eres un asistente legal experto.
            Tu tarea es generar un resumen ejecutivo del documento proporcionado.
            Extrae los puntos clave, las partes involucradas y cualquier obligación importante.
            """;

        String content = this.callOpenAi(systemPrompt, text, 0.0);
        return content != null ? content : "Error al generar el resumen.";
    }

    private String callOpenAi(String systemPrompt, String userText, double temperature) {
        Map<String, Object> requestBody = Map.of(
                "model", this.model,
                "temperature", temperature,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userText)
                )
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri(this.apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null && message.containsKey("content")) {
                        return (String) message.get("content");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to call OpenAI API", e);
        }
        return null;
    }
}