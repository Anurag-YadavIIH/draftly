package com.airtribe.draftly.config;

import com.airtribe.draftly.service.rag.EmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Enables pgvector on the {@code style_sample} table: the {@code vector}
 * extension, a {@code embedding_vec} column sized to the active
 * {@link EmbeddingClient}'s dimensionality, and an HNSW cosine-distance index.
 *
 * Runs before {@link DataSeeder} (lower {@code @Order}) so seeded style samples
 * can populate {@code embedding_vec} immediately. Only active when
 * {@code draftly.vector-store=pgvector}, and requires a Postgres image that
 * ships the pgvector extension (e.g. {@code pgvector/pgvector:pg16}).
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "draftly.vector-store", havingValue = "pgvector")
public class PgVectorInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PgVectorInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;

    public PgVectorInitializer(JdbcTemplate jdbcTemplate, EmbeddingClient embeddingClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingClient = embeddingClient;
    }

    @Override
    public void run(String... args) {
        int dimensions = embeddingClient.dimensions();
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("ALTER TABLE style_sample ADD COLUMN IF NOT EXISTS embedding_vec vector(" + dimensions + ")");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_style_sample_embedding_vec "
                + "ON style_sample USING hnsw (embedding_vec vector_cosine_ops)");
        log.info("pgvector ready: style_sample.embedding_vec is vector({})", dimensions);
    }
}
