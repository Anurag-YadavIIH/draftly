package com.airtribe.draftly.service.rag;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * A simple, dependency-free embedding implementation using the "hashing trick":
 * each word is hashed into one of N buckets and the bucket counts (TF) form the
 * vector, which is then L2-normalised. It is deterministic and offline, so RAG
 * works in the demo without any embedding API.
 *
 * It is intentionally simple - the point of the capstone is to demonstrate the
 * RAG retrieval pipeline (embed -> store -> cosine-similarity search), which this
 * captures faithfully. For production you would swap in a real embedding model
 * behind the same {@link EmbeddingClient} interface.
 */
@Component
public class HashingEmbeddingClient implements EmbeddingClient {

    private static final int DIMENSIONS = 256;

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
