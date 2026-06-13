package com.airtribe.draftly.repository;

import com.airtribe.draftly.domain.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByUserEmail(String userEmail);
}
