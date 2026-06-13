package com.airtribe.draftly.controller;

import com.airtribe.draftly.dto.PreferenceDto;
import com.airtribe.draftly.dto.PreferenceRequest;
import com.airtribe.draftly.service.CurrentUser;
import com.airtribe.draftly.service.PreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * User preferences that shape generated drafts: the signature and the default
 * tone used when a generate request doesn't specify one.
 */
@RestController
@RequestMapping("/api/preferences")
@Tag(name = "Preferences", description = "Signature and default tone")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @Operation(summary = "Get current user preferences")
    @GetMapping
    public PreferenceDto get() {
        return PreferenceDto.from(preferenceService.get(CurrentUser.EMAIL));
    }

    @Operation(summary = "Create or update user preferences")
    @PutMapping
    public PreferenceDto update(@RequestBody PreferenceRequest request) {
        return PreferenceDto.from(preferenceService.upsert(CurrentUser.EMAIL, request));
    }
}
