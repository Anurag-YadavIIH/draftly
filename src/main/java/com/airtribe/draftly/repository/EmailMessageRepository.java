package com.airtribe.draftly.repository;

import com.airtribe.draftly.domain.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {

    Optional<EmailMessage> findByGmailMessageId(String gmailMessageId);

    List<EmailMessage> findByUserEmailOrderByReceivedAtDesc(String userEmail);

    List<EmailMessage> findByUserEmailAndUnreadTrueOrderByReceivedAtDesc(String userEmail);

    boolean existsByGmailMessageId(String gmailMessageId);

    Optional<EmailMessage> findByIdAndUserEmail(Long id, String userEmail);
}
