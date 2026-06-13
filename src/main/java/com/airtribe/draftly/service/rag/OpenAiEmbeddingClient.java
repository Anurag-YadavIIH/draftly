package com.airtribe.draftly.service.rag;

import com.airtribe.draftly.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Real embedding provider that calls OpenAI's Embeddings API
 * ({@code text-embedding-3-small} by default, 1536 dimensions).
 *
 * Activated when {@code draftly.embedding-provider=openai}. Uses the built-in
 * Java HTTP client, mirroring {@link com.airtribe.draftly.service.llm.OpenAiLlmClient}.
 */
@Component
@ConditionalOnProperty(name = "draftly.embedding-provider", havingValue = "openai")
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private static final int DIMENSIONS = 1536;

    private final AppProperties props;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiEmbeddingClient(AppProperties props) {
        this.props = props;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    @Override
    public double[] embed(String text) {
        try {
            Map<String, Object> payload = Map.of(
                    "model", props.getOpenai().getEmbeddingModel(),
                    "input", text == null ? "" : text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getOpenai().getEmbeddingsBaseUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.getOpenai().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("OpenAI Embeddings API returned HTTP " + response.statusCode()
                        + ": " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            // Response shape: { "data": [ { "embedding": [0.1, 0.2, ...] } ] }
            JsonNode values = root.path("data").path(0).path("embedding");
            double[] vec = new double[DIMENSIONS];
            for (int i = 0; i < DIMENSIONS && i < values.size(); i++) {
                vec[i] = values.get(i).asDouble();
            }
            return vec;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get embedding from OpenAI: " + e.getMessage(), e);
        }
    }
}
