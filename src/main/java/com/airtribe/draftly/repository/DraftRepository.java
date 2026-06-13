package com.airtribe.draftly.repository;

import com.airtribe.draftly.domain.Draft;
import com.airtribe.draftly.domain.DraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DraftRepository extends JpaRepository<Draft, Long> {

    List<Draft> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    List<Draft> findByStatus(DraftStatus status);

    List<Draft> findByEmailId(Long emailId);
}
