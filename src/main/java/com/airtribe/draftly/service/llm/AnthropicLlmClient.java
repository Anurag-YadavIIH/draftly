package com.airtribe.draftly.service.llm;

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
 * Real LLM provider that calls the Anthropic Messages API.
 *
 * Activated only when {@code draftly.llm-provider=anthropic} and an API key is
 * configured. Uses the built-in Java HTTP client so we don't pull in extra SDKs.
 */
@Component
@ConditionalOnProperty(name = "draftly.llm-provider", havingValue = "anthropic")
public class AnthropicLlmClient implements LlmClient {

    private final AppProperties props;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public AnthropicLlmClient(AppProperties props) {
        this.props = props;
    }

    @Override
    public String generateReply(ReplyContext ctx) {
        try {
            Map<String, Object> payload = Map.of(
                    "model", props.getAnthropic().getModel(),
                    "max_tokens", 700,
                    "system", PromptBuilder.systemPrompt(ctx.tone()),
                    "messages", new Object[]{
                            Map.of("role", "user", "content", PromptBuilder.userPrompt(ctx))
                    }
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getAnthropic().getBaseUrl()))
                    .timeout(Duration.ofSeconds(40))
                    .header("content-type", "application/json")
                    .header("x-api-key", props.getAnthropic().getApiKey())
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("LLM API returned HTTP " + response.statusCode()
                        + ": " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode content = root.path("content");
            StringBuilder text = new StringBuilder();
            if (content.isArray()) {
                for (JsonNode block : content) {
                    if ("text".equals(block.path("type").asText())) {
                        text.append(block.path("text").asText());
                    }
                }
            }
            return text.toString().trim();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate reply from LLM: " + e.getMessage(), e);
        }
    }
}
