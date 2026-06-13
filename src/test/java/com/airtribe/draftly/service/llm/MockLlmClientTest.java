package com.airtribe.draftly.service.llm;

import com.airtribe.draftly.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the deterministic mock LLM. These run with no Spring context, no
 * database and no network - they simply verify that the draft generator is
 * tone-aware and always appends the signature.
 */
class MockLlmClientTest {

    private final MockLlmClient client = new MockLlmClient(new AppProperties());

    private LlmClient.ReplyContext context(String tone) {
        return new LlmClient.ReplyContext(
                "Project timeline",
                "Can you confirm the delivery date for the project?",
                "Priya Sharma <priya@example.com>",
                tone,
                "Best regards,\nAlex Morgan",
                List.of("Thanks for reaching out, happy to help.")
        );
    }

    @Test
    void formalReplyUsesFormalGreetingAndSignature() {
        String reply = client.generateReply(context("formal"));
        assertTrue(reply.startsWith("Dear Priya,"), "formal tone should open with 'Dear'");
        assertTrue(reply.contains("Best regards,\nAlex Morgan"), "signature must be appended");
    }

    @Test
    void friendlyReplyUsesCasualGreeting() {
        String reply = client.generateReply(context("friendly"));
        assertTrue(reply.startsWith("Hi Priya,"), "friendly tone should open with 'Hi'");
    }

    @Test
    void differentTonesProduceDifferentBodies() {
        String formal = client.generateReply(context("formal"));
        String concise = client.generateReply(context("concise"));
        assertNotEquals(formal, concise, "tone should change the generated text");
    }

    @Test
    void nullToneFallsBackToFormal() {
        String reply = client.generateReply(context(null));
        assertTrue(reply.startsWith("Dear Priya,"), "null tone should default to formal");
    }

    @Test
    void isDeterministic() {
        // Same input must always give the same output (important for demos/tests).
        assertEquals(client.generateReply(context("concise")),
                client.generateReply(context("concise")));
    }
}
