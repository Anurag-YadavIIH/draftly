package com.airtribe.draftly.service.llm;

import com.airtribe.draftly.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic, offline LLM stand-in. It produces a realistic, tone-aware reply
 * by lightly summarising the incoming email and applying the requested tone and
 * signature. This is the DEFAULT provider so the whole app runs end-to-end with
 * zero external dependencies - ideal for the demo video and for unit tests.
 *
 * Swap to the real model by setting {@code draftly.llm-provider=anthropic}.
 */
@Component
@ConditionalOnProperty(name = "draftly.llm-provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    public MockLlmClient(AppProperties props) {
        // props injected for symmetry with the real client; not needed here.
    }

    @Override
    public String generateReply(ReplyContext ctx) {
        String tone = ctx.tone() == null ? "formal" : ctx.tone().toLowerCase();
        String firstName = firstName(ctx.senderName());
        String topic = shorten(ctx.incomingSubject());

        String greeting;
        String body;
        String closing;

        switch (tone) {
            case "friendly" -> {
                greeting = "Hi " + firstName + ",";
                body = "Thanks so much for reaching out about \"" + topic + "\". "
                        + "I've read through your note and I'm happy to help. "
                        + "I'll take care of this and follow up shortly with the details.";
                closing = "Talk soon,";
            }
            case "concise" -> {
                greeting = "Hi " + firstName + ",";
                body = "Got it - re: \"" + topic + "\". Confirmed, I'll handle it and update you shortly.";
                closing = "Thanks,";
            }
            default -> { // formal
                greeting = "Dear " + firstName + ",";
                body = "Thank you for your email regarding \"" + topic + "\". "
                        + "I have reviewed the details you shared and will proceed accordingly. "
                        + "Please let me know if any further information is required.";
                closing = "Best regards,";
            }
        }

        StringBuilder reply = new StringBuilder();
        reply.append(greeting).append("\n\n");
        reply.append(body).append("\n\n");
        reply.append(closing).append("\n");

        if (ctx.signature() != null && !ctx.signature().isBlank()) {
            reply.append(ctx.signature());
        }
        return reply.toString();
    }

    private String firstName(String sender) {
        if (sender == null || sender.isBlank()) {
            return "there";
        }
        String name = sender.contains("<") ? sender.substring(0, sender.indexOf('<')).trim() : sender.trim();
        if (name.isBlank() || name.contains("@")) {
            return "there";
        }
        return name.split("\\s+")[0];
    }

    private String shorten(String subject) {
        if (subject == null || subject.isBlank()) {
            return "your message";
        }
        String s = subject.replaceFirst("(?i)^(re:|fwd:)\\s*", "").trim();
        return s.length() > 60 ? s.substring(0, 57) + "..." : s;
    }
}
