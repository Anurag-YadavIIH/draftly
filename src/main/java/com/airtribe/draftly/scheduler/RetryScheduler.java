package com.airtribe.draftly.scheduler;

import com.airtribe.draftly.config.AppProperties;
import com.airtribe.draftly.domain.DraftStatus;
import com.airtribe.draftly.domain.SentLog;
import com.airtribe.draftly.repository.SentLogRepository;
import com.airtribe.draftly.service.SendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Background worker that automatically re-attempts failed sends. Every 15s it
 * looks for {@link SentLog} rows in FAILED state that still have retry budget
 * left ({@code attempts < draftly.max-send-retries}) and asks the
 * {@link SendService} to try again. Because sends are idempotent, retrying is
 * always safe.
 */
@Component
public class RetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetryScheduler.class);

    private final SentLogRepository sentLogRepository;
    private final SendService sendService;
    private final AppProperties props;

    public RetryScheduler(SentLogRepository sentLogRepository,
                          SendService sendService,
                          AppProperties props) {
        this.sentLogRepository = sentLogRepository;
        this.sendService = sendService;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "15000")
    public void retryFailedSends() {
        List<SentLog> failed = sentLogRepository.findByStatus(DraftStatus.FAILED);
        for (SentLog logEntry : failed) {
            if (logEntry.getAttempts() >= props.getMaxSendRetries()) {
                continue; // exhausted - the user has already been notified.
            }
            log.info("Retrying failed send for draft {} (attempt {} of {})",
                    logEntry.getDraftId(), logEntry.getAttempts() + 1, props.getMaxSendRetries());
            try {
                // injectTransientFailures=0: a genuine retry, no simulated failure.
                sendService.send(logEntry.getDraftId(), logEntry.getIdempotencyKey(), 0);
            } catch (Exception e) {
                log.warn("Retry for draft {} threw: {}", logEntry.getDraftId(), e.getMessage());
            }
        }
    }
}
