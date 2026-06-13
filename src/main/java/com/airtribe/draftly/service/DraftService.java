package com.airtribe.draftly.service;

import com.airtribe.draftly.domain.Draft;
import com.airtribe.draftly.domain.DraftStatus;
import com.airtribe.draftly.domain.EmailMessage;
import com.airtribe.draftly.domain.UserPreference;
import com.airtribe.draftly.exception.InvalidStateException;
import com.airtribe.draftly.exception.NotFoundException;
import com.airtribe.draftly.repository.DraftRepository;
import com.airtribe.draftly.service.llm.LlmClient;
import com.airtribe.draftly.service.rag.StyleRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The heart of Draftly. Generating a draft is a small agent pipeline:
 *
 *   1. Load the inbound email and the user's preferences (tone, signature).
 *   2. RAG: retrieve the user's most similar past emails for style grounding.
 *   3. Ask the LLM to write a reply using that context.
 *   4. Persist the result as a SUGGESTED draft for human review.
 *
 * It also enforces the review state machine (approve / edit / reject) and makes
 * sure only APPROVED or EDITED drafts can ever be sent.
 */
@Service
public class DraftService {

    private static final Logger log = LoggerFactory.getLogger(DraftService.class);

    private final DraftRepository draftRepository;
    private final EmailService emailService;
    private final PreferenceService preferenceService;
    private final StyleRetriever styleRetriever;
    private final LlmClient llmClient;

    public DraftService(DraftRepository draftRepository,
                        EmailService emailService,
                        PreferenceService preferenceService,
                        StyleRetriever styleRetriever,
                        LlmClient llmClient) {
        this.draftRepository = draftRepository;
        this.emailService = emailService;
        this.preferenceService = preferenceService;
        this.styleRetriever = styleRetriever;
        this.llmClient = llmClient;
    }

    @Transactional
    public Draft generate(String userEmail, Long emailId, String requestedTone) {
        EmailMessage email = emailService.getOwnedById(userEmail, emailId);
        UserPreference pref = preferenceService.get(userEmail);

        String tone = (requestedTone != null && !requestedTone.isBlank())
                ? requestedTone : pref.getDefaultTone();

        // RAG step: find stylistically similar past emails to ground the reply.
        List<String> styleSamples = styleRetriever.retrieveSimilar(
                userEmail, email.getSubject() + " " + email.getBody());
        log.info("RAG retrieved {} style sample(s) for email {}", styleSamples.size(), emailId);

        LlmClient.ReplyContext ctx = new LlmClient.ReplyContext(
                email.getSubject(), email.getBody(), email.getSender(),
                tone, pref.getSignature(), styleSamples);

        String content = llmClient.generateReply(ctx);

        Draft draft = new Draft(email.getId(), email.getThreadId(), userEmail, tone, content);
        draft.setStatus(DraftStatus.SUGGESTED);
        Draft saved = draftRepository.save(draft);
        log.info("Generated draft {} for email {} (tone={})", saved.getId(), emailId, tone);
        return saved;
    }

    public List<Draft> list(String userEmail) {
        return draftRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    public Draft getById(Long id) {
        return draftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Draft not found: " + id));
    }

    /** Like {@link #getById}, but 404s if the draft belongs to a different user. */
    public Draft getOwnedById(String userEmail, Long id) {
        return draftRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new NotFoundException("Draft not found: " + id));
    }

    @Transactional
    public Draft approve(Long id) {
        Draft draft = getById(id);
        requireReviewable(draft);
        draft.setStatus(DraftStatus.APPROVED);
        return draftRepository.save(draft);
    }

    @Transactional
    public Draft edit(Long id, String newContent) {
        Draft draft = getById(id);
        requireReviewable(draft);
        draft.setContent(newContent);
        draft.setStatus(DraftStatus.EDITED);
        return draftRepository.save(draft);
    }

    @Transactional
    public Draft reject(Long id) {
        Draft draft = getById(id);
        requireReviewable(draft);
        draft.setStatus(DraftStatus.REJECTED);
        return draftRepository.save(draft);
    }

    /** A draft can only be reviewed while it is still pending (not sent/rejected). */
    private void requireReviewable(Draft draft) {
        if (draft.getStatus() == DraftStatus.SENT
                || draft.getStatus() == DraftStatus.REJECTED) {
            throw new InvalidStateException(
                    "Draft " + draft.getId() + " is " + draft.getStatus() + " and cannot be changed.");
        }
    }
}
