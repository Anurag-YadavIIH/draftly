package com.airtribe.draftly.controller;

import com.airtribe.draftly.dto.EmailDto;
import com.airtribe.draftly.service.CurrentUser;
import com.airtribe.draftly.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Inbox endpoints: pull recent emails from Gmail and list what we have stored.
 */
@RestController
@RequestMapping("/api/emails")
@Tag(name = "Emails", description = "Fetch and view inbox emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @Operation(summary = "Fetch recent emails from Gmail and store new ones")
    @PostMapping("/fetch")
    public List<EmailDto> fetch(@RequestParam(defaultValue = "10") int max, Authentication authentication) {
        return emailService.fetchAndStore(CurrentUser.email(authentication), max).stream()
                .map(EmailDto::from)
                .toList();
    }

    @Operation(summary = "List stored emails (most recent first)")
    @GetMapping
    public List<EmailDto> list(Authentication authentication) {
        return emailService.list(CurrentUser.email(authentication)).stream()
                .map(EmailDto::from)
                .toList();
    }

    @Operation(summary = "Get a single email by id")
    @GetMapping("/{id}")
    public EmailDto get(@PathVariable Long id, Authentication authentication) {
        return EmailDto.from(emailService.getOwnedById(CurrentUser.email(authentication), id));
    }
}
