package com.airtribe.draftly.service.rag;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * A simple, dependency-free embedding implementation using the "hashing trick":
 * each word is hashed into one of N buckets and the bucket counts (TF) form the
 * vector, which is then L2-normalised. It is deterministic and offline, so RAG
 * works in the demo without any embedding API.
 *
 * It is intentionally simple - this is the MOCK-profile default so RAG works
 * end-to-end with zero external dependencies. Real deployments use
 * {@link OpenAiEmbeddingClient} behind the same {@link EmbeddingClient} interface.
 */
@Component
@ConditionalOnProperty(name = "draftly.embedding-provider", havingValue = "hashing", matchIfMissing = true)
public class HashingEmbeddingClient implements EmbeddingClient {

    private static final int DIMENSIONS = 256;

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    @Override
    public double[] embed(String text) {
        double[] vec = new double[DIMENSIONS];
        if (text == null || text.isBlank()) {
            return vec;
        }
        String[] tokens = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+");

        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            int bucket = Math.floorMod(token.hashCode(), DIMENSIONS);
            vec[bucket] += 1.0;
        }

        // L2 normalise so cosine similarity is comparable across emails of
        // different lengths.
        double norm = 0.0;
        for (double v : vec) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vec.length; i++) {
                vec[i] /= norm;
            }
        }
        return vec;
    }
}
