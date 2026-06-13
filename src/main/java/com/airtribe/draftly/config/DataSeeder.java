package com.airtribe.draftly.config;

import com.airtribe.draftly.domain.UserPreference;
import com.airtribe.draftly.repository.StyleSampleRepository;
import com.airtribe.draftly.repository.UserPreferenceRepository;
import com.airtribe.draftly.service.CurrentUser;
import com.airtribe.draftly.service.rag.StyleRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds demo data on first startup so the RAG style-matching and preferences are
 * populated out of the box. Safe to run repeatedly: it only seeds when empty.
 *
 * The style samples below are written in a consistent, warm-but-professional
 * voice. Because draft generation retrieves these as context, generated drafts
 * will visibly echo this style - which is great to point out in the demo video.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserPreferenceRepository preferenceRepository;
    private final StyleSampleRepository styleSampleRepository;
    private final StyleRetriever styleRetriever;

    public DataSeeder(UserPreferenceRepository preferenceRepository,
                      StyleSampleRepository styleSampleRepository,
                      StyleRetriever styleRetriever) {
        this.preferenceRepository = preferenceRepository;
        this.styleSampleRepository = styleSampleRepository;
        this.styleRetriever = styleRetriever;
    }

    @Override
    public void run(String... args) {
        seedPreferences();
        seedStyleSamples();
    }

    private void seedPreferences() {
        if (preferenceRepository.findByUserEmail(CurrentUser.EMAIL).isEmpty()) {
            UserPreference pref = new UserPreference(
                    CurrentUser.EMAIL,
                    "Best regards,\nAlex Morgan\nProduct Team, Draftly",
                    "formal");
            preferenceRepository.save(pref);
            log.info("Seeded default preferences for {}", CurrentUser.EMAIL);
        }
    }

    private void seedStyleSamples() {
        if (!styleSampleRepository.findByUserEmail(CurrentUser.EMAIL).isEmpty()) {
            return;
        }
        List<String> pastEmails = List.of(
                "Thanks for setting this up. Wednesday afternoon works well for me - shall we say 3 PM? "
                        + "I'll send a calendar invite with a meeting link.",
                "Appreciate the quick turnaround on this. I've reviewed the numbers and they look good. "
                        + "Let's proceed and I'll loop in the finance team for the final sign-off.",
                "Thanks for following up. Apologies for the delay on my end - I've now gone through the "
                        + "proposal and I'm happy with the scope. Let's schedule a call to finalise the details.",
                "Got it, thanks for confirming. I'll make sure everything is ready on our side before the "
                        + "deadline. Reach out anytime if anything else comes up.",
                "Thank you for the invoice. I can confirm receipt and everything looks correct. "
                        + "Payment will be processed by end of week."
        );
        pastEmails.forEach(text -> styleRetriever.indexSentEmail(CurrentUser.EMAIL, text));
        log.info("Seeded {} style samples for RAG", pastEmails.size());
    }
}
