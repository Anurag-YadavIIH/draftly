package com.airtribe.draftly.service.rag;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Small math utilities for the RAG layer: cosine similarity between vectors and
 * (de)serialisation of vectors to/from the comma-separated form stored in the DB.
 */
public final class VectorMath {

    private VectorMath() {
    }

    /** Cosine similarity in [-1, 1]; higher means more semantically similar. */
    public static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    public static String toCsv(double[] vec) {
        return Arrays.stream(vec)
                .mapToObj(Double::toString)
                .collect(Collectors.joining(","));
    }

    public static double[] fromCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return new double[0];
        }
        String[] parts = csv.split(",");
        double[] vec = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vec[i] = Double.parseDouble(parts[i].trim());
        }
        return vec;
    }
}
