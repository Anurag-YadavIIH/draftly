package com.airtribe.draftly.service.gmail;

import com.airtribe.draftly.config.AppProperties;
import com.airtribe.draftly.exception.SendFailedException;
import com.airtribe.draftly.service.auth.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Real Gmail integration using the Gmail REST API and the user's stored OAuth2
 * access token. Activated with {@code draftly.gmail-mode=real}.
 *
 * Endpoints used:
 *  - GET  /gmail/v1/users/me/messages              (list message ids)
 *  - GET  /gmail/v1/users/me/messages/{id}         (fetch a message)
 *  - POST /gmail/v1/users/me/messages/send         (send a reply, raw RFC 2822)
 *
 * Threading is preserved by including the {@code threadId} in the send body and
 * the {@code In-Reply-To}/{@code References} headers in the raw MIME message.
 *
 * NOTE: This class is wired and ready, but exercising it requires real Google
 * OAuth credentials. The default demo runs on {@link MockGmailTool}.
 */
@Component
@ConditionalOnProperty(name = "draftly.gmail-mode", havingValue = "real")
public class RealGmailTool implements GmailTool {

    private static final Logger log = LoggerFactory.getLogger(RealGmailTool.class);
    private static final String GMAIL_BASE = "https://gmail.googleapis.com/gmail/v1/users/me";

    private final TokenService tokenService;
    private final AppProperties props;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public RealGmailTool(TokenService tokenService, AppProperties props) {
        this.tokenService = tokenService;
        this.props = props;
    }

    @Override
    public List<FetchedEmail> fetchRecent(String userEmail, int max) {
        String token = tokenService.getValidAccessToken(userEmail);
        List<FetchedEmail> result = new ArrayList<>();
        try {
            HttpRequest listReq = HttpRequest.newBuilder()
                    .uri(URI.create(GMAIL_BASE + "/messages?maxResults=" + max + "&q=is:unread"))
                    .header("Authorization", "Bearer " + token)
                    .timeout(Duration.ofSeconds(20))
                    .GET().build();
            HttpResponse<String> listResp = http.send(listReq, HttpResponse.BodyHandlers.ofString());
            JsonNode messages = mapper.readTree(listResp.body()).path("messages");

            for (JsonNode m : messages) {
                String id = m.path("id").asText();
                HttpRequest msgReq = HttpRequest.newBuilder()
                        .uri(URI.create(GMAIL_BASE + "/messages/" + id + "?format=full"))
                        .header("Authorization", "Bearer " + token)
                        .timeout(Duration.ofSeconds(20))
                        .GET().build();
                JsonNode msg = mapper.readTree(
                        http.send(msgReq, HttpResponse.BodyHandlers.ofString()).body());
                result.add(parseMessage(userEmail, msg));
            }
        } catch (Exception e) {
            log.error("Failed to fetch from Gmail", e);
            throw new IllegalStateException("Gmail fetch failed: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public SentMessage sendReply(OutboundMessage message) throws SendFailedException {
        String token = tokenService.getValidAccessToken(message.userEmail());
        try {
            String raw = buildRawMime(message);
            String body = mapper.writeValueAsString(java.util.Map.of(
                    "raw", Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)),
                    "threadId", message.threadId()));

            HttpRequest sendReq = HttpRequest.newBuilder()
                    .uri(URI.create(GMAIL_BASE + "/messages/send"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(sendReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new SendFailedException("Gmail send returned HTTP " + resp.statusCode()
                        + ": " + resp.body());
            }
            JsonNode node = mapper.readTree(resp.body());
            return new SentMessage(node.path("id").asText(), node.path("threadId").asText());
        } catch (SendFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new SendFailedException("Gmail send failed: " + e.getMessage(), e);
        }
    }

    private FetchedEmail parseMessage(String userEmail, JsonNode msg) {
        String threadId = msg.path("threadId").asText();
        String id = msg.path("id").asText();
        String subject = "", from = "";
        for (JsonNode h : msg.path("payload").path("headers")) {
            String name = h.path("name").asText();
            if (name.equalsIgnoreCase("Subject")) subject = h.path("value").asText();
            if (name.equalsIgnoreCase("From")) from = h.path("value").asText();
        }
        String snippet = msg.path("snippet").asText();
        boolean unread = msg.path("labelIds").toString().contains("UNREAD");
        return new FetchedEmail(id, threadId, from, userEmail, subject, snippet, Instant.now(), unread);
    }

    private String buildRawMime(OutboundMessage m) {
        // Minimal RFC 2822 message. In-Reply-To/References preserve threading.
        return "To: " + m.to() + "\r\n"
                + "Subject: " + m.subject() + "\r\n"
                + (m.inReplyToMessageId() != null
                    ? "In-Reply-To: " + m.inReplyToMessageId() + "\r\n"
                      + "References: " + m.inReplyToMessageId() + "\r\n"
                    : "")
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "\r\n"
                + m.body();
    }
}
