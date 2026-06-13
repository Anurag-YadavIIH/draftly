package com.airtribe.draftly.service.llm;

/**
 * Builds the system and user prompts sent to the LLM. Kept separate so both the
 * real and mock clients (and any future provider) construct prompts the same way,
 * and so the RAG style samples are injected consistently.
 */
public final class PromptBuilder {

    private PromptBuilder() {
    }

    public static String systemPrompt(String tone) {
        return """
                You are Draftly, an assistant that writes email replies on the user's behalf.
                Write a reply in a %s tone. Be clear and professional, keep it concise,
                and only reply to what the email actually asks. Do not invent facts.
                Match the user's writing style shown in the examples. Output ONLY the reply body,
                with no subject line and no commentary.
                """.formatted(tone == null ? "formal" : tone);
    }

    public static String userPrompt(LlmClient.ReplyContext ctx) {
        StringBuilder sb = new StringBuilder();

        if (ctx.styleSamples() != null && !ctx.styleSamples().isEmpty()) {
            sb.append("Here are examples of how the user usually writes:\n");
            int i = 1;
            for (String sample : ctx.styleSamples()) {
                sb.append(i++).append(". ").append(sample).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Reply to this email:\n");
        sb.append("From: ").append(ctx.senderName()).append("\n");
        sb.append("Subject: ").append(ctx.incomingSubject()).append("\n");
        sb.append("Body:\n").append(ctx.incomingBody()).append("\n\n");

        if (ctx.signature() != null && !ctx.signature().isBlank()) {
            sb.append("End the reply with this signature exactly:\n").append(ctx.signature()).append("\n");
        }
        return sb.toString();
    }
}
