package com.airtribe.draftly.controller;

import com.airtribe.draftly.dto.*;
import com.airtribe.draftly.service.CurrentUser;
import com.airtribe.draftly.service.DraftService;
import com.airtribe.draftly.service.SendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The core draft workflow: generate a reply, review it (approve / edit / reject),
 * and send approved drafts. This is the heart of the demo.
 */
@RestController
@RequestMapping("/api/drafts")
@Tag(name = "Drafts", description = "Generate, review and send AI reply drafts")
public class DraftController {

    private final DraftService draftService;
    private final SendService sendService;

    public DraftController(DraftService draftService, SendService sendService) {
        this.draftService = draftService;
        this.sendService = sendService;
    }

    @Operation(summary = "Generate an AI reply draft for an email (uses RAG + LLM)")
    @PostMapping
    public DraftDto generate(@Valid @RequestBody GenerateDraftRequest request, Authentication authentication) {
        return DraftDto.from(draftService.generate(CurrentUser.email(authentication), request.emailId(), request.tone()));
    }

    @Operation(summary = "List all drafts (most recent first)")
    @GetMapping
    public List<DraftDto> list(Authentication authentication) {
        return draftService.list(CurrentUser.email(authentication)).stream().map(DraftDto::from).toList();
    }

    @Operation(summary = "Get a single draft by id")
    @GetMapping("/{id}")
    public DraftDto get(@PathVariable Long id, Authentication authentication) {
        return DraftDto.from(draftService.getOwnedById(CurrentUser.email(authentication), id));
    }

    @Operation(summary = "Approve a draft so it can be sent")
    @PostMapping("/{id}/approve")
    public DraftDto approve(@PathVariable Long id, Authentication authentication) {
        draftService.getOwnedById(CurrentUser.email(authentication), id);
        return DraftDto.from(draftService.approve(id));
    }

    @Operation(summary = "Edit a draft's body (marks it EDITED, still sendable)")
    @PutMapping("/{id}/edit")
    public DraftDto edit(@PathVariable Long id, @Valid @RequestBody EditDraftRequest request, Authentication authentication) {
        draftService.getOwnedById(CurrentUser.email(authentication), id);
        return DraftDto.from(draftService.edit(id, request.content()));
    }

    @Operation(summary = "Reject a draft (it can no longer be sent)")
    @PostMapping("/{id}/reject")
    public DraftDto reject(@PathVariable Long id, Authentication authentication) {
        draftService.getOwnedById(CurrentUser.email(authentication), id);
        return DraftDto.from(draftService.reject(id));
    }

    @Operation(summary = "Send an approved/edited draft via Gmail (idempotent, with retry)")
    @PostMapping("/{id}/send")
    public SendResultDto send(@PathVariable Long id,
                              Authentication authentication,
                              @RequestParam(required = false) String idempotencyKey,
                              @RequestParam(defaultValue = "0") int simulateFailures) {
        draftService.getOwnedById(CurrentUser.email(authentication), id);
        return sendService.send(id, idempotencyKey, simulateFailures);
    }
}
