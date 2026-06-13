package com.airtribe.draftly.repository;

import com.airtribe.draftly.domain.StyleSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StyleSampleRepository extends JpaRepository<StyleSample, Long> {

    List<StyleSample> findByUserEmail(String userEmail);

    /** Backfills the pgvector column for a sample from its CSV embedding. */
    @Modifying
    @Transactional
    @Query(value = "UPDATE style_sample SET embedding_vec = CAST(:vector AS vector) WHERE id = :id",
            nativeQuery = true)
    void updateEmbeddingVector(@Param("id") Long id, @Param("vector") String vector);

    /**
     * Top-{@code limit} most similar sample texts by cosine distance
     * ({@code <=>}), using the pgvector HNSW index on {@code embedding_vec}.
     */
    @Query(value = "SELECT text FROM style_sample "
            + "WHERE user_email = :userEmail AND embedding_vec IS NOT NULL "
            + "ORDER BY embedding_vec <=> CAST(:queryVector AS vector) LIMIT :limit",
            nativeQuery = true)
    List<String> findSimilarByVector(@Param("userEmail") String userEmail,
                                      @Param("queryVector") String queryVector,
                                      @Param("limit") int limit);
}
