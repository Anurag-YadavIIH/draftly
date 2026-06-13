package com.airtribe.draftly.service;

import com.airtribe.draftly.domain.UserPreference;
import com.airtribe.draftly.dto.PreferenceRequest;
import com.airtribe.draftly.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and updates per-user preferences (signature, default tone) that feed
 * into draft generation.
 */
@Service
public class PreferenceService {

    private final UserPreferenceRepository repository;

    public PreferenceService(UserPreferenceRepository repository) {
        this.repository = repository;
    }

    public UserPreference get(String userEmail) {
        return repository.findByUserEmail(userEmail)
                .orElseGet(() -> new UserPreference(userEmail, "", "formal"));
    }

    @Transactional
    public UserPreference upsert(String userEmail, PreferenceRequest request) {
        UserPreference pref = repository.findByUserEmail(userEmail)
                .orElseGet(() -> new UserPreference(userEmail, "", "formal"));
        if (request.signature() != null) {
            pref.setSignature(request.signature());
        }
        if (request.defaultTone() != null && !request.defaultTone().isBlank()) {
            pref.setDefaultTone(request.defaultTone());
        }
        return repository.save(pref);
    }
}
