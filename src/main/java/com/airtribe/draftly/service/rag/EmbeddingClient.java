package com.airtribe.draftly.service.rag;

/**
 * Turns a piece of text into a numeric vector (embedding) so we can measure how
 * semantically similar two emails are. Used by the RAG style retriever.
 */
public interface EmbeddingClient {

    /** @return a fixed-length embedding vector for the given text. */
    double[] embed(String text);

    /** @return the fixed dimensionality of vectors returned by {@link #embed}. */
    int dimensions();
}
