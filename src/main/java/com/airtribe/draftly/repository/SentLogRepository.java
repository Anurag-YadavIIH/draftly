package com.airtribe.draftly.repository;

import com.airtribe.draftly.domain.SentLog;
import com.airtribe.draftly.domain.DraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SentLogRepository extends JpaRepository<SentLog, Long> {

    Optional<SentLog> findByIdempotencyKey(String idempotencyKey);

    Optional<SentLog> findFirstByDraftIdOrderByCreatedAtDesc(Long draftId);

    List<SentLog> findByStatus(DraftStatus status);
}
