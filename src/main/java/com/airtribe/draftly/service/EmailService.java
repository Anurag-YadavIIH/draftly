package com.airtribe.draftly.service;

import com.airtribe.draftly.domain.EmailMessage;
import com.airtribe.draftly.exception.NotFoundException;
import com.airtribe.draftly.repository.EmailMessageRepository;
import com.airtribe.draftly.service.gmail.GmailTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fetches inbound emails through the configured {@link GmailTool} and persists
 * them so drafts can be generated against stable records. Re-fetching is safe:
 * messages already imported (by Gmail message id) are skipped.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final GmailTool gmailTool;
    private final EmailMessageRepository emailRepository;

    public EmailService(GmailTool gmailTool, EmailMessageRepository emailRepository) {
        this.gmailTool = gmailTool;
        this.emailRepository = emailRepository;
    }

    /**
     * Pull recent messages from Gmail and persist any new ones.
     *
     * @return the freshly imported emails (not previously seen).
     */
    @Transactional
    public List<EmailMessage> fetchAndStore(String userEmail, int max) {
        List<GmailTool.FetchedEmail> fetched = gmailTool.fetchRecent(userEmail, max);
        return fetched.stream()
                .filter(f -> !emailRepository.existsByGmailMessageId(f.gmailMessageId()))
                .map(f -> emailRepository.save(new EmailMessage(
                        userEmail, f.gmailMessageId(), f.threadId(), f.sender(), f.recipient(),
                        f.subject(), f.body(), f.receivedAt(), f.unread())))
                .peek(e -> log.info("Imported email {} from {}", e.getGmailMessageId(), e.getSender()))
                .toList();
    }

    public List<EmailMessage> list(String userEmail) {
        return emailRepository.findByUserEmailOrderByReceivedAtDesc(userEmail);
    }

    public EmailMessage getById(Long id) {
        return emailRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Email not found: " + id));
    }
}
