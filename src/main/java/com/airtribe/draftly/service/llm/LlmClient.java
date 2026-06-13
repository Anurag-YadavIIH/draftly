package com.airtribe.draftly.service.llm;

import java.util.List;

/**
 * Abstraction over a Large Language Model used to generate reply drafts.
 *
 * Two implementations are provided and selected by the {@code draftly.llm-provider}
 * property: a deterministic {@link MockLlmClient} (default, needs no API key, great
 * for demos and tests) and {@link AnthropicLlmClient} (calls the real API).
 */
public interface LlmClient {

    /**
     * Generate a reply draft.
     *
     * @param ctx all the context the model needs: the incoming email, desired
     *            tone, signature and retrieved style samples.
     * @return the generated reply body.
     */
    String generateReply(ReplyContext ctx);

    /**
     * Bundle of everything the LLM needs to draft a reply. Using a record keeps
     * the call site readable and makes it easy to add fields later.
     */
    record ReplyContext(
            String incomingSubject,
            String incomingBody,
            String senderName,
            String tone,
            String signature,
            List<String> styleSamples
    ) {
    }
}
