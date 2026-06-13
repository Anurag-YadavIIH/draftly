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
import java.util.List;
import java.util.Map;

/**
 * Real LLM provider that calls the OpenAI Chat Completions API.
 *
 * Activated only when {@code draftly.llm-provider=openai}. Reads the API key from
 * {@code draftly.openai.api-key} (env {@code OPENAI_API_KEY}) and defaults to the
 * {@code gpt-4o-mini} model. Uses the built-in Java HTTP client so we don't pull
 * in an extra SDK, mirroring {@link AnthropicLlmClient}.
 *
 * Prompts are built by the shared {@link PromptBuilder}, so OpenAI, Anthropic and
 * the mock all produce drafts the same way - the only difference is the wire format.
 */
@Component
@ConditionalOnProperty(name = "draftly.llm-provider", havingValue = "openai")
public class OpenAiLlmClient implements LlmClient {

    private final AppProperties props;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiLlmClient(AppProperties props) {
        this.props = props;
    }

    @Override
    public String generateReply(ReplyContext ctx) {
        try {
            // OpenAI uses a "messages" array with system + user roles.
            Map<String, Object> payload = Map.of(
                    "model", props.getOpenai().getModel(),
                    "max_tokens", 700,
                    "temperature", 0.7,
                    "messages", List.of(
                            Map.of("role", "system", "content", PromptBuilder.systemPrompt(ctx.tone())),
                            Map.of("role", "user", "content", PromptBuilder.userPrompt(ctx))
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getOpenai().getBaseUrl()))
                    .timeout(Duration.ofSeconds(40))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.getOpenai().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("OpenAI API returned HTTP " + response.statusCode()
                        + ": " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            // Response shape: { "choices": [ { "message": { "content": "..." } } ] }
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.asText("").trim();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate reply from OpenAI: " + e.getMessage(), e);
        }
    }
}
