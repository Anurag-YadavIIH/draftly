package com.airtribe.draftly.repository;

import com.airtribe.draftly.domain.OAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {

    Optional<OAuthToken> findByUserEmail(String userEmail);

    void deleteByUserEmail(String userEmail);
}
