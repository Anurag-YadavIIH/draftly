package com.airtribe.draftly.repository;

import com.airtribe.draftly.domain.Draft;
import com.airtribe.draftly.domain.DraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DraftRepository extends JpaRepository<Draft, Long> {

    List<Draft> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    List<Draft> findByStatus(DraftStatus status);

    List<Draft> findByEmailId(Long emailId);

    Optional<Draft> findByIdAndUserEmail(Long id, String userEmail);
}
