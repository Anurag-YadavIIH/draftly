package com.airtribe.draftly.repository;

import com.airtribe.draftly.domain.StyleSample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StyleSampleRepository extends JpaRepository<StyleSample, Long> {

    List<StyleSample> findByUserEmail(String userEmail);
}
