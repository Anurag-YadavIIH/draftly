package com.airtribe.draftly.service;

import com.airtribe.draftly.config.AppProperties;
import com.airtribe.draftly.domain.Draft;
import com.airtribe.draftly.domain.DraftStatus;
import com.airtribe.draftly.domain.EmailMessage;
import com.airtribe.draftly.domain.SentLog;
import com.airtribe.draftly.dto.SendResultDto;
import com.airtribe.draftly.exception.InvalidStateException;
import com.airtribe.draftly.repository.DraftRepository;
import com.airtribe.draftly.repository.SentLogRepository;
import com.airtribe.draftly.service.gmail.GmailTool;
import com.airtribe.draftly.service.rag.StyleRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Sends approved drafts via the {@link GmailTool}, with three production-minded
 * guarantees:
 *
 *  - IDEMPOTENCY: every send carries an idempotency key (defaulting to the draft
 *    id). A draft that is already SENT is never sent again; the original result
 *    is returned instead.
 *  - RETRY + LOGGING: each attempt is recorded in {@link SentLog} with an attempt
 *    counter and the last error. Transient failures leave the log in FAILED state
 *    for the retry scheduler to pick up.
 *  - LEARNING: on a successful send, the reply is indexed as a style sample so
 *    future drafts better match the user's voice (closing the RAG feedback loop).
 */
@Service
public class SendService {

    private static final Logger log = LoggerFactory.getLogger(SendService.class);

    private final DraftRepository draftRepository;
    private final SentLogRepository sentLogRepository;
    private final EmailService emailService;
    private final GmailTool gmailTool;
    private final StyleRetriever styleRetriever;
    private final NotificationService notificationService;
    private final AppProperties props;

    public SendService(DraftRepository draftRepository,
                       SentLogRepository sentLogRepository,
                       EmailService emailService,
                       GmailTool gmailTool,
                       StyleRetriever styleRetriever,
                       NotificationService notificationService,
                       AppProperties props) {
        this.draftRepository = draftRepository;
        this.sentLogRepository = sentLogRepository;
        this.emailService = emailService;
        this.gmailTool = gmailTool;
        this.styleRetriever = styleRetriever;
        this.notificationService = notificationService;
        this.props = props;
    }

    /**
     * Send an approved draft.
     *
     * @param draftId                 the draft to send.
     * @param idempotencyKey          optional; defaults to "draft-{id}".
     * @param injectTransientFailures demo-only: make the mock fail N times first.
     */
    @Transactional
    public SendResultDto send(Long draftId, String idempotencyKey, int injectTransientFailures) {
        Draft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new InvalidStateException("Draft not found: " + draftId));

        // Only approved/edited drafts may be sent. FAILED is also allowed because
        // it represents a previously-approved send that we are now retrying.
        boolean sendable = draft.getStatus() == DraftStatus.APPROVED
                || draft.getStatus() == DraftStatus.EDITED
                || draft.getStatus() == DraftStatus.FAILED;
        if (!sendable) {
            if (draft.getStatus() == DraftStatus.SENT) {
                SentLog existing = sentLogRepository.findFirstByDraftIdOrderByCreatedAtDesc(draftId).orElse(null);
                return new SendResultDto(draftId, DraftStatus.SENT,
                        existing != null ? existing.getSentGmailMessageId() : null,
                        existing != null ? existing.getAttempts() : 0,
                        "Already sent - idempotent no-op.");
            }
            throw new InvalidStateException("Draft " + draftId + " is " + draft.getStatus()
                    + ". Only APPROVED or EDITED drafts can be sent.");
        }

        String key = (idempotencyKey == null || idempotencyKey.isBlank())
                ? "draft-" + draftId : idempotencyKey;

        // Reuse an existing log for this key (so retries share one audit record).
        SentLog sentLog = sentLogRepository.findByIdempotencyKey(key)
                .orElseGet(() -> new SentLog(draftId, draft.getThreadId(), key));

        if (sentLog.getStatus() == DraftStatus.SENT) {
            return new SendResultDto(draftId, DraftStatus.SENT, sentLog.getSentGmailMessageId(),
                    sentLog.getAttempts(), "Already sent - idempotent no-op.");
        }

        EmailMessage original = emailService.getById(draft.getEmailId());
        GmailTool.OutboundMessage outbound = new GmailTool.OutboundMessage(
                draft.getUserEmail(),
                draft.getThreadId(),
                original.getGmailMessageId(),
                original.getSender(),
                replySubject(original.getSubject()),
                draft.getContent(),
                injectTransientFailures);

        sentLog.setAttempts(sentLog.getAttempts() + 1);
        try {
            GmailTool.SentMessage result = gmailTool.sendReply(outbound);

            sentLog.setStatus(DraftStatus.SENT);
            sentLog.setSentGmailMessageId(result.sentGmailMessageId());
            sentLog.setSentAt(Instant.now());
            sentLog.setLastError(null);
            sentLogRepository.save(sentLog);

            draft.setStatus(DraftStatus.SENT);
            draftRepository.save(draft);

            // Learning loop: remember how the user replied.
            styleRetriever.indexSentEmail(draft.getUserEmail(), draft.getContent());

            log.info("Draft {} sent successfully as {} (attempt {})",
                    draftId, result.sentGmailMessageId(), sentLog.getAttempts());
            return new SendResultDto(draftId, DraftStatus.SENT, result.sentGmailMessageId(),
                    sentLog.getAttempts(), "Sent successfully.");

        } catch (Exception ex) {
            sentLog.setStatus(DraftStatus.FAILED);
            sentLog.setLastError(ex.getMessage());
            sentLogRepository.save(sentLog);

            draft.setStatus(DraftStatus.FAILED);
            draftRepository.save(draft);

            log.warn("Draft {} send failed on attempt {}: {}", draftId, sentLog.getAttempts(), ex.getMessage());

            if (sentLog.getAttempts() >= props.getMaxSendRetries()) {
                notificationService.notifyPersistentFailure(draft.getUserEmail(), draftId, ex.getMessage());
                return new SendResultDto(draftId, DraftStatus.FAILED, null, sentLog.getAttempts(),
                        "Failed after " + sentLog.getAttempts() + " attempts: " + ex.getMessage());
            }
            return new SendResultDto(draftId, DraftStatus.FAILED, null, sentLog.getAttempts(),
                    "Send failed (will retry automatically): " + ex.getMessage());
        }
    }

    private String replySubject(String original) {
        if (original == null || original.isBlank()) {
            return "Re:";
        }
        return original.toLowerCase().startsWith("re:") ? original : "Re: " + original;
    }
}
