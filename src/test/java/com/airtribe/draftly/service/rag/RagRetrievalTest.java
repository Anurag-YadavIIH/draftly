package com.airtribe.draftly.service.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the RAG building blocks: the hashing embedder and cosine similarity.
 * These guarantee that "similar text ranks higher" - the core promise of the
 * retrieval step that feeds the LLM the user's writing style.
 */
class RagRetrievalTest {

    private final HashingEmbeddingClient embedder = new HashingEmbeddingClient();

    @Test
    void embeddingIsDeterministic() {
        double[] a = embedder.embed("let's schedule a meeting next week");
        double[] b = embedder.embed("let's schedule a meeting next week");
        assertTrue(VectorMath.cosineSimilarity(a, b) > 0.999,
                "identical text must embed identically");
    }

    @Test
    void similarTextRanksHigherThanUnrelatedText() {
        double[] query = embedder.embed("can we schedule a meeting to discuss the project");
        double[] similar = embedder.embed("let's set up a meeting about the project plan");
        double[] unrelated = embedder.embed("the invoice payment is overdue please refund");

        double simScore = VectorMath.cosineSimilarity(query, similar);
        double unrelatedScore = VectorMath.cosineSimilarity(query, unrelated);

        assertTrue(simScore > unrelatedScore,
                "topically similar text should score higher than unrelated text");
    }

    @Test
    void csvRoundTripPreservesVector() {
        double[] original = embedder.embed("hello world from draftly");
        double[] restored = VectorMath.fromCsv(VectorMath.toCsv(original));
        assertTrue(VectorMath.cosineSimilarity(original, restored) > 0.999,
                "serialise -> deserialise must preserve the vector");
    }
}
